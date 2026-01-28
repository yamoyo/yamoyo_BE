package com.yamoyo.be.domain.meeting.dto.request;

import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import com.yamoyo.be.domain.meeting.entity.enums.MeetingColor;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record MeetingUpdateRequest(
        @NotBlank(message = "회의 제목은 필수입니다")
        @Size(max = 100, message = "회의 제목은 100자 이내여야 합니다")
        String title,

        @NotNull(message = "회의 설명은 필수입니다")
        @Size(max = 500, message = "회의 설명은 500자 이내여야 합니다")
        String description,

        @NotNull(message = "장소는 필수입니다")
        @Size(max = 100, message = "장소는 100자 이내여야 합니다")
        String location,

        /**
         * [SINGLE scope 전용] 변경할 날짜
         * - SINGLE scope에서 필수
         * - THIS_AND_FUTURE scope에서는 무시됨
         */
        LocalDate startDate,

        /**
         * [THIS_AND_FUTURE scope 전용] 변경할 요일
         * - THIS_AND_FUTURE scope에서 필수
         * - SINGLE scope에서는 무시됨
         */
        DayOfWeek dayOfWeek,

        /**
         * [공통] 변경할 시작 시간
         * - 모든 scope에서 필수
         */
        @NotNull(message = "시작 시간은 필수입니다")
        LocalTime startTime,

        /**
         * [공통] 변경할 종료 시간
         * - 모든 scope에서 필수
         * - endTime < time이면 자정을 넘긴 것으로 판단
         */
        @NotNull(message = "종료 시간은 필수입니다")
        LocalTime endTime,

        @NotNull(message = "색상은 필수입니다")
        MeetingColor color,

        @NotNull(message = "참석자는 필수입니다")
        @Size(min = 1, message = "참석자는 최소 1명 이상이어야 합니다")
        List<Long> participantUserIds
) {}
