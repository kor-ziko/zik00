package com.zik00.admin.repository.member_management.deposit_request;

import com.zik00.admin.domain.member_management.deposit_request.DepositRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositRequestRepository extends JpaRepository<DepositRequest, Long> {
    List<DepositRequest> findAllByOrderByRequestedAtDescIdDesc();
}
