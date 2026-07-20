package com.zik00.shop.service.auth;

import com.zik00.shop.domain.User;
import com.zik00.shop.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleAccountService {
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public GoogleAccountService(UserRepository userRepository, AuthenticatedUserService authenticatedUserService) {
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Transactional
    public User findOrCreate(String subject, String email, String googleName) {
        String loginId = authenticatedUserService.toGoogleLoginId(subject);
        return userRepository.findByLoginId(loginId).orElseGet(() -> userRepository.save(
                User.createGoogleUser(loginId, email, googleName)
        ));
    }
}
