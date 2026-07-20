package com.zik00.shop.dto.mypage;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {
    @NotBlank(message = "\uC774\uB984\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    @Size(max = 100, message = "\uC774\uB984\uC740 100\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    private String name;

    @NotBlank(message = "\uB2C9\uB124\uC784\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    @Size(max = 100, message = "\uB2C9\uB124\uC784\uC740 100\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    private String nickname;

    @NotBlank(message = "일반전화 번호를 입력해주세요.")
    @Size(max = 50, message = "일반전화 번호는 50자 이하로 입력해주세요.")
    @Pattern(regexp = "^0\\d{1,4}-?\\d{1,4}-?\\d{4}$", message = "일반전화 번호 형식을 확인해주세요. 예: 02-123-1234")
    private String telephone;

    @NotBlank(message = "\uD734\uB300\uD3F0 \uBC88\uD638\uB97C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    @Size(max = 50, message = "\uD734\uB300\uD3F0 \uBC88\uD638\uB294 50\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    @Pattern(regexp = "^(070|080|090)-?\\d{4}-?\\d{4}$", message = "휴대전화 번호 형식을 확인해주세요. 예: 090-1234-1234")
    private String mobilePhone;

    @NotBlank(message = "\uC774\uBA54\uC77C\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    @Email(message = "\uC774\uBA54\uC77C \uD615\uC2DD\uC744 \uD655\uC778\uD574\uC8FC\uC138\uC694.")
    @Size(max = 255, message = "\uC774\uBA54\uC77C\uC740 255\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    private String email;
    private boolean alarmConsent;

}
