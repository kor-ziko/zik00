package com.zik00.shop.dto.auth;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationDetailRequest {
    @NotBlank(message = "이름(한자)을 입력해주세요.")
    @Size(max = 100, message = "이름은 100자 이하로 입력해주세요.")
    private String nameKanji;

    @NotBlank(message = "이름(카타카나)을 입력해주세요.")
    @Size(max = 100, message = "카타카나 이름은 100자 이하로 입력해주세요.")
    @Pattern(regexp = "^[ァ-ヶー・　\\s]+$", message = "이름은 전각 카타카나로 입력해주세요.")
    private String nameKatakana;

    @NotNull(message = "생년월일을 입력해주세요.")
    @Past(message = "생년월일은 오늘보다 이전 날짜여야 합니다.")
    private LocalDate birthDate;

    @NotBlank(message = "성별을 선택해주세요.")
    @Pattern(regexp = "^(남자|여자|기타)$", message = "성별을 올바르게 선택해주세요.")
    private String gender;

    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(max = 100, message = "닉네임은 100자 이하로 입력해주세요.")
    private String nickname;

    @NotBlank(message = "우편번호를 조회해주세요.")
    @Pattern(regexp = "^\\d{3}-?\\d{4}$", message = "일본 우편번호 7자리를 입력해주세요.")
    private String zipCode;

    @NotBlank(message = "도도부현을 선택해주세요.")
    @Size(max = 100)
    private String province;

    @NotBlank(message = "우편번호로 주소를 조회해주세요.")
    @Size(max = 255)
    private String baseAddress;

    @NotBlank(message = "상세 주소를 입력해주세요.")
    @Size(max = 150, message = "상세 주소는 150자 이하로 입력해주세요.")
    private String detailAddress;

    @NotBlank(message = "일반전화 번호를 입력해주세요.")
    @Pattern(
            regexp = "^0\\d{1,4}-?\\d{1,4}-?\\d{4}$",
            message = "일반전화 번호 형식을 확인해주세요. 예: 02-123-1234"
    )
    private String telephone;

    @NotBlank(message = "휴대전화 번호를 입력해주세요.")
    @Pattern(
            regexp = "^(070|080|090)-?\\d{4}-?\\d{4}$",
            message = "휴대전화 번호 형식을 확인해주세요. 예: 090-1234-1234"
    )
    private String mobilePhone;

}
