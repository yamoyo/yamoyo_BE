package com.yamoyo.be.domain.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "에브리타임 시간표 파싱 요청")
public record EverytimeParseRequest(
        @Schema(description = "에브리타임 공개 URL", example = "https://everytime.kr/@testuser")
        @NotBlank(message = "URL은 필수입니다.")
        @Pattern(
                regexp = "^https://everytime\\.kr/@[a-zA-Z0-9_]+$",
                message = "올바른 에브리타임 URL 형식이 아닙니다."
        )
        String url
) {
}
