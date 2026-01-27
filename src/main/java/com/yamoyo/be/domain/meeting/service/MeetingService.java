package com.yamoyo.be.domain.meeting.service;

import com.yamoyo.be.domain.meeting.dto.request.MeetingCreateRequest;
import com.yamoyo.be.domain.meeting.dto.response.MeetingCreateResponse;
import com.yamoyo.be.domain.meeting.dto.response.MeetingDetailResponse;
import com.yamoyo.be.domain.meeting.dto.response.MeetingListResponse;
import com.yamoyo.be.domain.meeting.entity.Meeting;
import com.yamoyo.be.domain.meeting.entity.MeetingParticipant;
import com.yamoyo.be.domain.meeting.entity.MeetingSeries;
import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import com.yamoyo.be.domain.meeting.entity.enums.MeetingType;
import com.yamoyo.be.domain.meeting.repository.MeetingParticipantRepository;
import com.yamoyo.be.domain.meeting.repository.MeetingRepository;
import com.yamoyo.be.domain.meeting.repository.MeetingSeriesRepository;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.enums.TeamRole;
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

        TeamRoom teamRoom = findTeamRoom(teamRoomId);
        validateUserIsTeamMember(teamRoomId, userId);
        User creator = findUser(userId);
        validateParticipantsAreTeamMembers(teamRoomId, request.participantUserIds());

        MeetingSeries meetingSeries = createMeetingSeries(teamRoom, request, creator);
        List<Meeting> meetings = createMeetingInstances(meetingSeries, request, teamRoom.getDeadline());
        createMeetingParticipants(meetings, request.participantUserIds());

        log.info("회의 생성 완료 - meetingSeriesId: {}, 생성된 회의 수: {}",
                meetingSeries.getId(), meetings.size());

        return buildCreateResponse(meetingSeries, meetings);
    }

    private TeamRoom findTeamRoom(Long teamRoomId) {
        return teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));
    }

    private void validateUserIsTeamMember(Long teamRoomId, Long userId) {
        teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));
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

    private MeetingSeries createMeetingSeries(TeamRoom teamRoom, MeetingCreateRequest request, User creator) {
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
        return meetingSeries;
    }

    private List<Meeting> createMeetingInstances(MeetingSeries meetingSeries, MeetingCreateRequest request,
                                                  LocalDateTime deadline) {
        List<Meeting> meetings = new ArrayList<>();

        if (!request.isRecurring()) {
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

        meetingRepository.saveAll(meetings);
        return meetings;
    }

    private void createMeetingParticipants(List<Meeting> meetings, List<Long> participantUserIds) {
        List<User> participants = userRepository.findAllById(participantUserIds);
        List<MeetingParticipant> allParticipants = new ArrayList<>();

        for (Meeting meeting : meetings) {
            for (User participant : participants) {
                MeetingParticipant meetingParticipant = MeetingParticipant.create(meeting, participant);
                allParticipants.add(meetingParticipant);
            }
        }

        meetingParticipantRepository.saveAll(allParticipants);
    }

    private MeetingCreateResponse buildCreateResponse(MeetingSeries meetingSeries, List<Meeting> meetings) {
        List<Long> meetingIds = meetings.stream()
                .map(Meeting::getId)
                .collect(Collectors.toList());

        return new MeetingCreateResponse(
                meetingSeries.getId(),
                meetingIds,
                meetings.size()
        );
    }

    public MeetingListResponse getMeetingList(Long teamRoomId, Long userId, Integer year, Integer month) {
        log.info("회의 목록 조회 시작 - teamRoomId: {}, userId: {}, year: {}, month: {}", teamRoomId, userId, year, month);

        teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        if (year == null || month == null) {
            throw new YamoyoException(ErrorCode.MEETING_INVALID_YEAR_MONTH_PARAMETER);
        }
        YearMonth targetYearMonth = YearMonth.of(year, month);

        LocalDateTime startDateTime = targetYearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDateTime = targetYearMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<Meeting> meetings = meetingRepository.findByTeamRoomIdAndStartTimeBetween(
                teamRoomId, startDateTime, endDateTime);

        Map<Long, Integer> participantCountMap = getParticipantCountMap(meetings);

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

    public MeetingDetailResponse getMeetingDetail(Long meetingId, Long userId) {
        log.info("회의 상세 조회 시작 - meetingId: {}, userId: {}", meetingId, userId);

        Meeting meeting = meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.MEETING_NOT_FOUND));

        Long teamRoomId = meeting.getMeetingSeries().getTeamRoom().getId();
        TeamMember teamMember = teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        List<MeetingParticipant> participants = meetingParticipantRepository.findByMeetingIdWithUser(meetingId);

        boolean canEditAndDelete = determineEditDeletePermission(meeting, teamMember, userId);

        log.info("회의 상세 조회 완료 - meetingId: {}, canEdit: {}, canDelete: {}",
                meetingId, canEditAndDelete, canEditAndDelete);

        return MeetingDetailResponse.from(meeting, participants, canEditAndDelete);
    }

    private boolean determineEditDeletePermission(Meeting meeting, TeamMember teamMember, Long userId) {
        MeetingType meetingType = meeting.getMeetingSeries().getMeetingType();

        if (meetingType == MeetingType.INITIAL_REGULAR) {
            return teamMember.getTeamRole() == TeamRole.LEADER;
        } else {
            return meetingParticipantRepository.existsByMeetingIdAndUserId(meeting.getId(), userId);
        }
    }
}
