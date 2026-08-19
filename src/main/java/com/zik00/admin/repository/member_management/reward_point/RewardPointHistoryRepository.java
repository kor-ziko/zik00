package com.zik00.admin.repository.member_management.reward_point;

import com.zik00.admin.domain.member_management.reward_point.RewardPointHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardPointHistoryRepository extends JpaRepository<RewardPointHistory, Long> {
    List<RewardPointHistory> findAllByOrderByCreatedAtDescIdDesc();
}
