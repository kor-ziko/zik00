package com.zik00.shop.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "`user`")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long memberId;

    private String name;
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

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
    public long getMemberId() {
        return memberId == null ? 0L : memberId;
    }
    public String getName() {
        return name;
    }
    public LocalDate getBirthDate() {
        return birthDate;
    }
    public String getGender() {
        return gender;
    }
    public String getNickname() {
        return nickname;
    }
    public String getTelephone() {
        return telephone;
    }
    public String getUserId() {
        return loginId;
    }
    public int getDepositBalance() {
        return depositBalance;
    }
    public int getRewardPoint() {
        return rewardPoint;
    }
    public String getMobilePhone() {
        return mobilePhone;
    }
    public String getEmail() {
        return email;
    }
    public int getCompletedOrderCount() {
        return completedOrderCount;
    }
    public LocalDate getJoinedDate() {
        return joinedDate;
    }
    public String getMemberDetail() {
        return memberDetail;
    }
    public boolean isAlarmConsent() {
        return alarmConsent;
    }
}
