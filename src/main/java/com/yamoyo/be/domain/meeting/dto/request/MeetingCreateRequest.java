package com.yamoyo.be.domain.meeting.dto.request;

import com.yamoyo.be.domain.meeting.entity.enums.MeetingColor;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public record MeetingCreateRequest(
        @NotBlank(message = "회의 제목은 필수입니다")
        @Size(max = 100, message = "회의 제목은 100자 이내여야 합니다")
        String title,

        @Size(max = 500, message = "회의 설명은 500자 이내여야 합니다")
        String description,

        @Size(max = 100, message = "장소는 100자 이내여야 합니다")
        String location,

        @NotNull(message = "시작 시간은 필수입니다")
        @Future(message = "시작 시간은 현재 시간 이후여야 합니다")
        LocalDateTime startTime,

        @NotNull(message = "회의 시간은 필수입니다")
        @Min(value = 30, message = "회의 시간은 최소 30분 이상이어야 합니다")
        @Max(value = 240, message = "회의 시간은 최대 240분 이내여야 합니다")
        Integer durationMinutes,

        @NotNull(message = "색상은 필수입니다")
        MeetingColor color,

        @NotNull(message = "반복 여부는 필수입니다")
        Boolean isRecurring,

        @NotNull(message = "참석자는 필수입니다")
        @Size(min = 1, message = "참석자는 최소 1명 이상이어야 합니다")
        List<Long> participantUserIds
) {}
