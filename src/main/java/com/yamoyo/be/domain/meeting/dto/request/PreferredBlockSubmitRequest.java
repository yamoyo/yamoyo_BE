package com.yamoyo.be.domain.meeting.dto.request;

import com.yamoyo.be.domain.meeting.entity.enums.PreferredBlock;
import jakarta.validation.constraints.NotNull;

public record PreferredBlockSubmitRequest(
        @NotNull(message = "선호시간대는 필수입니다.")
        PreferredBlock preferredBlock
) {}
