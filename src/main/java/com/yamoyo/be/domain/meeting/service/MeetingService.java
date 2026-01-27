package com.yamoyo.be.domain.meeting.service;

import com.yamoyo.be.domain.meeting.dto.request.MeetingCreateRequest;
import com.yamoyo.be.domain.meeting.dto.response.MeetingCreateResponse;
import com.yamoyo.be.domain.meeting.entity.Meeting;
import com.yamoyo.be.domain.meeting.entity.MeetingParticipant;
import com.yamoyo.be.domain.meeting.entity.MeetingSeries;
import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import com.yamoyo.be.domain.meeting.entity.enums.MeetingColor;
import com.yamoyo.be.domain.meeting.entity.enums.MeetingType;
import com.yamoyo.be.domain.meeting.repository.MeetingParticipantRepository;
import com.yamoyo.be.domain.meeting.repository.MeetingRepository;
import com.yamoyo.be.domain.meeting.repository.MeetingSeriesRepository;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingSeriesRepository meetingSeriesRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final TeamRoomRepository teamRoomRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public MeetingCreateResponse createMeeting(Long teamRoomId, MeetingCreateRequest request, Long userId) {
        log.info("회의 생성 시작 - teamRoomId: {}, userId: {}", teamRoomId, userId);

        // 1. 팀룸 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 요청자 팀원 검증
        teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        // 3. 요청자 User 조회 (creatorName용)
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));

        // 4. 시간 검증: startTime 분이 0 또는 30
        int startMinute = request.startTime().getMinute();
        if (startMinute != 0 && startMinute != 30) {
            throw new YamoyoException(ErrorCode.MEETING_INVALID_START_TIME);
        }

        // 5. 시간 검증: durationMinutes가 30 단위
        if (request.durationMinutes() % 30 != 0) {
            throw new YamoyoException(ErrorCode.MEETING_INVALID_DURATION);
        }

        // 6. 색상 검증: PURPLE 제외
        if (request.color() == MeetingColor.PURPLE) {
            throw new YamoyoException(ErrorCode.MEETING_PURPLE_COLOR_FORBIDDEN);
        }

        // 7. 참석자 검증: 모두 팀원인지
        validateParticipantsAreTeamMembers(teamRoomId, request.participantUserIds());

        // 8. MeetingSeries 생성 및 저장
        MeetingType meetingType = request.isRecurring()
                ? MeetingType.ADDITIONAL_RECURRING
                : MeetingType.ADDITIONAL_ONE_TIME;
        DayOfWeek dayOfWeek = convertToDayOfWeek(request.startTime().getDayOfWeek());

        MeetingSeries meetingSeries = MeetingSeries.builder()
                .teamRoom(teamRoom)
                .meetingType(meetingType)
                .dayOfWeek(dayOfWeek)
                .defaultStartTime(request.startTime().toLocalTime())
                .defaultDurationMinutes(request.durationMinutes())
                .creatorName(creator.getName())
                .build();
        meetingSeriesRepository.save(meetingSeries);

        // 9. Meeting 생성 및 저장
        List<Meeting> meetings = createMeetings(meetingSeries, request, teamRoom.getDeadline());
        meetingRepository.saveAll(meetings);

        // 10. MeetingParticipant 일괄 생성 및 저장
        List<User> participants = userRepository.findAllById(request.participantUserIds());
        List<MeetingParticipant> allParticipants = new ArrayList<>();
        for (Meeting meeting : meetings) {
            for (User participant : participants) {
                MeetingParticipant meetingParticipant = MeetingParticipant.builder()
                        .meeting(meeting)
                        .user(participant)
                        .build();
                allParticipants.add(meetingParticipant);
            }
        }
        meetingParticipantRepository.saveAll(allParticipants);

        log.info("회의 생성 완료 - meetingSeriesId: {}, 생성된 회의 수: {}",
                meetingSeries.getId(), meetings.size());

        // 11. MeetingCreateResponse 반환
        List<Long> meetingIds = meetings.stream()
                .map(Meeting::getId)
                .collect(Collectors.toList());

        return new MeetingCreateResponse(
                meetingSeries.getId(),
                meetingIds,
                meetings.size()
        );
    }

    private void validateParticipantsAreTeamMembers(Long teamRoomId, List<Long> participantUserIds) {
        Set<Long> teamMemberUserIds = teamMemberRepository.findByTeamRoomId(teamRoomId).stream()
                .map(member -> member.getUser().getId())
                .collect(Collectors.toSet());

        for (Long participantUserId : participantUserIds) {
            if (!teamMemberUserIds.contains(participantUserId)) {
                throw new YamoyoException(ErrorCode.MEETING_PARTICIPANT_NOT_TEAM_MEMBER);
            }
        }
    }

    private DayOfWeek convertToDayOfWeek(java.time.DayOfWeek javaDayOfWeek) {
        return switch (javaDayOfWeek) {
            case MONDAY -> DayOfWeek.MON;
            case TUESDAY -> DayOfWeek.TUE;
            case WEDNESDAY -> DayOfWeek.WED;
            case THURSDAY -> DayOfWeek.THU;
            case FRIDAY -> DayOfWeek.FRI;
            case SATURDAY -> DayOfWeek.SAT;
            case SUNDAY -> DayOfWeek.SUN;
        };
    }

    private List<Meeting> createMeetings(MeetingSeries meetingSeries, MeetingCreateRequest request,
                                          LocalDateTime deadline) {
        List<Meeting> meetings = new ArrayList<>();

        if (!request.isRecurring()) {
            // 일회성: 1개만 생성
            Meeting meeting = Meeting.builder()
                    .meetingSeries(meetingSeries)
                    .title(request.title())
                    .location(request.location())
                    .startTime(request.startTime())
                    .durationMinutes(request.durationMinutes())
                    .color(request.color())
                    .description(request.description())
                    .isIndividuallyModified(false)
                    .build();
            meetings.add(meeting);
        } else {
            // 반복: deadline까지 매주 생성
            LocalDateTime currentStartTime = request.startTime();
            while (!currentStartTime.isAfter(deadline)) {
                Meeting meeting = Meeting.builder()
                        .meetingSeries(meetingSeries)
                        .title(request.title())
                        .location(request.location())
                        .startTime(currentStartTime)
                        .durationMinutes(request.durationMinutes())
                        .color(request.color())
                        .description(request.description())
                        .isIndividuallyModified(false)
                        .build();
                meetings.add(meeting);
                currentStartTime = currentStartTime.plusWeeks(1);
            }
        }

        return meetings;
    }
}
