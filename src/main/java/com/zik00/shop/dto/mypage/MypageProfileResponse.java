package com.zik00.shop.dto.mypage;

import com.zik00.shop.domain.User;
import lombok.Getter;

@Getter
public class MypageProfileResponse {
    private final String name;
    private final String nickname;
    private final String telephone;
    private final String mobilePhone;
    private final String email;
    private final boolean alarmConsent;

    private MypageProfileResponse(
            String name,
            String nickname,
            String telephone,
            String mobilePhone,
            String email,
            boolean alarmConsent
    ) {
        this.name = name;
        this.nickname = nickname;
        this.telephone = telephone;
        this.mobilePhone = mobilePhone;
        this.email = email;
        this.alarmConsent = alarmConsent;
    }

    public static MypageProfileResponse from(User user) {
        return new MypageProfileResponse(
                user.getName(),
                user.getNickname(),
                user.getTelephone(),
                user.getMobilePhone(),
                user.getEmail(),
                user.isAlarmConsent()
        );
    }
}
