package com.yamoyo.be.domain.meeting.dto.response;

import com.yamoyo.be.domain.meeting.entity.TimepickParticipant;
import com.yamoyo.be.domain.meeting.entity.enums.TimepickParticipantStatus;

public record MyTimepickStatusDto(
        TimepickParticipantStatus availabilityStatus,
        TimepickParticipantStatus preferredBlockStatus
) {
    public static MyTimepickStatusDto from(TimepickParticipant participant) {
        return new MyTimepickStatusDto(
                participant.getAvailabilityStatus(),
                participant.getPreferredBlockStatus()
        );
    }
}
