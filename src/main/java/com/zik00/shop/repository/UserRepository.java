package com.zik00.shop.repository;

import java.util.Optional;

import com.zik00.shop.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);

    Optional<User> findByAccessId(String accessId);
}
