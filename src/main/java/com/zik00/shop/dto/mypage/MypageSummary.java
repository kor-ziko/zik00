package com.zik00.shop.dto.mypage;

import lombok.Getter;

@Getter
public class MypageSummary {
    private final int completedOrderCount;
    private final int deliveryTrackingCount;
    private final int inquiryCount;
    private final int couponCount;
    private final int depositBalance;
    private final int rewardPoint;
    private final String memberNickname;

    public MypageSummary(
            int completedOrderCount,
            int deliveryTrackingCount,
            int inquiryCount,
            int couponCount,
            int depositBalance,
            int rewardPoint,
            String memberNickname
    ) {
        this.completedOrderCount = completedOrderCount;
        this.deliveryTrackingCount = deliveryTrackingCount;
        this.inquiryCount = inquiryCount;
        this.couponCount = couponCount;
        this.depositBalance = depositBalance;
        this.rewardPoint = rewardPoint;
        this.memberNickname = memberNickname;
    }

}
