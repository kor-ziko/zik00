package com.zik00.shop.service.auth;

import com.zik00.shop.domain.User;
import com.zik00.shop.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedUserService {
    private static final String GOOGLE_LOGIN_PREFIX = "google:";

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

    public String toGoogleLoginId(String googleSubject) {
        if (googleSubject == null || googleSubject.isBlank()) {
            throw new IllegalArgumentException("Google 계정 식별자가 없습니다.");
        }
        return GOOGLE_LOGIN_PREFIX + googleSubject.trim();
    }
}
