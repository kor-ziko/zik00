package com.zik00.shop.controller.auth;

import java.util.List;
import java.util.Map;
import java.time.Instant;

import com.zik00.shop.dto.auth.AdditionalInfoRequest;
import com.zik00.shop.service.auth.AuthenticatedUserService;
import com.zik00.shop.service.auth.RegistrationService;
import com.zik00.shop.service.auth.JwtSessionService;
import com.zik00.shop.service.auth.JwtCookieService;
import com.zik00.shop.service.auth.InvalidJwtException;
import com.zik00.shop.service.auth.OAuthLoginCompletionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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

    public AuthApiController(
            AuthenticatedUserService authenticatedUserService,
            RegistrationService registrationService,
            JwtSessionService jwtSessionService,
            JwtCookieService jwtCookieService,
            OAuthLoginCompletionService oAuthLoginCompletionService
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.registrationService = registrationService;
        this.jwtSessionService = jwtSessionService;
        this.jwtCookieService = jwtCookieService;
        this.oAuthLoginCompletionService = oAuthLoginCompletionService;
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

    @PostMapping("/additional-info")
    public ResponseEntity<Void> completeRegistration(@Valid @RequestBody AdditionalInfoRequest request) {
        registrationService.completeRegistration(request);
        return ResponseEntity.noContent().build();
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

    public record AuthSessionResponse(boolean authenticated, boolean registrationComplete, String nickname) {
    }

    public record AccessTokenResponse(String accessToken, Instant expiresAt) {
    }

    public record OAuthCompleteRequest(String code) {
    }

    public record OAuthCompleteResponse(String accessToken, Instant expiresAt, String destination) {
    }

    public record ApiErrorResponse(List<String> messages) {
    }
}
