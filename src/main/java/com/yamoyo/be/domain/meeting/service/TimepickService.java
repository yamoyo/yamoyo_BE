package com.yamoyo.be.domain.meeting.service;

import com.yamoyo.be.domain.meeting.dto.response.TimepickResponse;
import com.yamoyo.be.domain.meeting.entity.Timepick;
import com.yamoyo.be.domain.meeting.entity.TimepickParticipant;
import com.yamoyo.be.domain.meeting.repository.TimepickParticipantRepository;
import com.yamoyo.be.domain.meeting.repository.TimepickRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimepickService {

    private final TimepickRepository timepickRepository;
    private final TimepickParticipantRepository timepickParticipantRepository;

    public TimepickResponse getTimepick(Long teamRoomId, Long userId) {
        Timepick timepick = timepickRepository.findByTeamRoomId(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TIMEPICK_NOT_FOUND));

        TimepickParticipant participant = timepickParticipantRepository
                .findByTimepickIdAndUserId(timepick.getId(), userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TIMEPICK_NOT_PARTICIPANT));

        log.info("타임픽 조회 - TeamRoomId: {}, UserId: {}, TimepickId: {}", teamRoomId, userId, timepick.getId());

        return TimepickResponse.from(timepick, participant);
    }
}
