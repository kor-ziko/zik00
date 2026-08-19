package com.zik00.admin.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "admin_users")
public class AdminUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AdminUser() {
    }

    public AdminUser(String loginId, String passwordHash, String name, boolean active) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.name = name;
        this.active = active;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String name, boolean active) {
        this.name = name;
        this.active = active;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

}
