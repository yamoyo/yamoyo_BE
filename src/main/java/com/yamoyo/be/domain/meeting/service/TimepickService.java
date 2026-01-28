package com.yamoyo.be.domain.meeting.service;

import com.yamoyo.be.domain.meeting.dto.request.AvailabilitySubmitRequest;
import com.yamoyo.be.domain.meeting.dto.request.PreferredBlockSubmitRequest;
import com.yamoyo.be.domain.meeting.dto.response.TimepickResponse;
import com.yamoyo.be.domain.meeting.entity.*;
import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import com.yamoyo.be.domain.meeting.entity.enums.MeetingType;
import com.yamoyo.be.domain.meeting.repository.*;
import com.yamoyo.be.domain.meeting.util.AvailabilityBitmapConverter;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimepickService {

    private static final int DEFAULT_DEADLINE_HOURS = 6;
    private static final int DEFAULT_MEETING_DURATION_MINUTES = 60;
    private static final String INITIAL_MEETING_TITLE = "정기회의";

    private final TimepickRepository timepickRepository;
    private final TimepickParticipantRepository timepickParticipantRepository;
    private final UserTimepickDefaultService userTimepickDefaultService;
    private final TeamRoomRepository teamRoomRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MeetingScheduleAlgorithmService meetingScheduleAlgorithmService;
    private final MeetingSeriesRepository meetingSeriesRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;

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

        checkAndFinalizeIfAllSubmitted(context.timepick().getId());
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

        checkAndFinalizeIfAllSubmitted(context.timepick().getId());
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

    /**
     * 타임픽을 마감하고 최초 정기회의를 자동 생성한다.
     *
     * [비즈니스 규칙]
     * - 마감 시 미응답자는 "전체 가능 + 20~24시 선호"로 간주
     * - 최적 시간은 참석 가능 인원 최대화를 최우선으로 선정
     * - 최초 정기회의는 PURPLE 색상, 팀장만 수정 가능
     * - 참석자는 마감 시점의 팀룸 멤버 전원
     *
     * [처리 순서]
     * 1. 미응답자 처리 (PENDING → EXPIRED)
     * 2. 최적 시간 계산
     * 3. 회의 시리즈 및 회차 생성
     * 4. 타임픽 상태 변경 (FINALIZED)
     */
    @Transactional
    public void finalizeTimepick(Long timepickId) {
        Timepick timepick = timepickRepository.findById(timepickId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TIMEPICK_NOT_FOUND));

        if (timepick.isFinalized()) {
            throw new YamoyoException(ErrorCode.TIMEPICK_ALREADY_FINALIZED);
        }

        List<TimepickParticipant> participants = timepickParticipantRepository
                .findByTimepickIdWithUser(timepickId);

        // 1. 미응답자 EXPIRED 처리
        participants.forEach(TimepickParticipant::expireIfPending);

        // 2. 최적 시간 계산
        MeetingScheduleAlgorithmService.ScheduleResult result =
                meetingScheduleAlgorithmService.calculateOptimalSchedule(participants);

        // 3. 회의 시리즈 및 회차 생성
        TeamRoom teamRoom = timepick.getTeamRoom();
        createInitialRegularMeetingSeries(teamRoom, result, participants);

        // 4. 타임픽 상태 변경
        timepick.markAsFinalized();

        log.info("타임픽 마감 완료 - TimepickId: {}, 최적시간: {} {}, 참석가능: {}명",
                timepickId, result.dayOfWeek(), result.startTime(), result.availableCount());
    }

    /**
     * 전원이 가용시간과 선호시간대를 모두 제출했는지 확인하고,
     * 전원 제출 시 타임픽을 자동 마감한다.
     */
    private void checkAndFinalizeIfAllSubmitted(Long timepickId) {
        List<TimepickParticipant> participants = timepickParticipantRepository
                .findByTimepickIdWithUser(timepickId);

        boolean allSubmitted = participants.stream()
                .allMatch(TimepickParticipant::hasSubmittedBoth);

        if (allSubmitted) {
            log.info("전원 제출 완료 - 자동 마감 시작. TimepickId: {}", timepickId);
            finalizeTimepickInternal(timepickId, participants);
        }
    }

    /**
     * 내부용 타임픽 마감 메서드.
     * 이미 조회된 참가자 목록을 재사용한다.
     */
    private void finalizeTimepickInternal(Long timepickId, List<TimepickParticipant> participants) {
        Timepick timepick = timepickRepository.findById(timepickId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TIMEPICK_NOT_FOUND));

        if (timepick.isFinalized()) {
            return;
        }

        MeetingScheduleAlgorithmService.ScheduleResult result =
                meetingScheduleAlgorithmService.calculateOptimalSchedule(participants);

        TeamRoom teamRoom = timepick.getTeamRoom();
        createInitialRegularMeetingSeries(teamRoom, result, participants);

        timepick.markAsFinalized();

        log.info("타임픽 자동 마감 완료 - TimepickId: {}, 최적시간: {} {}",
                timepickId, result.dayOfWeek(), result.startTime());
    }

    /**
     * 최초 정기회의 시리즈와 회차들을 생성한다.
     * 팀룸 deadline까지 매주 회의를 생성한다.
     */
    private void createInitialRegularMeetingSeries(
            TeamRoom teamRoom,
            MeetingScheduleAlgorithmService.ScheduleResult result,
            List<TimepickParticipant> participants) {

        // 회의 시리즈 생성
        MeetingSeries series = MeetingSeries.create(
                teamRoom,
                MeetingType.INITIAL_REGULAR,
                result.dayOfWeek(),
                result.startTime(),
                DEFAULT_MEETING_DURATION_MINUTES,
                "시스템"
        );
        meetingSeriesRepository.save(series);

        // 회의 회차 생성 (deadline까지 매주)
        List<LocalDate> meetingDates = calculateMeetingDates(
                result.dayOfWeek(),
                teamRoom.getDeadline().toLocalDate()
        );

        List<User> users = participants.stream()
                .map(TimepickParticipant::getUser)
                .toList();

        for (LocalDate meetingDate : meetingDates) {
            LocalDateTime startDateTime = LocalDateTime.of(meetingDate, result.startTime());

            Meeting meeting = Meeting.createInitialRegular(
                    series,
                    INITIAL_MEETING_TITLE,
                    startDateTime,
                    DEFAULT_MEETING_DURATION_MINUTES
            );
            meetingRepository.save(meeting);

            // 참석자 등록
            for (User user : users) {
                MeetingParticipant participant = MeetingParticipant.create(meeting, user);
                meetingParticipantRepository.save(participant);
            }
        }

        log.info("정기회의 생성 완료 - TeamRoomId: {}, 회차 수: {}", teamRoom.getId(), meetingDates.size());
    }

    /**
     * 다음 주부터 deadline까지 해당 요일의 날짜 목록을 계산한다.
     */
    private List<LocalDate> calculateMeetingDates(DayOfWeek dayOfWeek, LocalDate deadline) {
        List<LocalDate> dates = new ArrayList<>();
        java.time.DayOfWeek javaDayOfWeek = dayOfWeek.toJavaDayOfWeek();

        // 다음 주 해당 요일부터 시작
        LocalDate nextMeetingDate = LocalDate.now()
                .with(java.time.temporal.TemporalAdjusters.next(javaDayOfWeek));

        while (!nextMeetingDate.isAfter(deadline)) {
            dates.add(nextMeetingDate);
            nextMeetingDate = nextMeetingDate.plusWeeks(1);
        }

        return dates;
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
