package com.zik00.shop.domain;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "`user`")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long memberId;

    @Column(name = "access_id", nullable = false, unique = true, length = 36)
    private String accessId;

    private String name;

    @Column(name = "name_kana")
    private String nameKana;
    @Column(name = "birth_date")
    private LocalDate birthDate;

    private String gender;
    private String nickname;

    private String telephone;

    @Column(name = "login_id")
    private String loginId;

    @Column(name = "deposit_balance")
    private int depositBalance;

    @Column(name = "reward_point")
    private int rewardPoint;

    @Column(name = "mobile_phone")
    private String mobilePhone;

    private String email;

    @Column(name = "completed_order_count")
    private int completedOrderCount;

    @Column(name = "joined_date")
    private LocalDate joinedDate;

    @Column(name = "member_detail")
    private String memberDetail;

    @Column(name = "alarm_consent")
    private boolean alarmConsent;

    protected User() {
    }

    public User(
            String name,
            LocalDate birthDate,
            String gender,
            String nickname,
            String telephone,
            String loginId,
            int depositBalance,
            int rewardPoint,
            String mobilePhone,
            String email,
            int completedOrderCount,
            LocalDate joinedDate,
            String memberDetail,
            boolean alarmConsent
    ) {
        this.accessId = UUID.randomUUID().toString();
        this.name = name;
        this.birthDate = birthDate;
        this.gender = gender;
        this.nickname = nickname;
        this.telephone = telephone;
        this.loginId = loginId;
        this.depositBalance = depositBalance;
        this.rewardPoint = rewardPoint;
        this.mobilePhone = mobilePhone;
        this.email = email;
        this.completedOrderCount = completedOrderCount;
        this.joinedDate = joinedDate;
        this.memberDetail = memberDetail;
        this.alarmConsent = alarmConsent;
    }

    public void updateProfile(
            String name,
            String nickname,
            String mobilePhone,
            String email,
            boolean alarmConsent
    ) {
        this.name = normalize(name);
        this.nickname = normalize(nickname);
        this.mobilePhone = normalize(mobilePhone);
        this.email = normalize(email);
        this.alarmConsent = alarmConsent;
    }

    public static User createGoogleUser(String loginId, String email, String googleName) {
        return new User(
                normalizeValue(googleName),
                null,
                "",
                "",
                "",
                loginId,
                0,
                0,
                "",
                normalizeValue(email),
                0,
                LocalDate.now(),
                "",
                false
        );
    }

    public void completeRegistration(
            String name,
            String nameKana,
            LocalDate birthDate,
            String gender,
            String nickname,
            String mobilePhone
    ) {
        this.name = normalizeValue(name);
        this.nameKana = normalizeValue(nameKana);
        this.birthDate = birthDate;
        this.gender = normalizeValue(gender);
        this.nickname = normalizeValue(nickname);
        this.mobilePhone = normalizeValue(mobilePhone);
        if (this.joinedDate == null) {
            this.joinedDate = LocalDate.now();
        }
    }

    public boolean hasCompletedRegistration() {
        return !normalizeValue(name).isEmpty()
                && !normalizeValue(nameKana).isEmpty()
                && birthDate != null
                && !normalizeValue(gender).isEmpty()
                && !normalizeValue(nickname).isEmpty()
                && !normalizeValue(mobilePhone).isEmpty();
    }

    private String normalize(String value) {
        return normalizeValue(value);
    }

    private static String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }
    public long getMemberId() {
        return memberId == null ? 0L : memberId;
    }

    public String getUserId() {
        return loginId;
    }
}
