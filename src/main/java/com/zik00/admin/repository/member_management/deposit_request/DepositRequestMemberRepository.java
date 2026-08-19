package com.zik00.admin.repository.member_management.deposit_request;

import com.zik00.shop.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositRequestMemberRepository extends JpaRepository<User, Long> {
}
