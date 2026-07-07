package com.zik00.shop.repository;

import java.util.Optional;

import com.zik00.shop.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query(value = "select * from `user` order by user_id asc limit 1", nativeQuery = true)
    Optional<User> findCurrentUser();
}
