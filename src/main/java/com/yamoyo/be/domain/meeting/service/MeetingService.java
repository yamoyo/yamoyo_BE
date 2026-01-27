package com.yamoyo.be.domain.meeting.service;

import com.yamoyo.be.domain.meeting.dto.request.MeetingCreateRequest;
import com.yamoyo.be.domain.meeting.dto.response.MeetingCreateResponse;
import com.yamoyo.be.domain.meeting.dto.response.MeetingListResponse;
import com.yamoyo.be.domain.meeting.entity.Meeting;
import com.yamoyo.be.domain.meeting.entity.MeetingParticipant;
import com.yamoyo.be.domain.meeting.entity.MeetingSeries;
import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

        // 4. 참석자 검증: 모두 팀원인지
        validateParticipantsAreTeamMembers(teamRoomId, request.participantUserIds());

        // 5. MeetingSeries 생성 및 저장
        MeetingType meetingType = request.isRecurring()
                ? MeetingType.ADDITIONAL_RECURRING
                : MeetingType.ADDITIONAL_ONE_TIME;
        DayOfWeek dayOfWeek = DayOfWeek.from(request.startTime().getDayOfWeek());

        MeetingSeries meetingSeries = MeetingSeries.create(
                teamRoom,
                meetingType,
                dayOfWeek,
                request.startTime().toLocalTime(),
                request.durationMinutes(),
                creator.getName()
        );
        meetingSeriesRepository.save(meetingSeries);

        // 6. Meeting 생성 및 저장
        List<Meeting> meetings = createMeetings(meetingSeries, request, teamRoom.getDeadline());
        meetingRepository.saveAll(meetings);

        // 7. MeetingParticipant 일괄 생성 및 저장
        List<User> participants = userRepository.findAllById(request.participantUserIds());
        List<MeetingParticipant> allParticipants = new ArrayList<>();
        for (Meeting meeting : meetings) {
            for (User participant : participants) {
                MeetingParticipant meetingParticipant = MeetingParticipant.create(meeting, participant);
                allParticipants.add(meetingParticipant);
            }
        }
        meetingParticipantRepository.saveAll(allParticipants);

        log.info("회의 생성 완료 - meetingSeriesId: {}, 생성된 회의 수: {}",
                meetingSeries.getId(), meetings.size());

        // 8. MeetingCreateResponse 반환
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

    public MeetingListResponse getMeetingList(Long teamRoomId, Long userId, Integer year, Integer month) {
        log.info("회의 목록 조회 시작 - teamRoomId: {}, userId: {}, year: {}, month: {}", teamRoomId, userId, year, month);

        // 1. 팀룸 존재 확인
        teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 팀원 권한 검증
        teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        // 3. year/month 검증
        if (year == null || month == null) {
            throw new YamoyoException(ErrorCode.MEETING_INVALID_YEAR_MONTH_PARAMETER);
        }
        YearMonth targetYearMonth = YearMonth.of(year, month);

        // 4. 월 범위 계산
        LocalDateTime startDateTime = targetYearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDateTime = targetYearMonth.plusMonths(1).atDay(1).atStartOfDay();

        // 5. 회의 목록 조회
        List<Meeting> meetings = meetingRepository.findByTeamRoomIdAndStartTimeBetween(
                teamRoomId, startDateTime, endDateTime);

        // 6. 참석자 수 일괄 조회 (N+1 방지)
        Map<Long, Integer> participantCountMap = getParticipantCountMap(meetings);

        // 7. MeetingListResponse 반환
        List<MeetingListResponse.MeetingSummary> meetingSummaries = meetings.stream()
                .map(meeting -> MeetingListResponse.MeetingSummary.from(
                        meeting,
                        participantCountMap.getOrDefault(meeting.getId(), 0)))
                .collect(Collectors.toList());

        log.info("회의 목록 조회 완료 - 조회된 회의 수: {}", meetingSummaries.size());

        return new MeetingListResponse(
                targetYearMonth.getYear(),
                targetYearMonth.getMonthValue(),
                meetingSummaries
        );
    }

    private Map<Long, Integer> getParticipantCountMap(List<Meeting> meetings) {
        if (meetings.isEmpty()) {
            return Map.of();
        }

        List<Long> meetingIds = meetings.stream()
                .map(Meeting::getId)
                .collect(Collectors.toList());

        List<Object[]> countResults = meetingParticipantRepository.countByMeetingIds(meetingIds);

        return countResults.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()
                ));
    }

    private List<Meeting> createMeetings(MeetingSeries meetingSeries, MeetingCreateRequest request,
                                          LocalDateTime deadline) {
        List<Meeting> meetings = new ArrayList<>();

        if (!request.isRecurring()) {
            // 일회성: 1개만 생성
            Meeting meeting = Meeting.create(
                    meetingSeries,
                    request.title(),
                    request.location(),
                    request.startTime(),
                    request.durationMinutes(),
                    request.color(),
                    request.description()
            );
            meetings.add(meeting);
        } else {
            // 반복: deadline까지 매주 생성
            LocalDateTime currentStartTime = request.startTime();
            while (!currentStartTime.isAfter(deadline)) {
                Meeting meeting = Meeting.create(
                        meetingSeries,
                        request.title(),
                        request.location(),
                        currentStartTime,
                        request.durationMinutes(),
                        request.color(),
                        request.description()
                );
                meetings.add(meeting);
                currentStartTime = currentStartTime.plusWeeks(1);
            }
        }

        return meetings;
    }
}
