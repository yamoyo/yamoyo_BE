package com.yamoyo.be.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "프로필 설정 요청")
public record ProfileSetupRequest(
        @Schema(description = "이름 (한글 또는 영문, 최대 10자)", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 10, message = "이름은 최대 10자까지 입력 가능합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "이름은 한글 또는 영문만 입력 가능합니다.")
        String name,

        @Schema(description = "전공", example = "컴퓨터공학", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "전공은 필수입니다.")
        String major,

        @Schema(description = "MBTI (16가지 유형)", example = "INTJ", requiredMode = Schema.RequiredMode.REQUIRED)
        @Pattern(
                regexp = "^(ISTJ|ISFJ|INFJ|INTJ|ISTP|ISFP|INFP|INTP|ESTP|ESFP|ENFP|ENTP|ESTJ|ESFJ|ENFJ|ENTJ)$",
                message = "MBTI는 16가지 유형 중 하나여야 합니다. (예: INTJ)"
        )
        String mbti,

        @Schema(description = "프로필 이미지 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "프로필 이미지는 필수입니다.")
        Long profileImageId
) {}
