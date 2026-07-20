package com.zik00.shop.service.auth;

import com.zik00.shop.domain.User;
import com.zik00.shop.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class OAuthLoginCompletionService {
    private static final Duration CODE_TTL = Duration.ofMinutes(2);

    private final UserRepository userRepository;
    private final JwtSessionService jwtSessionService;
    private final Map<String, PendingLogin> pendingLogins = new ConcurrentHashMap<>();

    public OAuthLoginCompletionService(
            UserRepository userRepository,
            JwtSessionService jwtSessionService
    ) {
        this.userRepository = userRepository;
        this.jwtSessionService = jwtSessionService;
    }

    public String prepare(User user, String destination) {
        Instant now = Instant.now();
        pendingLogins.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));

        String code = UUID.randomUUID() + "-" + UUID.randomUUID();
        pendingLogins.put(code, new PendingLogin(user.getAccessId(), destination, now.plus(CODE_TTL)));
        return code;
    }

    public String complete(String code, HttpServletResponse response) {
        PendingLogin pendingLogin = code == null ? null : pendingLogins.remove(code);
        if (pendingLogin == null || !Instant.now().isBefore(pendingLogin.expiresAt())) {
            throw new InvalidJwtException("OAuth 로그인 완료 코드가 만료되었거나 유효하지 않습니다.");
        }

        User user = userRepository.findByAccessId(pendingLogin.accessId())
                .orElseThrow(() -> new InvalidJwtException("OAuth 로그인 회원을 찾을 수 없습니다."));
        String destination = "/".equals(pendingLogin.destination())
                || "/login/additional-info".equals(pendingLogin.destination())
                ? pendingLogin.destination()
                : "/";

        jwtSessionService.issue(user, response);
        return destination;
    }

    private record PendingLogin(String accessId, String destination, Instant expiresAt) {
    }
}
