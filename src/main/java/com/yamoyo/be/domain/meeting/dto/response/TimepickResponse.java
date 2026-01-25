package com.yamoyo.be.domain.meeting.dto.response;

import com.yamoyo.be.domain.meeting.entity.Timepick;
import com.yamoyo.be.domain.meeting.entity.TimepickParticipant;
import com.yamoyo.be.domain.meeting.entity.enums.TimepickStatus;

import java.time.LocalDateTime;

public record TimepickResponse(
        Long timepickId,
        TimepickStatus status,
        LocalDateTime deadline,
        MyTimepickStatusDto myStatus
) {
    public static TimepickResponse from(Timepick timepick, TimepickParticipant participant) {
        return new TimepickResponse(
                timepick.getId(),
                timepick.getStatus(),
                timepick.getDeadline(),
                MyTimepickStatusDto.from(participant)
        );
    }
}
