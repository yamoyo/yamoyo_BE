package com.yamoyo.be.domain.teamroom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "팀장 위임 요청")
public record ChangeLeaderRequest(
        @Schema(description = "팀장 권한 위임할 팀원 ID", example = "3")
        @NotNull(message = "새 팀장 ID는 필수입니다.")
        Long newLeaderMemberId
) {}
