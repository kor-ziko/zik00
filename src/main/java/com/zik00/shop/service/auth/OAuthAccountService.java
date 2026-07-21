package com.zik00.shop.service.auth;

import com.zik00.shop.domain.User;
import com.zik00.shop.repository.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthAccountService {
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public OAuthAccountService(UserRepository userRepository, AuthenticatedUserService authenticatedUserService) {
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Transactional(readOnly = true)
    public Optional<User> findExisting(String provider, String subject) {
        String loginId = authenticatedUserService.toOAuthLoginId(provider, subject);
        return userRepository.findByLoginId(loginId);
    }

    @Transactional
    public void saveEmailIfMissing(User user, String email) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            user.updateOAuthEmail(email);
            userRepository.save(user);
        }
    }
}
