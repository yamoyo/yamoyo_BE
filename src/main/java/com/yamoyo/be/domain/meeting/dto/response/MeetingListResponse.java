package com.yamoyo.be.domain.meeting.dto.response;

import com.yamoyo.be.domain.meeting.entity.Meeting;
import com.yamoyo.be.domain.meeting.entity.enums.MeetingColor;
import com.yamoyo.be.domain.meeting.entity.enums.MeetingType;

import java.time.LocalDateTime;
import java.util.List;

public record MeetingListResponse(
        Integer year,
        Integer month,
        List<MeetingSummary> meetings
) {
    public record MeetingSummary(
            Long meetingId,
            Long meetingSeriesId,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer durationMinutes,
            MeetingColor color,
            MeetingType meetingType,
            Boolean isIndividuallyModified,
            Integer participantCount
    ) {
        public static MeetingSummary from(Meeting meeting, int participantCount) {
            return new MeetingSummary(
                    meeting.getId(),
                    meeting.getMeetingSeries().getId(),
                    meeting.getTitle(),
                    meeting.getStartTime(),
                    meeting.getStartTime().plusMinutes(meeting.getDurationMinutes()),
                    meeting.getDurationMinutes(),
                    meeting.getColor(),
                    meeting.getMeetingSeries().getMeetingType(),
                    meeting.getIsIndividuallyModified(),
                    participantCount
            );
        }
    }
}
