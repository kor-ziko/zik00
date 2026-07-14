package com.zik00.shop.dto;

import lombok.Getter;

@Getter
public enum MypageSection {
    HOME("", "\uB9C8\uC774\uD398\uC774\uC9C0", "\uC1FC\uD551 \uD65C\uB3D9\uC744 \uD55C\uB208\uC5D0 \uD655\uC778\uD558\uC138\uC694."),
    ORDERS("orders", "\uAD6C\uB9E4\uB0B4\uC5ED", "\uC644\uB8CC\uB41C \uC8FC\uBB38\uACFC \uAD6C\uB9E4 \uB0B4\uC5ED\uC744 \uD655\uC778\uD569\uB2C8\uB2E4."),
    DELIVERIES("deliveries", "\uBC30\uC1A1\uC870\uD68C", "\uC8FC\uBB38 \uC0C1\uD488\uC758 \uBC30\uC1A1 \uC0C1\uD0DC\uB97C \uD655\uC778\uD569\uB2C8\uB2E4."),
    INQUIRIES("inquiries", "1:1 \uBB38\uC758\uB0B4\uC5ED", "\uB4F1\uB85D\uD55C \uBB38\uC758\uC640 \uB2F5\uBCC0 \uC0C1\uD0DC\uB97C \uD655\uC778\uD569\uB2C8\uB2E4."),
    COUPONS("coupons", "\uCFE0\uD3F0\uD568", "\uBCF4\uC720 \uCFE0\uD3F0\uACFC \uC0AC\uC6A9 \uAC00\uB2A5 \uAE30\uAC04\uC744 \uD655\uC778\uD569\uB2C8\uB2E4."),
    DEPOSITS("deposits", "\uC608\uCE58\uAE08 \uAD00\uB9AC", "\uC608\uCE58\uAE08 \uC794\uC561\uACFC \uC0AC\uC6A9 \uB0B4\uC5ED\uC744 \uD655\uC778\uD569\uB2C8\uB2E4."),
    PROFILE("profile", "\uD68C\uC6D0\uC815\uBCF4\uC218\uC815", "\uBC30\uC1A1\uC9C0, \uD734\uB300\uD3F0\uBC88\uD638, \uC54C\uB78C \uB3D9\uC758\uB97C \uC218\uC815\uD569\uB2C8\uB2E4.");

    private final String path;
    private final String title;
    private final String description;

    MypageSection(String path, String title, String description) {
        this.path = path;
        this.title = title;
        this.description = description;
    }

    public String getUrl() {
        return path.isEmpty() ? "/mypage" : "/mypage/" + path;
    }
}
