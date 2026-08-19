package com.zik00.admin.domain.member_management.deposit_request;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@Entity
@Table(name = "deposit_request")
public class DepositRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deposit_request_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private int amount;

    @Column(name = "depositor_name", nullable = false, length = 100)
    private String depositorName;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "admin_memo", length = 500)
    private String adminMemo;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    protected DepositRequest() {
    }

    public DepositRequest(Long memberId, int amount, String depositorName) {
        this.memberId = memberId;
        this.amount = amount;
        this.depositorName = depositorName;
    }

    @PrePersist
    void onCreate() {
        if (requestedAt == null) requestedAt = LocalDateTime.now();
    }

    public void approve(String memo) {
        ensurePending();
        status = "APPROVED";
        adminMemo = normalize(memo);
        processedAt = LocalDateTime.now();
    }

    public void reject(String memo) {
        ensurePending();
        status = "REJECTED";
        adminMemo = normalize(memo);
        processedAt = LocalDateTime.now();
    }

    private void ensurePending() {
        if (!"PENDING".equals(status)) throw new IllegalStateException("이미 처리된 예치금 신청입니다.");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
