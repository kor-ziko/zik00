package com.zik00.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InquiryCommentCreateRequest {
    @NotBlank(message = "\uB313\uAE00 \uB0B4\uC6A9\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    @Size(max = 2000, message = "\uB313\uAE00 \uB0B4\uC6A9\uC740 2000\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    private String content;

}
