package com.zik00.shop.repository;

import java.util.Optional;

import com.zik00.shop.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    //로그인 기능이 없기도 하고 로그인한 유저 id 가져오도록 수정해야함.
    @Query(value = "select * from `user` order by user_id asc limit 1", nativeQuery = true)
    Optional<User> findCurrentUser();
}
