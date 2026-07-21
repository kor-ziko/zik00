package com.zik00.shop.service.auth;

import com.zik00.shop.domain.User;
import com.zik00.shop.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedUserService {
    private static final java.util.Set<String> OAUTH_PROVIDERS = java.util.Set.of("google", "kakao", "line");

    private final UserRepository userRepository;

    public AuthenticatedUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        return userRepository.findByAccessId(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("로그인 회원을 찾을 수 없습니다."));
    }

    public String toOAuthLoginId(String provider, String subject) {
        String normalizedProvider = provider == null ? "" : provider.trim().toLowerCase(java.util.Locale.ROOT);
        if (!OAUTH_PROVIDERS.contains(normalizedProvider)) {
            throw new IllegalArgumentException("지원하지 않는 OAuth 공급자입니다.");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("OAuth 계정 식별자가 없습니다.");
        }
        return normalizedProvider + ":" + subject.trim();
    }
}
