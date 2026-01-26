package com.yamoyo.be.domain.teamroom.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChangeLeaderRequest(
        @NotNull(message = "새 팀장 ID는 필수입니다.")
        Long newLeaderMemberId
) {}
