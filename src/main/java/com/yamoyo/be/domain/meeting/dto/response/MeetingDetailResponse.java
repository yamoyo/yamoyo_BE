package com.yamoyo.be.domain.meeting.dto.response;

import com.yamoyo.be.domain.meeting.entity.Meeting;
import com.yamoyo.be.domain.meeting.entity.MeetingParticipant;
import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import com.yamoyo.be.domain.meeting.entity.enums.MeetingColor;
import com.yamoyo.be.domain.meeting.entity.enums.MeetingType;

import java.time.LocalDateTime;
import java.util.List;

public record MeetingDetailResponse(
        Long meetingId,
        String title,
        String description,
        String location,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer durationMinutes,
        MeetingColor color,
        Boolean isIndividuallyModified,
        Long meetingSeriesId,
        MeetingType meetingType,
        DayOfWeek dayOfWeek,
        String creatorName,
        Long teamRoomId,
        List<ParticipantInfo> participants,
        Boolean canModify
) {
    public record ParticipantInfo(
            Long userId,
            String name,
            Long profileImageId
    ) {
        public static ParticipantInfo from(MeetingParticipant participant) {
            return new ParticipantInfo(
                    participant.getUser().getId(),
                    participant.getUser().getName(),
                    participant.getUser().getProfileImageId()
            );
        }
    }

    public static MeetingDetailResponse from(
            Meeting meeting,
            List<MeetingParticipant> participants,
            boolean canModify
    ) {
        List<ParticipantInfo> participantInfos = participants.stream()
                .map(ParticipantInfo::from)
                .toList();

        LocalDateTime endTime = meeting.getStartTime().plusMinutes(meeting.getDurationMinutes());

        return new MeetingDetailResponse(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getLocation(),
                meeting.getStartTime(),
                endTime,
                meeting.getDurationMinutes(),
                meeting.getColor(),
                meeting.getIsIndividuallyModified(),
                meeting.getMeetingSeries().getId(),
                meeting.getMeetingSeries().getMeetingType(),
                meeting.getMeetingSeries().getDayOfWeek(),
                meeting.getMeetingSeries().getCreatorName(),
                meeting.getMeetingSeries().getTeamRoom().getId(),
                participantInfos,
                canModify
        );
    }
}
