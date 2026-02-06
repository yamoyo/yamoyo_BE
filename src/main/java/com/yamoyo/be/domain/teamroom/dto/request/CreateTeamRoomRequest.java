package com.yamoyo.be.domain.teamroom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Schema(description = "팀룸 생성/수정 요청")
public record CreateTeamRoomRequest (
    @Schema(description = "팀룸 제목", example = "야모요", maxLength = 20)
    @NotBlank(message = "팀룸 제목은 필수입니다")
    @Size(max = 20, message = "제목은 20자 이내여야 합니다.")
    String title,

    @Schema(description = "팀룸 설명", example = "모이자마자 완성되는 팀 세팅 플랫폼", maxLength = 50, nullable = true)
    @Size(max = 50, message = "설명은 최대 50자까지 입력할 수 있습니다.")
    String description,

    @Schema(description = "마감일", example = "2026-02-28T23:59:59")
    @NotNull(message = "마감일은 필수입니다")
    @Future(message = "마감일은 현재 시간 이후여야 합니다")
    LocalDateTime deadline,

    @Schema(description = "배너 이미지 ID", example = "1", nullable = true)
    Long bannerImageId
){}
