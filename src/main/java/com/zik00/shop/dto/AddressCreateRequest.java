package com.zik00.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressCreateRequest {
    @NotBlank(message = "\uBC30\uC1A1\uC9C0\uBA85\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    @Size(max = 100, message = "\uBC30\uC1A1\uC9C0\uBA85\uC740 100\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    private String addressName;

    @NotBlank(message = "\uC218\uB839\uC778\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    @Size(max = 100, message = "\uC218\uB839\uC778\uC740 100\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    private String receiverName;

    @NotBlank(message = "\uC218\uB839\uC778 \uC804\uD654\uBC88\uD638\uB97C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    @Size(max = 50, message = "\uC218\uB839\uC778 \uC804\uD654\uBC88\uD638\uB294 50\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    @Pattern(regexp = "^[0-9+()\\-\\s]+$", message = "\uC218\uB839\uC778 \uC804\uD654\uBC88\uD638 \uD615\uC2DD\uC744 \uD655\uC778\uD574\uC8FC\uC138\uC694.")
    private String receiverPhone;

    @Size(max = 20, message = "\uC6B0\uD3B8\uBC88\uD638\uB294 20\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    private String zipCode;

    @Size(max = 100, message = "\uB3C4\uB3C4\uBD80\uD604\uC740 100\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    private String province;

    @Size(max = 255, message = "\uC870\uD68C \uC8FC\uC18C\uB294 255\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    private String baseAddress;

    @Size(max = 255, message = "\uC0C1\uC138 \uC8FC\uC18C\uB294 255\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    private String detailAddress;
    private boolean defaultAddress;

}
