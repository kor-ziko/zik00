package com.zik00.admin.repository.member_management.deposit_history;

import com.zik00.admin.domain.member_management.deposit_history.DepositHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositHistoryRepository extends JpaRepository<DepositHistory, Long> {
    List<DepositHistory> findAllByOrderByCreatedAtDescIdDesc();
}
