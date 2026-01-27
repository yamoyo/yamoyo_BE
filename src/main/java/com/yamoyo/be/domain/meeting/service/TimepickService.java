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
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimepickService {

    private static final int DEFAULT_DEADLINE_HOURS = 6;

    private final TimepickRepository timepickRepository;
    private final TimepickParticipantRepository timepickParticipantRepository;
    private final UserTimepickDefaultService userTimepickDefaultService;
    private final TeamRoomRepository teamRoomRepository;
    private final TeamMemberRepository teamMemberRepository;

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
        TimepickSubmissionContext context = validateSubmissionContext(teamRoomId, userId);

        if (context.participant().hasSubmittedAvailability()) {
            throw new YamoyoException(ErrorCode.TIMEPICK_AVAILABILITY_ALREADY_SUBMITTED);
        }

        Map<DayOfWeek, boolean[]> availabilityMap = request.availability().toDayOfWeekMap();
        Map<DayOfWeek, Long> bitmaps = AvailabilityBitmapConverter.toBitmaps(availabilityMap);

        context.participant().submitAvailability(bitmaps);
        userTimepickDefaultService.updateAvailability(userId, bitmaps);

        log.info("가용시간 제출 완료 - TeamRoomId: {}, UserId: {}", teamRoomId, userId);
    }

    @Transactional
    public void submitPreferredBlock(Long teamRoomId, Long userId, PreferredBlockSubmitRequest request) {
        TimepickSubmissionContext context = validateSubmissionContext(teamRoomId, userId);

        if (context.participant().hasSubmittedPreferredBlock()) {
            throw new YamoyoException(ErrorCode.TIMEPICK_PREFERRED_BLOCK_ALREADY_SUBMITTED);
        }

        context.participant().submitPreferredBlock(request.preferredBlock());
        userTimepickDefaultService.updatePreferredBlock(userId, request.preferredBlock());

        log.info("선호시간대 제출 완료 - TeamRoomId: {}, UserId: {}", teamRoomId, userId);
    }

    @Transactional
    public void createTimepick(Long teamRoomId) {
        // 1. 이미 존재하는지 확인
        if (timepickRepository.findByTeamRoomId(teamRoomId).isPresent()) {
            throw new YamoyoException(ErrorCode.TIMEPICK_ALREADY_EXISTS);
        }

        // 2. TeamRoom 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 3. 마감시간 = 현재 + 6시간
        LocalDateTime deadline = LocalDateTime.now().plusHours(DEFAULT_DEADLINE_HOURS);

        // 4. Timepick 생성 (status = OPEN)
        Timepick timepick = Timepick.create(teamRoom, deadline);
        timepickRepository.save(timepick);

        // 5. 팀룸 멤버 조회 (스냅샷)
        List<TeamMember> members = teamMemberRepository.findByTeamRoomId(teamRoomId, Sort.by("createdAt"));

        // 6. TimepickParticipant 일괄 생성 (모두 PENDING)
        List<TimepickParticipant> participants = members.stream()
                .map(member -> TimepickParticipant.create(timepick, member.getUser()))
                .toList();
        timepickParticipantRepository.saveAll(participants);

        log.info("타임픽 생성 완료 - TeamRoomId: {}, TimepickId: {}, 참가자 수: {}",
                teamRoomId, timepick.getId(), participants.size());
    }

    private record TimepickSubmissionContext(Timepick timepick, TimepickParticipant participant) {}

    private TimepickSubmissionContext validateSubmissionContext(Long teamRoomId, Long userId) {
        Timepick timepick = timepickRepository.findByTeamRoomId(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TIMEPICK_NOT_FOUND));

        if (timepick.isFinalized()) {
            throw new YamoyoException(ErrorCode.TIMEPICK_NOT_OPEN);
        }

        TimepickParticipant participant = timepickParticipantRepository
                .findByTimepickIdAndUserId(timepick.getId(), userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TIMEPICK_NOT_PARTICIPANT));

        return new TimepickSubmissionContext(timepick, participant);
    }
}
