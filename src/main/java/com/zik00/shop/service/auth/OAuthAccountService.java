package com.zik00.shop.service.auth;

import com.zik00.shop.domain.User;
import com.zik00.shop.repository.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthAccountService {
    private final UserRepository userRepository;

    public OAuthAccountService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public Optional<User> findExistingAndBackfillEmail(OAuthProfile profile) {
        Optional<User> user = userRepository.findByLoginId(profile.loginId());
        user.filter(existing -> existing.getEmail() == null || existing.getEmail().isBlank())
                .ifPresent(existing -> existing.updateOAuthEmail(profile.email()));
        return user;
    }
}
