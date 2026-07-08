package com.zik00.shop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileUpdateRequest {
    @NotBlank(message = "\uC774\uB984\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    @Size(max = 100, message = "\uC774\uB984\uC740 100\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    private String name;

    @NotBlank(message = "\uB2C9\uB124\uC784\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    @Size(max = 100, message = "\uB2C9\uB124\uC784\uC740 100\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    private String nickname;

    @NotBlank(message = "\uD734\uB300\uD3F0 \uBC88\uD638\uB97C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    @Size(max = 50, message = "\uD734\uB300\uD3F0 \uBC88\uD638\uB294 50\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    @Pattern(regexp = "^[0-9+()\\-\\s]+$", message = "\uD734\uB300\uD3F0 \uBC88\uD638 \uD615\uC2DD\uC744 \uD655\uC778\uD574\uC8FC\uC138\uC694.")
    private String mobilePhone;

    @NotBlank(message = "\uC774\uBA54\uC77C\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    @Email(message = "\uC774\uBA54\uC77C \uD615\uC2DD\uC744 \uD655\uC778\uD574\uC8FC\uC138\uC694.")
    @Size(max = 255, message = "\uC774\uBA54\uC77C\uC740 255\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    private String email;
    private boolean alarmConsent;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }
    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAlarmConsent() {
        return alarmConsent;
    }
    public void setAlarmConsent(boolean alarmConsent) {
        this.alarmConsent = alarmConsent;
    }
}
