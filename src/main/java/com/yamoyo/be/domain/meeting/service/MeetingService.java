package com.yamoyo.be.domain.meeting.service;

import com.yamoyo.be.domain.meeting.dto.request.MeetingCreateRequest;
import com.yamoyo.be.domain.meeting.dto.request.MeetingUpdateRequest;
import com.yamoyo.be.domain.meeting.dto.response.MeetingCreateResponse;
import com.yamoyo.be.domain.meeting.dto.response.MeetingDeleteResponse;
import com.yamoyo.be.domain.meeting.dto.response.MeetingDetailResponse;
import com.yamoyo.be.domain.meeting.dto.response.MeetingListResponse;
import com.yamoyo.be.domain.meeting.dto.response.MeetingUpdateResponse;
import com.yamoyo.be.domain.meeting.entity.Meeting;
import com.yamoyo.be.domain.meeting.entity.MeetingParticipant;
import com.yamoyo.be.domain.meeting.entity.MeetingSeries;
import com.yamoyo.be.domain.meeting.entity.WeeklyAvailability;
import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import com.yamoyo.be.domain.meeting.entity.enums.MeetingColor;
import com.yamoyo.be.domain.meeting.entity.enums.MeetingType;
import com.yamoyo.be.domain.meeting.entity.enums.UpdateScope;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
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

    // =============================================================================
    // Public 메서드
    // =============================================================================

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

    @Transactional
    public MeetingUpdateResponse updateMeeting(Long meetingId, UpdateScope scope, MeetingUpdateRequest request, Long userId) {
        log.info("회의 수정 시작 - meetingId: {}, scope: {}, userId: {}", meetingId, scope, userId);

        Meeting meeting = meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.MEETING_NOT_FOUND));

        MeetingSeries meetingSeries = meeting.getMeetingSeries();
        Long teamRoomId = meetingSeries.getTeamRoom().getId();
        MeetingType meetingType = meetingSeries.getMeetingType();

        validatePermission(meeting, teamRoomId, userId, meetingType, ErrorCode.MEETING_EDIT_FORBIDDEN);
        validateUpdateScope(meetingType, scope);
        validateColorChange(meetingType, meeting.getColor(), request.color());
        validateParticipantsAreTeamMembers(teamRoomId, request.participantUserIds());
        validateTimeFieldsByScope(scope, request);

        List<Long> updatedMeetingIds = new ArrayList<>();
        List<Long> skippedMeetingIds = new ArrayList<>();

        if (scope == UpdateScope.SINGLE) {
            updateMeetingWithDate(meeting, request);
            updatedMeetingIds.add(meeting.getId());
        } else {
            List<Meeting> futureMeetings = meetingRepository.findByMeetingSeriesIdAndStartTimeGreaterThanEqual(
                    meetingSeries.getId(), meeting.getStartTime());

            for (Meeting futureMeeting : futureMeetings) {
                if (futureMeeting.isIndividuallyModified()) {
                    skippedMeetingIds.add(futureMeeting.getId());
                } else {
                    updateMeetingWithDayOfWeek(futureMeeting, request);
                    updatedMeetingIds.add(futureMeeting.getId());
                }
            }
        }

        log.info("회의 수정 완료 - 수정된 회의 수: {}, 건너뛴 회의 수: {}",
                updatedMeetingIds.size(), skippedMeetingIds.size());

        return new MeetingUpdateResponse(scope, updatedMeetingIds.size(), updatedMeetingIds, skippedMeetingIds);
    }

    @Transactional
    public MeetingDeleteResponse deleteMeeting(Long meetingId, UpdateScope scope, Long userId) {
        log.info("회의 삭제 시작 - meetingId: {}, scope: {}, userId: {}", meetingId, scope, userId);

        Meeting meeting = meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.MEETING_NOT_FOUND));

        MeetingSeries meetingSeries = meeting.getMeetingSeries();
        Long teamRoomId = meetingSeries.getTeamRoom().getId();
        MeetingType meetingType = meetingSeries.getMeetingType();

        validatePermission(meeting, teamRoomId, userId, meetingType, ErrorCode.MEETING_DELETE_FORBIDDEN);
        validateDeleteScope(meetingType, scope);

        List<Long> deletedMeetingIds;
        if (scope == UpdateScope.SINGLE) {
            deletedMeetingIds = deleteSingleMeeting(meeting);
        } else {
            deletedMeetingIds = deleteThisAndFutureMeetings(meetingSeries, meeting.getStartTime());
        }

        boolean seriesDeleted = checkAndDeleteSeriesIfEmpty(meetingSeries);

        log.info("회의 삭제 완료 - 삭제된 회의 수: {}, 시리즈 삭제 여부: {}",
                deletedMeetingIds.size(), seriesDeleted);

        return new MeetingDeleteResponse(scope, deletedMeetingIds.size(), deletedMeetingIds, seriesDeleted);
    }

    // =============================================================================
    // Private 메서드 - Finder
    // =============================================================================

    private TeamRoom findTeamRoom(Long teamRoomId) {
        return teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));
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

    // =============================================================================
    // Private 메서드 - Validation
    // =============================================================================

    private void validateUserIsTeamMember(Long teamRoomId, Long userId) {
        teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));
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

    private void validatePermission(Meeting meeting, Long teamRoomId, Long userId,
                                     MeetingType meetingType, ErrorCode errorCode) {
        TeamMember teamMember = teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));
        if (!hasPermission(meeting, userId, meetingType, teamMember)) {
            throw new YamoyoException(errorCode);
        }
    }

    private boolean hasPermission(Meeting meeting, Long userId, MeetingType meetingType, TeamMember teamMember) {
        if (meetingType == MeetingType.INITIAL_REGULAR) {
            return teamMember.getTeamRole() == TeamRole.LEADER;
        }
        return meetingParticipantRepository.existsByMeetingIdAndUserId(meeting.getId(), userId);
    }

    private void validateUpdateScope(MeetingType meetingType, UpdateScope scope) {
        if (meetingType == MeetingType.ADDITIONAL_ONE_TIME && scope == UpdateScope.THIS_AND_FUTURE) {
            throw new YamoyoException(ErrorCode.MEETING_INVALID_UPDATE_SCOPE);
        }
    }

    private void validateColorChange(MeetingType meetingType, MeetingColor currentColor, MeetingColor newColor) {
        if (meetingType == MeetingType.INITIAL_REGULAR && currentColor != newColor) {
            throw new YamoyoException(ErrorCode.MEETING_COLOR_CHANGE_NOT_ALLOWED);
        }
    }

    private void validateTimeFieldsByScope(UpdateScope scope, MeetingUpdateRequest request) {
        if (scope == UpdateScope.SINGLE) {
            if (request.startDate() == null) {
                throw new YamoyoException(ErrorCode.MEETING_SINGLE_SCOPE_REQUIRES_DATE);
            }
        } else {
            if (request.dayOfWeek() == null) {
                throw new YamoyoException(ErrorCode.MEETING_FUTURE_SCOPE_REQUIRES_DAY_OF_WEEK);
            }
        }
    }

    private void validateDeleteScope(MeetingType meetingType, UpdateScope scope) {
        if (meetingType == MeetingType.ADDITIONAL_ONE_TIME && scope == UpdateScope.THIS_AND_FUTURE) {
            throw new YamoyoException(ErrorCode.MEETING_INVALID_DELETE_SCOPE);
        }
    }

    private void validateDurationMinutes(int durationMinutes) {
        if (durationMinutes % WeeklyAvailability.SLOT_DURATION_MINUTES != 0) {
            throw new YamoyoException(ErrorCode.MEETING_INVALID_DURATION);
        }
        if (durationMinutes < 30 || durationMinutes > 240) {
            throw new YamoyoException(ErrorCode.MEETING_DURATION_OUT_OF_RANGE);
        }
    }

    // =============================================================================
    // Private 메서드 - Permission
    // =============================================================================

    private boolean determineEditDeletePermission(Meeting meeting, TeamMember teamMember, Long userId) {
        MeetingType meetingType = meeting.getMeetingSeries().getMeetingType();

        if (meetingType == MeetingType.INITIAL_REGULAR) {
            return teamMember.getTeamRole() == TeamRole.LEADER;
        } else {
            return meetingParticipantRepository.existsByMeetingIdAndUserId(meeting.getId(), userId);
        }
    }

    // =============================================================================
    // Private 메서드 - Create 관련
    // =============================================================================

    private MeetingSeries createMeetingSeries(TeamRoom teamRoom, MeetingCreateRequest request, User creator) {
        MeetingType meetingType = request.isRecurring()
                ? MeetingType.ADDITIONAL_RECURRING
                : MeetingType.ADDITIONAL_ONE_TIME;
        DayOfWeek dayOfWeek = DayOfWeek.from(request.startDate().getDayOfWeek());
        int durationMinutes = calculateDurationMinutes(request.startTime(), request.endTime());
        validateDurationMinutes(durationMinutes);

        MeetingSeries meetingSeries = MeetingSeries.create(
                teamRoom,
                meetingType,
                dayOfWeek,
                request.startTime(),
                durationMinutes,
                creator.getName()
        );
        meetingSeriesRepository.save(meetingSeries);
        return meetingSeries;
    }

    private List<Meeting> createMeetingInstances(MeetingSeries meetingSeries, MeetingCreateRequest request,
                                                  LocalDateTime deadline) {
        List<Meeting> meetings = new ArrayList<>();
        LocalDateTime startDateTime = LocalDateTime.of(request.startDate(), request.startTime());
        int durationMinutes = calculateDurationMinutes(request.startTime(), request.endTime());

        if (!request.isRecurring()) {
            Meeting meeting = Meeting.create(
                    meetingSeries,
                    request.title(),
                    request.location(),
                    startDateTime,
                    durationMinutes,
                    request.color(),
                    request.description()
            );
            meetings.add(meeting);
        } else {
            LocalDateTime currentStartTime = startDateTime;
            while (!currentStartTime.isAfter(deadline)) {
                Meeting meeting = Meeting.create(
                        meetingSeries,
                        request.title(),
                        request.location(),
                        currentStartTime,
                        durationMinutes,
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

        List<MeetingParticipant> allParticipants = meetings.stream()
                .flatMap(meeting -> participants.stream()
                        .map(participant -> MeetingParticipant.create(meeting, participant)))
                .toList();

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

    // =============================================================================
    // Private 메서드 - Update 관련
    // =============================================================================

    private int calculateDurationMinutes(LocalTime startTime, LocalTime endTime) {
        int startMinutes = startTime.getHour() * 60 + startTime.getMinute();
        int endMinutes = endTime.getHour() * 60 + endTime.getMinute();

        if (endMinutes > startMinutes) {
            // 같은 날: 14:00 → 16:00 = 120분
            return endMinutes - startMinutes;
        } else {
            // 자정 넘김: 22:00 → 02:00 = 240분
            return (24 * 60 - startMinutes) + endMinutes;
        }
    }

    private void updateMeetingWithDate(Meeting meeting, MeetingUpdateRequest request) {
        LocalDateTime newStartTime = LocalDateTime.of(request.startDate(), request.startTime());
        int durationMinutes = calculateDurationMinutes(request.startTime(), request.endTime());

        meeting.update(
                request.title(),
                request.description(),
                request.location(),
                newStartTime,
                durationMinutes,
                request.color()
        );
        meeting.markAsIndividuallyModified();

        updateMeetingParticipants(meeting, request.participantUserIds());
    }

    private void updateMeetingWithDayOfWeek(Meeting meeting, MeetingUpdateRequest request) {
        LocalDate currentDate = meeting.getStartTime().toLocalDate();
        java.time.DayOfWeek targetDayOfWeek = request.dayOfWeek().toJavaDayOfWeek();

        LocalDate newDate;
        if (targetDayOfWeek.getValue() < currentDate.getDayOfWeek().getValue()) {
            newDate = currentDate.with(TemporalAdjusters.previousOrSame(targetDayOfWeek));
        } else {
            newDate = currentDate.with(TemporalAdjusters.nextOrSame(targetDayOfWeek));
        }

        LocalDateTime newStartTime = LocalDateTime.of(newDate, request.startTime());
        int durationMinutes = calculateDurationMinutes(request.startTime(), request.endTime());

        meeting.update(
                request.title(),
                request.description(),
                request.location(),
                newStartTime,
                durationMinutes,
                request.color()
        );

        updateMeetingParticipants(meeting, request.participantUserIds());
    }

    private void updateMeetingParticipants(Meeting meeting, List<Long> participantUserIds) {
        meetingParticipantRepository.deleteByMeetingId(meeting.getId());

        List<User> participants = userRepository.findAllById(participantUserIds);
        List<MeetingParticipant> newParticipants = participants.stream()
                .map(user -> MeetingParticipant.create(meeting, user))
                .collect(Collectors.toList());
        meetingParticipantRepository.saveAll(newParticipants);
    }

    // =============================================================================
    // Private 메서드 - Delete 관련
    // =============================================================================

    private List<Long> deleteSingleMeeting(Meeting meeting) {
        Long meetingId = meeting.getId();
        meetingParticipantRepository.deleteByMeetingId(meetingId);
        meetingRepository.delete(meeting);
        return List.of(meetingId);
    }

    private List<Long> deleteThisAndFutureMeetings(MeetingSeries meetingSeries, LocalDateTime startTime) {
        List<Meeting> meetingsToDelete = meetingRepository.findByMeetingSeriesIdAndStartTimeGreaterThanEqual(
                meetingSeries.getId(), startTime);

        List<Long> meetingIds = meetingsToDelete.stream()
                .map(Meeting::getId)
                .collect(Collectors.toList());

        if (!meetingIds.isEmpty()) {
            meetingParticipantRepository.deleteByMeetingIdIn(meetingIds);
            meetingRepository.deleteAll(meetingsToDelete);
        }

        return meetingIds;
    }

    private boolean checkAndDeleteSeriesIfEmpty(MeetingSeries meetingSeries) {
        long remainingCount = meetingRepository.countByMeetingSeriesId(meetingSeries.getId());
        if (remainingCount == 0) {
            meetingSeriesRepository.delete(meetingSeries);
            return true;
        }
        return false;
    }
}
