package com.yamoyo.be.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileSetupRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 10, message = "이름은 최대 10자까지 입력 가능합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "이름은 한글 또는 영문만 입력 가능합니다.")
        String name,

        @NotBlank(message = "전공은 필수입니다.")
        String major,

        @NotBlank(message = "MBTI는 필수입니다.")
        @Pattern(regexp = "^[A-Z]{4}$", message = "MBTI는 영문 대문자 4자로 입력해주세요. (예: INTJ)")
        String mbti,

        @NotNull(message = "프로필 이미지는 필수입니다.")
        Long profileImageId
) {}
