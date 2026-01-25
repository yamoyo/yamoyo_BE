package com.yamoyo.be.domain.meeting.service;

import com.yamoyo.be.domain.meeting.dto.request.AvailabilitySubmitRequest;
import com.yamoyo.be.domain.meeting.dto.request.PreferredBlockSubmitRequest;
import com.yamoyo.be.domain.meeting.dto.response.TimepickResponse;
import com.yamoyo.be.domain.meeting.entity.Timepick;
import com.yamoyo.be.domain.meeting.entity.TimepickParticipant;
import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import com.yamoyo.be.domain.meeting.repository.TimepickParticipantRepository;
import com.yamoyo.be.domain.meeting.repository.TimepickRepository;
import com.yamoyo.be.domain.meeting.util.AvailabilityBitmapConverter;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimepickService {

    private final TimepickRepository timepickRepository;
    private final TimepickParticipantRepository timepickParticipantRepository;
    private final UserTimepickDefaultService userTimepickDefaultService;

    public TimepickResponse getTimepick(Long teamRoomId, Long userId) {
        Timepick timepick = timepickRepository.findByTeamRoomId(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TIMEPICK_NOT_FOUND));

        TimepickParticipant participant = timepickParticipantRepository
                .findByTimepickIdAndUserId(timepick.getId(), userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TIMEPICK_NOT_PARTICIPANT));

        log.info("타임픽 조회 - TeamRoomId: {}, UserId: {}, TimepickId: {}", teamRoomId, userId, timepick.getId());

        return TimepickResponse.from(timepick, participant);
    }

    @Transactional
    public void submitAvailability(Long teamRoomId, Long userId, AvailabilitySubmitRequest request) {
        Timepick timepick = timepickRepository.findByTeamRoomId(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TIMEPICK_NOT_FOUND));

        if (timepick.isFinalized()) {
            throw new YamoyoException(ErrorCode.TIMEPICK_NOT_OPEN);
        }

        TimepickParticipant participant = timepickParticipantRepository
                .findByTimepickIdAndUserId(timepick.getId(), userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TIMEPICK_NOT_PARTICIPANT));

        if (participant.hasSubmittedAvailability()) {
            throw new YamoyoException(ErrorCode.TIMEPICK_AVAILABILITY_ALREADY_SUBMITTED);
        }

        Map<DayOfWeek, boolean[]> availabilityMap = request.availability().toDayOfWeekMap();
        Map<DayOfWeek, Long> bitmaps = AvailabilityBitmapConverter.toBitmaps(availabilityMap);

        participant.submitAvailability(bitmaps);
        userTimepickDefaultService.updateAvailability(userId, bitmaps);

        log.info("가용시간 제출 완료 - TeamRoomId: {}, UserId: {}", teamRoomId, userId);
    }

    @Transactional
    public void submitPreferredBlock(Long teamRoomId, Long userId, PreferredBlockSubmitRequest request) {
        Timepick timepick = timepickRepository.findByTeamRoomId(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TIMEPICK_NOT_FOUND));

        if (timepick.isFinalized()) {
            throw new YamoyoException(ErrorCode.TIMEPICK_NOT_OPEN);
        }

        TimepickParticipant participant = timepickParticipantRepository
                .findByTimepickIdAndUserId(timepick.getId(), userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TIMEPICK_NOT_PARTICIPANT));

        if (participant.hasSubmittedPreferredBlock()) {
            throw new YamoyoException(ErrorCode.TIMEPICK_PREFERRED_BLOCK_ALREADY_SUBMITTED);
        }

        participant.submitPreferredBlock(request.preferredBlock());
        userTimepickDefaultService.updatePreferredBlock(userId, request.preferredBlock());

        log.info("선호시간대 제출 완료 - TeamRoomId: {}, UserId: {}", teamRoomId, userId);
    }
}
