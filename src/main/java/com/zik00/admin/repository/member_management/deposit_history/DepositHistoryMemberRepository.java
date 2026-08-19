package com.zik00.admin.repository.member_management.deposit_history;

import com.zik00.shop.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositHistoryMemberRepository extends JpaRepository<User, Long> {
}
