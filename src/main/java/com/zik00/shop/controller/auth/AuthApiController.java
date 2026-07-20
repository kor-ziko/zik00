package com.zik00.shop.controller.auth;

import java.util.List;
import java.util.Map;
import java.time.Instant;

import com.zik00.shop.dto.auth.RegistrationDetailRequest;
import com.zik00.shop.service.auth.AuthenticatedUserService;
import com.zik00.shop.service.auth.RegistrationService;
import com.zik00.shop.service.auth.JwtSessionService;
import com.zik00.shop.service.auth.JwtCookieService;
import com.zik00.shop.service.auth.InvalidJwtException;
import com.zik00.shop.service.auth.OAuthLoginCompletionService;
import com.zik00.shop.service.auth.PendingGoogleRegistrationService;
import com.zik00.shop.service.auth.RegistrationTermsRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
    private final AuthenticatedUserService authenticatedUserService;
    private final RegistrationService registrationService;
    private final JwtSessionService jwtSessionService;
    private final JwtCookieService jwtCookieService;
    private final OAuthLoginCompletionService oAuthLoginCompletionService;
    private final PendingGoogleRegistrationService pendingRegistrationService;

    public AuthApiController(
            AuthenticatedUserService authenticatedUserService,
            RegistrationService registrationService,
            JwtSessionService jwtSessionService,
            JwtCookieService jwtCookieService,
            OAuthLoginCompletionService oAuthLoginCompletionService,
            PendingGoogleRegistrationService pendingRegistrationService
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.registrationService = registrationService;
        this.jwtSessionService = jwtSessionService;
        this.jwtCookieService = jwtCookieService;
        this.oAuthLoginCompletionService = oAuthLoginCompletionService;
        this.pendingRegistrationService = pendingRegistrationService;
    }

    @GetMapping("/session")
    public AuthSessionResponse session() {
        var user = authenticatedUserService.getCurrentUser();
        return new AuthSessionResponse(
                true,
                registrationService.isRegistrationComplete(user),
                user.getNickname()
        );
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("headerName", csrfToken.getHeaderName(), "token", csrfToken.getToken());
    }

    @PostMapping("/oauth/complete")
    public ResponseEntity<?> completeOAuthLogin(
            @RequestBody OAuthCompleteRequest request,
            HttpServletResponse response
    ) {
        try {
            OAuthLoginCompletionService.CompletionResult result =
                    oAuthLoginCompletionService.complete(request.code(), response);
            return ResponseEntity.ok(new OAuthCompleteResponse(
                    result.accessToken(),
                    result.expiresAt(),
                    result.destination()
            ));
        } catch (InvalidJwtException exception) {
            jwtCookieService.clearRefreshToken(response);
            return ResponseEntity.status(401).body(new ApiErrorResponse(List.of(exception.getMessage())));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            JwtSessionService.AccessTokenResult result = jwtSessionService.refresh(request, response);
            return ResponseEntity.ok(new AccessTokenResponse(result.accessToken(), result.expiresAt()));
        } catch (InvalidJwtException exception) {
            jwtCookieService.clearRefreshToken(response);
            return ResponseEntity.status(401).body(new ApiErrorResponse(List.of(exception.getMessage())));
        }
    }

    @GetMapping("/terms")
    public TermsSessionResponse registrationTerms(HttpServletRequest request) {
        var pending = pendingRegistrationService.require(request);
        return new TermsSessionResponse(pending.termsAccepted(), pending.alarmConsent());
    }

    @PostMapping("/terms")
    public ResponseEntity<Void> acceptRegistrationTerms(
            @Valid @RequestBody TermsAgreementRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        pendingRegistrationService.require(httpRequest);
        pendingRegistrationService.acceptTerms(httpRequest, response, request.alarmConsent());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/detail")
    public DetailSessionResponse registrationDetail(HttpServletRequest request) {
        pendingRegistrationService.requireTermsAccepted(request);
        return new DetailSessionResponse(true);
    }

    @PostMapping("/detail")
    public AccessTokenResponse completeRegistration(
            @Valid @RequestBody RegistrationDetailRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        PendingGoogleRegistrationService.AcceptedGoogleRegistration pending =
                pendingRegistrationService.requireTermsAccepted(httpRequest);
        RegistrationService.PreparedRegistration detail = registrationService.prepareRegistration(request);
        PendingGoogleRegistrationService.AcceptedGoogleRegistration consumed =
                pendingRegistrationService.consumeTermsAccepted(httpRequest, response);
        if (!pending.account().subject().equals(consumed.account().subject())) {
            throw new InvalidJwtException("가입 요청 정보가 일치하지 않습니다. Google 로그인을 다시 진행해주세요.");
        }
        var user = registrationService.completeGoogleRegistration(consumed, detail);
        JwtSessionService.AccessTokenResult token = jwtSessionService.issue(user, response);
        return new AccessTokenResponse(token.accessToken(), token.expiresAt());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> validationError(MethodArgumentNotValidException exception) {
        List<String> messages = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .toList();
        return ResponseEntity.badRequest().body(new ApiErrorResponse(messages));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> invalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(List.of(exception.getMessage())));
    }

    @ExceptionHandler(InvalidJwtException.class)
    public ResponseEntity<ApiErrorResponse> invalidRegistration(InvalidJwtException exception) {
        return ResponseEntity.status(401).body(new ApiErrorResponse(List.of(exception.getMessage())));
    }

    @ExceptionHandler(RegistrationTermsRequiredException.class)
    public ResponseEntity<ApiErrorResponse> termsRequired(RegistrationTermsRequiredException exception) {
        return ResponseEntity.status(409).body(new ApiErrorResponse(List.of(exception.getMessage())));
    }

    public record AuthSessionResponse(boolean authenticated, boolean registrationComplete, String nickname) {
    }

    public record AccessTokenResponse(String accessToken, Instant expiresAt) {
    }

    public record OAuthCompleteRequest(String code) {
    }

    public record OAuthCompleteResponse(String accessToken, Instant expiresAt, String destination) {
    }

    public record DetailSessionResponse(boolean pending) {
    }

    public record TermsSessionResponse(boolean accepted, boolean alarmConsent) {
    }

    public record TermsAgreementRequest(
            @AssertTrue(message = "필수 약관에 모두 동의해주세요.") boolean accepted,
            boolean alarmConsent
    ) {
    }

    public record ApiErrorResponse(List<String> messages) {
    }
}
