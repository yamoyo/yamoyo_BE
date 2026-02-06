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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MeetingService 테스트")
@MockitoSettings(strictness = Strictness.LENIENT)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingSeriesRepository meetingSeriesRepository;

    @Mock
    private MeetingParticipantRepository meetingParticipantRepository;

    @Mock
    private TeamRoomRepository teamRoomRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MeetingService meetingService;

    // ==================== 공통 헬퍼 메서드 ====================

    private User createMockUser(Long userId, String name) {
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);
        given(user.getName()).willReturn(name);
        return user;
    }

    private TeamRoom createMockTeamRoom(Long teamRoomId, LocalDateTime deadline) {
        TeamRoom teamRoom = mock(TeamRoom.class);
        given(teamRoom.getId()).willReturn(teamRoomId);
        given(teamRoom.getDeadline()).willReturn(deadline);
        return teamRoom;
    }

    private TeamMember createMockTeamMember(Long memberId, User user) {
        TeamMember member = mock(TeamMember.class);
        given(member.getId()).willReturn(memberId);
        given(member.getUser()).willReturn(user);
        return member;
    }

    private MeetingCreateRequest createValidRequest(LocalDate startDate, LocalTime startTime, LocalTime endTime, boolean isRecurring) {
        return new MeetingCreateRequest(
                "스프린트 회고",
                "2주차 스프린트 회고",
                "줌 회의실",
                startDate,
                startTime,
                endTime,
                MeetingColor.YELLOW,
                isRecurring,
                Arrays.asList(1L, 2L)
        );
    }

    // ==================== 회의 생성 테스트 ====================

    @Nested
    @DisplayName("회의 생성")
    class CreateMeetingTest {

        @Test
        @DisplayName("일회성 회의 생성 성공")
        void createMeeting_OneTime_Success() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            LocalDate date = LocalDate.now().plusDays(7);
            LocalTime time = LocalTime.of(14, 0);
            LocalTime endTime = LocalTime.of(15, 0);
            LocalDateTime deadline = LocalDateTime.now().plusDays(30);

            MeetingCreateRequest request = createValidRequest(date, time, endTime, false);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, deadline);
            User creator = createMockUser(userId, "테스트유저");
            User participant1 = createMockUser(1L, "참석자1");
            User participant2 = createMockUser(2L, "참석자2");
            TeamMember creatorMember = createMockTeamMember(1L, creator);
            TeamMember member1 = createMockTeamMember(1L, participant1);
            TeamMember member2 = createMockTeamMember(2L, participant2);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(creatorMember));
            given(userRepository.findById(userId)).willReturn(Optional.of(creator));
            given(teamMemberRepository.findByTeamRoomId(teamRoomId))
                    .willReturn(Arrays.asList(member1, member2));
            given(userRepository.findAllById(anyList())).willReturn(Arrays.asList(participant1, participant2));
            given(meetingSeriesRepository.save(any(MeetingSeries.class)))
                    .willAnswer(invocation -> {
                        MeetingSeries series = invocation.getArgument(0);
                        return series;
                    });
            given(meetingRepository.saveAll(anyList()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            MeetingCreateResponse response = meetingService.createMeeting(teamRoomId, request, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.createdMeetingCount()).isEqualTo(1);
            assertThat(response.meetingIds()).hasSize(1);

            verify(teamRoomRepository).findById(teamRoomId);
            verify(teamMemberRepository).findByTeamRoomIdAndUserId(teamRoomId, userId);
            verify(userRepository).findById(userId);
            verify(meetingSeriesRepository).save(any(MeetingSeries.class));
            verify(meetingRepository).saveAll(anyList());
            verify(meetingParticipantRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("반복 회의 생성 성공 - deadline까지 매주 생성")
        void createMeeting_Recurring_Success() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            LocalDate date = LocalDate.now().plusDays(1);
            LocalTime time = LocalTime.of(14, 30);
            LocalTime endTime = LocalTime.of(15, 30);
            LocalDateTime deadline = LocalDateTime.now().plusWeeks(4); // 4주 후 deadline

            MeetingCreateRequest request = createValidRequest(date, time, endTime, true);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, deadline);
            User creator = createMockUser(userId, "테스트유저");
            User participant1 = createMockUser(1L, "참석자1");
            User participant2 = createMockUser(2L, "참석자2");
            TeamMember creatorMember = createMockTeamMember(1L, creator);
            TeamMember member1 = createMockTeamMember(1L, participant1);
            TeamMember member2 = createMockTeamMember(2L, participant2);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(creatorMember));
            given(userRepository.findById(userId)).willReturn(Optional.of(creator));
            given(teamMemberRepository.findByTeamRoomId(teamRoomId))
                    .willReturn(Arrays.asList(member1, member2));
            given(userRepository.findAllById(anyList())).willReturn(Arrays.asList(participant1, participant2));
            given(meetingSeriesRepository.save(any(MeetingSeries.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(meetingRepository.saveAll(anyList()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            MeetingCreateResponse response = meetingService.createMeeting(teamRoomId, request, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.createdMeetingCount()).isGreaterThanOrEqualTo(1);

            verify(meetingSeriesRepository).save(any(MeetingSeries.class));
            verify(meetingRepository).saveAll(anyList());
            verify(meetingParticipantRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("회의 생성 실패 - 팀룸 없음")
        void createMeeting_TeamRoomNotFound() {
            // given
            Long teamRoomId = 999L;
            Long userId = 1L;
            LocalDate date = LocalDate.now().plusDays(7);
            LocalTime time = LocalTime.of(14, 0);
            LocalTime endTime = LocalTime.of(15, 0);
            MeetingCreateRequest request = createValidRequest(date, time, endTime, false);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> meetingService.createMeeting(teamRoomId, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.TEAMROOM_NOT_FOUND.getMessage());

            verify(teamRoomRepository).findById(teamRoomId);
            verify(meetingSeriesRepository, never()).save(any());
        }

        @Test
        @DisplayName("회의 생성 실패 - 팀원이 아님")
        void createMeeting_NotTeamMember() {
            // given
            Long teamRoomId = 1L;
            Long userId = 999L;
            LocalDate date = LocalDate.now().plusDays(7);
            LocalTime time = LocalTime.of(14, 0);
            LocalTime endTime = LocalTime.of(15, 0);
            LocalDateTime deadline = LocalDateTime.now().plusDays(30);
            MeetingCreateRequest request = createValidRequest(date, time, endTime, false);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, deadline);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> meetingService.createMeeting(teamRoomId, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.NOT_TEAM_MEMBER.getMessage());

            verify(teamRoomRepository).findById(teamRoomId);
            verify(teamMemberRepository).findByTeamRoomIdAndUserId(teamRoomId, userId);
            verify(meetingSeriesRepository, never()).save(any());
        }

        @Test
        @DisplayName("회의 생성 실패 - 시작 시간이 30분 단위가 아님")
        void createMeeting_InvalidStartTime() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            LocalDate date = LocalDate.now().plusDays(7);
            LocalTime time = LocalTime.of(14, 15); // 15분 - 잘못된 시간
            LocalTime endTime = LocalTime.of(15, 15);
            LocalDateTime deadline = LocalDateTime.now().plusDays(30);

            MeetingCreateRequest request = createValidRequest(date, time, endTime, false);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, deadline);
            User creator = createMockUser(userId, "테스트유저");
            User participant1 = createMockUser(1L, "참석자1");
            User participant2 = createMockUser(2L, "참석자2");
            TeamMember creatorMember = createMockTeamMember(1L, creator);
            TeamMember member1 = createMockTeamMember(1L, participant1);
            TeamMember member2 = createMockTeamMember(2L, participant2);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(creatorMember));
            given(userRepository.findById(userId)).willReturn(Optional.of(creator));
            given(teamMemberRepository.findByTeamRoomId(teamRoomId))
                    .willReturn(Arrays.asList(member1, member2));
            given(meetingSeriesRepository.save(any(MeetingSeries.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when / then
            assertThatThrownBy(() -> meetingService.createMeeting(teamRoomId, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.MEETING_INVALID_START_TIME.getMessage());

            verify(meetingRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("회의 생성 실패 - 회의 시간이 30분 단위가 아님")
        void createMeeting_InvalidDuration() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            LocalDate date = LocalDate.now().plusDays(7);
            LocalTime time = LocalTime.of(14, 0);
            LocalTime endTime = LocalTime.of(14, 45); // 45분 - 잘못된 시간
            LocalDateTime deadline = LocalDateTime.now().plusDays(30);

            MeetingCreateRequest request = new MeetingCreateRequest(
                    "스프린트 회고",
                    "2주차 스프린트 회고",
                    "줌 회의실",
                    date,
                    time,
                    endTime,
                    MeetingColor.YELLOW,
                    false,
                    Arrays.asList(1L, 2L)
            );

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, deadline);
            User creator = createMockUser(userId, "테스트유저");
            User participant1 = createMockUser(1L, "참석자1");
            User participant2 = createMockUser(2L, "참석자2");
            TeamMember creatorMember = createMockTeamMember(1L, creator);
            TeamMember member1 = createMockTeamMember(1L, participant1);
            TeamMember member2 = createMockTeamMember(2L, participant2);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(creatorMember));
            given(userRepository.findById(userId)).willReturn(Optional.of(creator));
            given(teamMemberRepository.findByTeamRoomId(teamRoomId))
                    .willReturn(Arrays.asList(member1, member2));
            given(meetingSeriesRepository.save(any(MeetingSeries.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when / then
            assertThatThrownBy(() -> meetingService.createMeeting(teamRoomId, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.MEETING_INVALID_DURATION.getMessage());

            verify(meetingRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("회의 생성 실패 - PURPLE 색상 사용")
        void createMeeting_PurpleColorForbidden() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            LocalDate date = LocalDate.now().plusDays(7);
            LocalTime time = LocalTime.of(14, 0);
            LocalTime endTime = LocalTime.of(15, 0);
            LocalDateTime deadline = LocalDateTime.now().plusDays(30);

            MeetingCreateRequest request = new MeetingCreateRequest(
                    "스프린트 회고",
                    "2주차 스프린트 회고",
                    "줌 회의실",
                    date,
                    time,
                    endTime,
                    MeetingColor.PURPLE, // PURPLE - 금지된 색상
                    false,
                    Arrays.asList(1L, 2L)
            );

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, deadline);
            User creator = createMockUser(userId, "테스트유저");
            User participant1 = createMockUser(1L, "참석자1");
            User participant2 = createMockUser(2L, "참석자2");
            TeamMember creatorMember = createMockTeamMember(1L, creator);
            TeamMember member1 = createMockTeamMember(1L, participant1);
            TeamMember member2 = createMockTeamMember(2L, participant2);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(creatorMember));
            given(userRepository.findById(userId)).willReturn(Optional.of(creator));
            given(teamMemberRepository.findByTeamRoomId(teamRoomId))
                    .willReturn(Arrays.asList(member1, member2));
            given(meetingSeriesRepository.save(any(MeetingSeries.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when / then
            assertThatThrownBy(() -> meetingService.createMeeting(teamRoomId, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.MEETING_PURPLE_COLOR_FORBIDDEN.getMessage());

            verify(meetingRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("회의 생성 실패 - 참석자 중 팀원이 아닌 사용자")
        void createMeeting_ParticipantNotTeamMember() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            LocalDate date = LocalDate.now().plusDays(7);
            LocalTime time = LocalTime.of(14, 0);
            LocalTime endTime = LocalTime.of(15, 0);
            LocalDateTime deadline = LocalDateTime.now().plusDays(30);

            MeetingCreateRequest request = new MeetingCreateRequest(
                    "스프린트 회고",
                    "2주차 스프린트 회고",
                    "줌 회의실",
                    date,
                    time,
                    endTime,
                    MeetingColor.YELLOW,
                    false,
                    Arrays.asList(1L, 999L) // 999L은 팀원이 아님
            );

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, deadline);
            User creator = createMockUser(userId, "테스트유저");
            User participant1 = createMockUser(1L, "참석자1");
            TeamMember creatorMember = createMockTeamMember(1L, creator);
            TeamMember member1 = createMockTeamMember(1L, participant1);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(creatorMember));
            given(userRepository.findById(userId)).willReturn(Optional.of(creator));
            given(teamMemberRepository.findByTeamRoomId(teamRoomId))
                    .willReturn(Arrays.asList(member1)); // member1만 팀원

            // when / then
            assertThatThrownBy(() -> meetingService.createMeeting(teamRoomId, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.MEETING_PARTICIPANT_NOT_TEAM_MEMBER.getMessage());

            verify(meetingSeriesRepository, never()).save(any());
        }
    }

    // ==================== 회의 목록 조회 테스트 ====================

    @Nested
    @DisplayName("회의 목록 조회")
    class GetMeetingListTest {

        private Meeting createMockMeeting(Long meetingId, MeetingSeries meetingSeries, LocalDateTime startTime) {
            Meeting meeting = mock(Meeting.class);
            given(meeting.getId()).willReturn(meetingId);
            given(meeting.getMeetingSeries()).willReturn(meetingSeries);
            given(meeting.getTitle()).willReturn("테스트 회의");
            given(meeting.getStartTime()).willReturn(startTime);
            given(meeting.getDurationMinutes()).willReturn(60);
            given(meeting.getColor()).willReturn(MeetingColor.YELLOW);
            given(meeting.isIndividuallyModified()).willReturn(false);
            given(meeting.getLocation()).willReturn("테스트 장소");
            return meeting;
        }

        private MeetingSeries createMockMeetingSeries(Long seriesId) {
            MeetingSeries meetingSeries = mock(MeetingSeries.class);
            given(meetingSeries.getId()).willReturn(seriesId);
            given(meetingSeries.getMeetingType()).willReturn(
                    com.yamoyo.be.domain.meeting.entity.enums.MeetingType.ADDITIONAL_ONE_TIME);
            return meetingSeries;
        }

        @ParameterizedTest(name = "year={0}, month={1}")
        @CsvSource(value = {
                "null, null",
                "2026, null",
                "null, 3"
        }, nullValues = "null")
        @DisplayName("회의 목록 조회 실패 - year/month 파라미터 불완전")
        void getMeetingList_Fail_InvalidYearMonthParameter(Integer year, Integer month) {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            LocalDateTime deadline = LocalDateTime.now().plusDays(30);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, deadline);
            User user = createMockUser(userId, "테스트유저");
            TeamMember member = createMockTeamMember(1L, user);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));

            // when / then
            assertThatThrownBy(() -> meetingService.getMeetingList(teamRoomId, userId, year, month))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.MEETING_INVALID_YEAR_MONTH_PARAMETER.getMessage());

            verify(meetingRepository, never()).findByTeamRoomIdAndStartTimeBetween(any(), any(), any());
        }

        @Test
        @DisplayName("회의 목록 조회 성공 - 특정 연/월 지정")
        void getMeetingList_Success_SpecificYearMonth() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            Integer year = 2026;
            Integer month = 2;
            LocalDateTime deadline = LocalDateTime.now().plusDays(30);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, deadline);
            User user = createMockUser(userId, "테스트유저");
            TeamMember member = createMockTeamMember(1L, user);

            MeetingSeries meetingSeries = createMockMeetingSeries(1L);
            // meeting1: 14:00 시작, 60분 → endTime 15:00
            LocalDateTime meetingTime1 = LocalDateTime.of(2026, 2, 15, 14, 0);
            Meeting meeting1 = createMockMeeting(1L, meetingSeries, meetingTime1);

            // meeting2: 23:30 시작, 90분 → endTime 다음 날 01:00 (자정 넘김 케이스)
            LocalDateTime meetingTime2 = LocalDateTime.of(2026, 2, 22, 23, 30);
            Meeting meeting2 = mock(Meeting.class);
            given(meeting2.getId()).willReturn(2L);
            given(meeting2.getMeetingSeries()).willReturn(meetingSeries);
            given(meeting2.getTitle()).willReturn("테스트 회의");
            given(meeting2.getStartTime()).willReturn(meetingTime2);
            given(meeting2.getDurationMinutes()).willReturn(90);
            given(meeting2.getColor()).willReturn(MeetingColor.YELLOW);
            given(meeting2.isIndividuallyModified()).willReturn(false);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(meetingRepository.findByTeamRoomIdAndStartTimeBetween(
                    eq(teamRoomId), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(List.of(meeting1, meeting2));
            List<Object[]> countResult = new ArrayList<>();
            countResult.add(new Object[]{1L, 2L});
            countResult.add(new Object[]{2L, 4L});
            given(meetingParticipantRepository.countByMeetingIds(List.of(1L, 2L)))
                    .willReturn(countResult);

            // when
            MeetingListResponse response = meetingService.getMeetingList(teamRoomId, userId, year, month);

            // then
            assertThat(response).isNotNull();
            assertThat(response.year()).isEqualTo(2026);
            assertThat(response.month()).isEqualTo(2);
            assertThat(response.meetings()).hasSize(2);
            assertThat(response.meetings().get(0).participantCount()).isEqualTo(2);
            assertThat(response.meetings().get(1).participantCount()).isEqualTo(4);

            // endTime 검증
            // meeting1: 14:00 + 60분 = 15:00
            assertThat(response.meetings().get(0).endTime())
                    .isEqualTo(LocalDateTime.of(2026, 2, 15, 15, 0));

            // meeting2: 23:30 + 90분 = 다음 날 01:00
            assertThat(response.meetings().get(1).endTime())
                    .isEqualTo(LocalDateTime.of(2026, 2, 23, 1, 0));

            verify(meetingRepository).findByTeamRoomIdAndStartTimeBetween(
                    eq(teamRoomId),
                    eq(LocalDateTime.of(2026, 2, 1, 0, 0)),
                    eq(LocalDateTime.of(2026, 3, 1, 0, 0)));
        }

        @Test
        @DisplayName("회의 목록 조회 실패 - 팀룸 없음")
        void getMeetingList_TeamRoomNotFound() {
            // given
            Long teamRoomId = 999L;
            Long userId = 1L;

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> meetingService.getMeetingList(teamRoomId, userId, null, null))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.TEAMROOM_NOT_FOUND.getMessage());

            verify(teamRoomRepository).findById(teamRoomId);
            verify(meetingRepository, never()).findByTeamRoomIdAndStartTimeBetween(
                    any(), any(), any());
        }

        @Test
        @DisplayName("회의 목록 조회 실패 - 팀원이 아님")
        void getMeetingList_NotTeamMember_Forbidden() {
            // given
            Long teamRoomId = 1L;
            Long userId = 999L;
            LocalDateTime deadline = LocalDateTime.now().plusDays(30);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, deadline);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> meetingService.getMeetingList(teamRoomId, userId, null, null))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.NOT_TEAM_MEMBER.getMessage());

            verify(teamRoomRepository).findById(teamRoomId);
            verify(teamMemberRepository).findByTeamRoomIdAndUserId(teamRoomId, userId);
            verify(meetingRepository, never()).findByTeamRoomIdAndStartTimeBetween(
                    any(), any(), any());
        }

        @Test
        @DisplayName("회의 목록 조회 성공 - 회의 없는 경우 빈 목록 반환")
        void getMeetingList_Success_EmptyList() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            LocalDateTime deadline = LocalDateTime.now().plusDays(30);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, deadline);
            User user = createMockUser(userId, "테스트유저");
            TeamMember member = createMockTeamMember(1L, user);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(meetingRepository.findByTeamRoomIdAndStartTimeBetween(
                    eq(teamRoomId), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(Collections.emptyList());

            // when
            MeetingListResponse response = meetingService.getMeetingList(teamRoomId, userId, 2026, 6);

            // then
            assertThat(response).isNotNull();
            assertThat(response.year()).isEqualTo(2026);
            assertThat(response.month()).isEqualTo(6);
            assertThat(response.meetings()).isEmpty();

            verify(meetingParticipantRepository, never()).countByMeetingIds(any());
        }

    }

    // ==================== 회의 상세 조회 테스트 ====================

    @Nested
    @DisplayName("회의 상세 조회")
    class GetMeetingDetailTest {

        private TeamMember createMockTeamMemberWithRole(Long memberId, User user, TeamRole teamRole) {
            TeamMember member = mock(TeamMember.class);
            given(member.getId()).willReturn(memberId);
            given(member.getUser()).willReturn(user);
            given(member.getTeamRole()).willReturn(teamRole);
            return member;
        }

        private MeetingSeries createMockMeetingSeriesWithType(Long seriesId, TeamRoom teamRoom, MeetingType meetingType) {
            MeetingSeries meetingSeries = mock(MeetingSeries.class);
            given(meetingSeries.getId()).willReturn(seriesId);
            given(meetingSeries.getTeamRoom()).willReturn(teamRoom);
            given(meetingSeries.getMeetingType()).willReturn(meetingType);
            given(meetingSeries.getDayOfWeek()).willReturn(
                    com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek.MON);
            given(meetingSeries.getCreatorName()).willReturn("생성자");
            return meetingSeries;
        }

        private Meeting createMockMeetingWithSeries(Long meetingId, MeetingSeries meetingSeries, LocalDateTime startTime) {
            Meeting meeting = mock(Meeting.class);
            given(meeting.getId()).willReturn(meetingId);
            given(meeting.getMeetingSeries()).willReturn(meetingSeries);
            given(meeting.getTitle()).willReturn("테스트 회의");
            given(meeting.getDescription()).willReturn("회의 설명");
            given(meeting.getLocation()).willReturn("회의실");
            given(meeting.getStartTime()).willReturn(startTime);
            given(meeting.getDurationMinutes()).willReturn(60);
            given(meeting.getColor()).willReturn(MeetingColor.YELLOW);
            given(meeting.isIndividuallyModified()).willReturn(false);
            return meeting;
        }

        private MeetingParticipant createMockMeetingParticipant(Long id, Meeting meeting, User user) {
            MeetingParticipant participant = mock(MeetingParticipant.class);
            given(participant.getId()).willReturn(id);
            given(participant.getMeeting()).willReturn(meeting);
            given(participant.getUser()).willReturn(user);
            return participant;
        }

        @Test
        @DisplayName("회의 상세 조회 성공")
        void getMeetingDetail_Success() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_ONE_TIME);
            Meeting meeting = createMockMeetingWithSeries(meetingId, meetingSeries, startTime);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);
            MeetingParticipant participant = createMockMeetingParticipant(1L, meeting, user);

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.findByMeetingIdWithUser(meetingId))
                    .willReturn(List.of(participant));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId))
                    .willReturn(true);

            // when
            MeetingDetailResponse response = meetingService.getMeetingDetail(meetingId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.meetingId()).isEqualTo(meetingId);
            assertThat(response.title()).isEqualTo("테스트 회의");
            assertThat(response.participants()).hasSize(1);
            assertThat(response.canModify()).isTrue();

            verify(meetingRepository).findByIdWithSeriesAndTeamRoom(meetingId);
            verify(teamMemberRepository).findByTeamRoomIdAndUserId(teamRoomId, userId);
            verify(meetingParticipantRepository).findByMeetingIdWithUser(meetingId);
        }

        @Test
        @DisplayName("회의 상세 조회 실패 - 회의 없음")
        void getMeetingDetail_NotFound() {
            // given
            Long meetingId = 999L;
            Long userId = 1L;

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> meetingService.getMeetingDetail(meetingId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.MEETING_NOT_FOUND.getMessage());

            verify(meetingRepository).findByIdWithSeriesAndTeamRoom(meetingId);
            verify(teamMemberRepository, never()).findByTeamRoomIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("회의 상세 조회 실패 - 팀원 아님")
        void getMeetingDetail_NotTeamMember() {
            // given
            Long meetingId = 1L;
            Long userId = 999L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_ONE_TIME);
            Meeting meeting = createMockMeetingWithSeries(meetingId, meetingSeries, startTime);

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> meetingService.getMeetingDetail(meetingId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.NOT_TEAM_MEMBER.getMessage());

            verify(meetingRepository).findByIdWithSeriesAndTeamRoom(meetingId);
            verify(teamMemberRepository).findByTeamRoomIdAndUserId(teamRoomId, userId);
            verify(meetingParticipantRepository, never()).findByMeetingIdWithUser(any());
        }

        @Test
        @DisplayName("INITIAL_REGULAR 회의 - LEADER는 수정/삭제 가능")
        void getMeetingDetail_InitialRegular_LeaderCanEdit() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.INITIAL_REGULAR);
            Meeting meeting = createMockMeetingWithSeries(meetingId, meetingSeries, startTime);

            User user = createMockUser(userId, "팀장");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.LEADER);
            MeetingParticipant participant = createMockMeetingParticipant(1L, meeting, user);

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.findByMeetingIdWithUser(meetingId))
                    .willReturn(List.of(participant));

            // when
            MeetingDetailResponse response = meetingService.getMeetingDetail(meetingId, userId);

            // then
            assertThat(response.canModify()).isTrue();

            verify(meetingParticipantRepository, never()).existsByMeetingIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("INITIAL_REGULAR 회의 - MEMBER는 수정/삭제 불가")
        void getMeetingDetail_InitialRegular_MemberCannotEdit() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.INITIAL_REGULAR);
            Meeting meeting = createMockMeetingWithSeries(meetingId, meetingSeries, startTime);

            User user = createMockUser(userId, "일반멤버");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);
            MeetingParticipant participant = createMockMeetingParticipant(1L, meeting, user);

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.findByMeetingIdWithUser(meetingId))
                    .willReturn(List.of(participant));

            // when
            MeetingDetailResponse response = meetingService.getMeetingDetail(meetingId, userId);

            // then
            assertThat(response.canModify()).isFalse();

            verify(meetingParticipantRepository, never()).existsByMeetingIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("ADDITIONAL 회의 - 참석자는 수정/삭제 가능")
        void getMeetingDetail_AdditionalMeeting_ParticipantCanEdit() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_ONE_TIME);
            Meeting meeting = createMockMeetingWithSeries(meetingId, meetingSeries, startTime);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);
            MeetingParticipant participant = createMockMeetingParticipant(1L, meeting, user);

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.findByMeetingIdWithUser(meetingId))
                    .willReturn(List.of(participant));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId))
                    .willReturn(true);

            // when
            MeetingDetailResponse response = meetingService.getMeetingDetail(meetingId, userId);

            // then
            assertThat(response.canModify()).isTrue();

            verify(meetingParticipantRepository).existsByMeetingIdAndUserId(meetingId, userId);
        }

        @Test
        @DisplayName("ADDITIONAL 회의 - 비참석자는 수정/삭제 불가")
        void getMeetingDetail_AdditionalMeeting_NonParticipantCannotEdit() {
            // given
            Long meetingId = 1L;
            Long userId = 2L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_RECURRING);
            Meeting meeting = createMockMeetingWithSeries(meetingId, meetingSeries, startTime);

            User participantUser = createMockUser(1L, "참석자");
            User nonParticipantUser = createMockUser(userId, "비참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(2L, nonParticipantUser, TeamRole.MEMBER);
            MeetingParticipant participant = createMockMeetingParticipant(1L, meeting, participantUser);

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.findByMeetingIdWithUser(meetingId))
                    .willReturn(List.of(participant));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId))
                    .willReturn(false);

            // when
            MeetingDetailResponse response = meetingService.getMeetingDetail(meetingId, userId);

            // then
            assertThat(response.canModify()).isFalse();

            verify(meetingParticipantRepository).existsByMeetingIdAndUserId(meetingId, userId);
        }
    }

    // ==================== 회의 수정 테스트 ====================

    @Nested
    @DisplayName("회의 수정")
    class UpdateMeetingTest {

        private TeamMember createMockTeamMemberWithRole(Long memberId, User user, TeamRole teamRole) {
            TeamMember member = mock(TeamMember.class);
            given(member.getId()).willReturn(memberId);
            given(member.getUser()).willReturn(user);
            given(member.getTeamRole()).willReturn(teamRole);
            return member;
        }

        private MeetingSeries createMockMeetingSeriesWithType(Long seriesId, TeamRoom teamRoom, MeetingType meetingType) {
            MeetingSeries meetingSeries = mock(MeetingSeries.class);
            given(meetingSeries.getId()).willReturn(seriesId);
            given(meetingSeries.getTeamRoom()).willReturn(teamRoom);
            given(meetingSeries.getMeetingType()).willReturn(meetingType);
            return meetingSeries;
        }

        private Meeting createMockMeetingForUpdate(Long meetingId, MeetingSeries meetingSeries, LocalDateTime startTime,
                                                    MeetingColor color, boolean isModified) {
            Meeting meeting = mock(Meeting.class);
            given(meeting.getId()).willReturn(meetingId);
            given(meeting.getMeetingSeries()).willReturn(meetingSeries);
            given(meeting.getTitle()).willReturn("테스트 회의");
            given(meeting.getDescription()).willReturn("회의 설명");
            given(meeting.getLocation()).willReturn("회의실");
            given(meeting.getStartTime()).willReturn(startTime);
            given(meeting.getDurationMinutes()).willReturn(60);
            given(meeting.getColor()).willReturn(color);
            given(meeting.isIndividuallyModified()).willReturn(isModified);
            given(meeting.isIndividuallyModified()).willReturn(isModified);
            return meeting;
        }

        private MeetingUpdateRequest createValidUpdateRequestForSingle() {
            return new MeetingUpdateRequest(
                    "수정된 회의",
                    "수정된 설명",
                    "수정된 장소",
                    LocalDate.of(2026, 2, 20),
                    null,
                    LocalTime.of(15, 0),
                    LocalTime.of(16, 30),  // endTime: 90분 후
                    MeetingColor.YELLOW,
                    Arrays.asList(1L, 2L)
            );
        }

        private MeetingUpdateRequest createValidUpdateRequestForThisAndFuture() {
            return new MeetingUpdateRequest(
                    "수정된 회의",
                    "수정된 설명",
                    "수정된 장소",
                    null,
                    DayOfWeek.FRI,
                    LocalTime.of(15, 0),
                    LocalTime.of(16, 30),  // endTime: 90분 후
                    MeetingColor.YELLOW,
                    Arrays.asList(1L, 2L)
            );
        }

        @Test
        @DisplayName("단일 회의 수정 성공 - 날짜와 시간 변경")
        void updateMeeting_SingleScope_Success() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_ONE_TIME);
            Meeting meeting = createMockMeetingForUpdate(meetingId, meetingSeries, startTime, MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);
            User participant1 = createMockUser(1L, "참석자1");
            User participant2 = createMockUser(2L, "참석자2");
            TeamMember member1 = createMockTeamMember(1L, participant1);
            TeamMember member2 = createMockTeamMember(2L, participant2);

            MeetingUpdateRequest request = createValidUpdateRequestForSingle();

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(true);
            given(teamMemberRepository.findByTeamRoomId(teamRoomId)).willReturn(Arrays.asList(member1, member2));
            given(userRepository.findAllById(anyList())).willReturn(Arrays.asList(participant1, participant2));

            // when
            MeetingUpdateResponse response = meetingService.updateMeeting(meetingId, UpdateScope.SINGLE, request, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.scope()).isEqualTo(UpdateScope.SINGLE);
            assertThat(response.updatedMeetingCount()).isEqualTo(1);
            assertThat(response.updatedMeetingIds()).containsExactly(meetingId);
            assertThat(response.skippedMeetingIds()).isEmpty();

            verify(meeting).update(eq("수정된 회의"), eq("수정된 설명"), eq("수정된 장소"),
                    eq(LocalDateTime.of(2026, 2, 20, 15, 0)), eq(90), eq(MeetingColor.YELLOW));
            verify(meeting).markAsIndividuallyModified();
            verify(meetingParticipantRepository).deleteByMeetingId(meetingId);
            verify(meetingParticipantRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("단일 회의 수정 시 isIndividuallyModified가 true로 설정됨")
        void updateMeeting_SingleScope_SetsIndividuallyModified() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_ONE_TIME);
            Meeting meeting = createMockMeetingForUpdate(meetingId, meetingSeries, startTime, MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);
            User participant1 = createMockUser(1L, "참석자1");
            TeamMember member1 = createMockTeamMember(1L, participant1);

            MeetingUpdateRequest request = new MeetingUpdateRequest(
                    "수정된 회의", "수정된 설명", "수정된 장소",
                    LocalDate.of(2026, 2, 20), null,
                    LocalTime.of(15, 0), LocalTime.of(16, 0), MeetingColor.YELLOW,
                    List.of(1L)
            );

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(true);
            given(teamMemberRepository.findByTeamRoomId(teamRoomId)).willReturn(List.of(member1));
            given(userRepository.findAllById(anyList())).willReturn(List.of(participant1));

            // when
            meetingService.updateMeeting(meetingId, UpdateScope.SINGLE, request, userId);

            // then
            verify(meeting).markAsIndividuallyModified();
        }

        @Test
        @DisplayName("THIS_AND_FUTURE 범위 회의 수정 성공 - 요일과 시간 변경")
        void updateMeeting_ThisAndFuture_Success() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            // 2026-02-15은 일요일
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_RECURRING);
            Meeting meeting1 = createMockMeetingForUpdate(1L, meetingSeries, startTime, MeetingColor.YELLOW, false);
            Meeting meeting2 = createMockMeetingForUpdate(2L, meetingSeries, startTime.plusWeeks(1), MeetingColor.YELLOW, false);
            Meeting meeting3 = createMockMeetingForUpdate(3L, meetingSeries, startTime.plusWeeks(2), MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);
            User participant1 = createMockUser(1L, "참석자1");
            TeamMember member1 = createMockTeamMember(1L, participant1);

            MeetingUpdateRequest request = new MeetingUpdateRequest(
                    "수정된 회의", "수정된 설명", "수정된 장소",
                    null, DayOfWeek.FRI,
                    LocalTime.of(15, 0), LocalTime.of(16, 0), MeetingColor.YELLOW,
                    List.of(1L)
            );

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting1));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(true);
            given(teamMemberRepository.findByTeamRoomId(teamRoomId)).willReturn(List.of(member1));
            given(meetingRepository.findByMeetingSeriesIdAndStartTimeGreaterThanEqual(1L, startTime))
                    .willReturn(Arrays.asList(meeting1, meeting2, meeting3));
            given(userRepository.findAllById(anyList())).willReturn(List.of(participant1));

            // when
            MeetingUpdateResponse response = meetingService.updateMeeting(meetingId, UpdateScope.THIS_AND_FUTURE, request, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.scope()).isEqualTo(UpdateScope.THIS_AND_FUTURE);
            assertThat(response.updatedMeetingCount()).isEqualTo(3);
            assertThat(response.updatedMeetingIds()).containsExactly(1L, 2L, 3L);
            assertThat(response.skippedMeetingIds()).isEmpty();

            // THIS_AND_FUTURE에서는 markAsIndividuallyModified가 호출되지 않음
            verify(meeting1, never()).markAsIndividuallyModified();
            verify(meeting2, never()).markAsIndividuallyModified();
            verify(meeting3, never()).markAsIndividuallyModified();
        }

        @Test
        @DisplayName("THIS_AND_FUTURE 범위 수정 시 개별 수정된 회의는 건너뜀")
        void updateMeeting_ThisAndFuture_SkipsIndividuallyModified() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_RECURRING);
            Meeting meeting1 = createMockMeetingForUpdate(1L, meetingSeries, startTime, MeetingColor.YELLOW, false);
            Meeting meeting2 = createMockMeetingForUpdate(2L, meetingSeries, startTime.plusWeeks(1), MeetingColor.YELLOW, true); // 개별 수정됨
            Meeting meeting3 = createMockMeetingForUpdate(3L, meetingSeries, startTime.plusWeeks(2), MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);
            User participant1 = createMockUser(1L, "참석자1");
            TeamMember member1 = createMockTeamMember(1L, participant1);

            MeetingUpdateRequest request = new MeetingUpdateRequest(
                    "수정된 회의", "수정된 설명", "수정된 장소",
                    null, DayOfWeek.FRI,
                    LocalTime.of(15, 0), LocalTime.of(16, 0), MeetingColor.YELLOW,
                    List.of(1L)
            );

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting1));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(true);
            given(teamMemberRepository.findByTeamRoomId(teamRoomId)).willReturn(List.of(member1));
            given(meetingRepository.findByMeetingSeriesIdAndStartTimeGreaterThanEqual(1L, startTime))
                    .willReturn(Arrays.asList(meeting1, meeting2, meeting3));
            given(userRepository.findAllById(anyList())).willReturn(List.of(participant1));

            // when
            MeetingUpdateResponse response = meetingService.updateMeeting(meetingId, UpdateScope.THIS_AND_FUTURE, request, userId);

            // then
            assertThat(response.updatedMeetingCount()).isEqualTo(2);
            assertThat(response.updatedMeetingIds()).containsExactly(1L, 3L);
            assertThat(response.skippedMeetingIds()).containsExactly(2L);

            // THIS_AND_FUTURE에서는 markAsIndividuallyModified가 호출되지 않음
            verify(meeting1, never()).markAsIndividuallyModified();
            verify(meeting2, never()).markAsIndividuallyModified();
            verify(meeting3, never()).markAsIndividuallyModified();
        }

        @Test
        @DisplayName("INITIAL_REGULAR 회의 - LEADER만 수정 가능")
        void updateMeeting_InitialRegular_OnlyLeader() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.INITIAL_REGULAR);
            Meeting meeting = createMockMeetingForUpdate(meetingId, meetingSeries, startTime, MeetingColor.PURPLE, false);

            User user = createMockUser(userId, "일반멤버");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);

            MeetingUpdateRequest request = new MeetingUpdateRequest(
                    "수정된 회의", "수정된 설명", "수정된 장소",
                    LocalDate.of(2026, 2, 20), null,
                    LocalTime.of(15, 0), LocalTime.of(16, 0), MeetingColor.PURPLE,
                    List.of(1L)
            );

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));

            // when / then
            assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, UpdateScope.SINGLE, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.MEETING_EDIT_FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("INITIAL_REGULAR 회의 - 색상 변경 불가")
        void updateMeeting_InitialRegular_ColorChangeNotAllowed() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.INITIAL_REGULAR);
            Meeting meeting = createMockMeetingForUpdate(meetingId, meetingSeries, startTime, MeetingColor.PURPLE, false);

            User user = createMockUser(userId, "팀장");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.LEADER);

            MeetingUpdateRequest request = new MeetingUpdateRequest(
                    "수정된 회의", "수정된 설명", "수정된 장소",
                    LocalDate.of(2026, 2, 20), null,
                    LocalTime.of(15, 0), LocalTime.of(16, 0), MeetingColor.YELLOW, // 색상 변경 시도
                    List.of(1L)
            );

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));

            // when / then
            assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, UpdateScope.SINGLE, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.MEETING_COLOR_CHANGE_NOT_ALLOWED.getMessage());
        }

        @Test
        @DisplayName("ADDITIONAL 회의 - 참석자만 수정 가능")
        void updateMeeting_AdditionalMeeting_OnlyParticipant() {
            // given
            Long meetingId = 1L;
            Long userId = 2L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_ONE_TIME);
            Meeting meeting = createMockMeetingForUpdate(meetingId, meetingSeries, startTime, MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "비참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);

            MeetingUpdateRequest request = new MeetingUpdateRequest(
                    "수정된 회의", "수정된 설명", "수정된 장소",
                    LocalDate.of(2026, 2, 20), null,
                    LocalTime.of(15, 0), LocalTime.of(16, 0), MeetingColor.YELLOW,
                    List.of(1L)
            );

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(false);

            // when / then
            assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, UpdateScope.SINGLE, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.MEETING_EDIT_FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("ONE_TIME 회의는 SINGLE scope만 허용")
        void updateMeeting_OneTimeMeeting_OnlySingleScope() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_ONE_TIME);
            Meeting meeting = createMockMeetingForUpdate(meetingId, meetingSeries, startTime, MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);

            MeetingUpdateRequest request = new MeetingUpdateRequest(
                    "수정된 회의", "수정된 설명", "수정된 장소",
                    null, DayOfWeek.FRI,
                    LocalTime.of(15, 0), LocalTime.of(16, 0), MeetingColor.YELLOW,
                    List.of(1L)
            );

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(true);

            // when / then
            assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, UpdateScope.THIS_AND_FUTURE, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.MEETING_INVALID_UPDATE_SCOPE.getMessage());
        }

        @Test
        @DisplayName("회의 수정 실패 - 참석자가 팀원이 아님")
        void updateMeeting_EmptyParticipants_BadRequest() {
            // given - DTO validation에서 @Size(min=1)이 처리하므로, 팀원이 아닌 참석자 테스트로 대체
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_ONE_TIME);
            Meeting meeting = createMockMeetingForUpdate(meetingId, meetingSeries, startTime, MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);
            User participant1 = createMockUser(1L, "참석자1");
            TeamMember member1 = createMockTeamMember(1L, participant1);

            MeetingUpdateRequest request = new MeetingUpdateRequest(
                    "수정된 회의", "수정된 설명", "수정된 장소",
                    LocalDate.of(2026, 2, 20), null,
                    LocalTime.of(15, 0), LocalTime.of(16, 0), MeetingColor.YELLOW,
                    List.of(999L) // 팀원이 아닌 사용자
            );

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(true);
            given(teamMemberRepository.findByTeamRoomId(teamRoomId)).willReturn(List.of(member1));

            // when / then
            assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, UpdateScope.SINGLE, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.MEETING_PARTICIPANT_NOT_TEAM_MEMBER.getMessage());
        }

        @Test
        @DisplayName("SINGLE scope 수정 시 date가 없으면 에러")
        void updateMeeting_SingleScope_RequiresDate() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_ONE_TIME);
            Meeting meeting = createMockMeetingForUpdate(meetingId, meetingSeries, startTime, MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);
            User participant1 = createMockUser(1L, "참석자1");
            TeamMember member1 = createMockTeamMember(1L, participant1);

            MeetingUpdateRequest request = new MeetingUpdateRequest(
                    "수정된 회의", "수정된 설명", "수정된 장소",
                    null, null, // date가 없음
                    LocalTime.of(15, 0), LocalTime.of(16, 0), MeetingColor.YELLOW,
                    List.of(1L)
            );

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(true);
            given(teamMemberRepository.findByTeamRoomId(teamRoomId)).willReturn(List.of(member1));

            // when / then
            assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, UpdateScope.SINGLE, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.MEETING_SINGLE_SCOPE_REQUIRES_DATE.getMessage());
        }

        @Test
        @DisplayName("THIS_AND_FUTURE scope 수정 시 dayOfWeek가 없으면 에러")
        void updateMeeting_ThisAndFuture_RequiresDayOfWeek() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_RECURRING);
            Meeting meeting = createMockMeetingForUpdate(meetingId, meetingSeries, startTime, MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);
            User participant1 = createMockUser(1L, "참석자1");
            TeamMember member1 = createMockTeamMember(1L, participant1);

            MeetingUpdateRequest request = new MeetingUpdateRequest(
                    "수정된 회의", "수정된 설명", "수정된 장소",
                    null, null, // dayOfWeek가 없음
                    LocalTime.of(15, 0), LocalTime.of(16, 0), MeetingColor.YELLOW,
                    List.of(1L)
            );

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(true);
            given(teamMemberRepository.findByTeamRoomId(teamRoomId)).willReturn(List.of(member1));

            // when / then
            assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, UpdateScope.THIS_AND_FUTURE, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.MEETING_FUTURE_SCOPE_REQUIRES_DAY_OF_WEEK.getMessage());
        }

        @Test
        @DisplayName("THIS_AND_FUTURE scope 수정 시 요일 변경이 올바르게 적용됨")
        void updateMeeting_ThisAndFuture_MovesToCorrectDayOfWeek() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            // 2026-02-04는 수요일
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 4, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_RECURRING);
            Meeting meeting = createMockMeetingForUpdate(meetingId, meetingSeries, startTime, MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);
            User participant1 = createMockUser(1L, "참석자1");
            TeamMember member1 = createMockTeamMember(1L, participant1);

            // 금요일로 변경 요청
            MeetingUpdateRequest request = new MeetingUpdateRequest(
                    "수정된 회의", "수정된 설명", "수정된 장소",
                    null, DayOfWeek.FRI,
                    LocalTime.of(15, 0), LocalTime.of(16, 0), MeetingColor.YELLOW,
                    List.of(1L)
            );

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(true);
            given(teamMemberRepository.findByTeamRoomId(teamRoomId)).willReturn(List.of(member1));
            given(meetingRepository.findByMeetingSeriesIdAndStartTimeGreaterThanEqual(1L, startTime))
                    .willReturn(List.of(meeting));
            given(userRepository.findAllById(anyList())).willReturn(List.of(participant1));

            // when
            meetingService.updateMeeting(meetingId, UpdateScope.THIS_AND_FUTURE, request, userId);

            // then
            // 수요일(2/4)에서 금요일(2/6)로 변경됨
            verify(meeting).update(eq("수정된 회의"), eq("수정된 설명"), eq("수정된 장소"),
                    eq(LocalDateTime.of(2026, 2, 6, 15, 0)), eq(60), eq(MeetingColor.YELLOW));
        }

        @Test
        @DisplayName("자정을 넘기는 회의 시간 계산 - 22:00~02:00 = 240분")
        void updateMeeting_CalculatesDurationAcrossMidnight() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_ONE_TIME);
            Meeting meeting = createMockMeetingForUpdate(meetingId, meetingSeries, startTime, MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);
            User participant1 = createMockUser(1L, "참석자1");
            TeamMember member1 = createMockTeamMember(1L, participant1);

            // time: 22:00, endTime: 02:00 → 자정 넘김 → 240분
            MeetingUpdateRequest request = new MeetingUpdateRequest(
                    "야간 회의", "자정을 넘기는 회의", "야간 회의실",
                    LocalDate.of(2026, 2, 20), null,
                    LocalTime.of(22, 0), LocalTime.of(2, 0), MeetingColor.YELLOW,
                    List.of(1L)
            );

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(true);
            given(teamMemberRepository.findByTeamRoomId(teamRoomId)).willReturn(List.of(member1));
            given(userRepository.findAllById(anyList())).willReturn(List.of(participant1));

            // when
            meetingService.updateMeeting(meetingId, UpdateScope.SINGLE, request, userId);

            // then
            // 22:00 ~ 02:00 = 240분 (4시간)
            verify(meeting).update(eq("야간 회의"), eq("자정을 넘기는 회의"), eq("야간 회의실"),
                    eq(LocalDateTime.of(2026, 2, 20, 22, 0)), eq(240), eq(MeetingColor.YELLOW));
        }

        @Test
        @DisplayName("같은 날 회의 시간 계산 - 14:00~16:00 = 120분")
        void updateMeeting_CalculatesDurationSameDay() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_ONE_TIME);
            Meeting meeting = createMockMeetingForUpdate(meetingId, meetingSeries, startTime, MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);
            User participant1 = createMockUser(1L, "참석자1");
            TeamMember member1 = createMockTeamMember(1L, participant1);

            // time: 14:00, endTime: 16:00 → 같은 날 → 120분
            MeetingUpdateRequest request = new MeetingUpdateRequest(
                    "오후 회의", "같은 날 회의", "회의실",
                    LocalDate.of(2026, 2, 20), null,
                    LocalTime.of(14, 0), LocalTime.of(16, 0), MeetingColor.YELLOW,
                    List.of(1L)
            );

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(true);
            given(teamMemberRepository.findByTeamRoomId(teamRoomId)).willReturn(List.of(member1));
            given(userRepository.findAllById(anyList())).willReturn(List.of(participant1));

            // when
            meetingService.updateMeeting(meetingId, UpdateScope.SINGLE, request, userId);

            // then
            // 14:00 ~ 16:00 = 120분 (2시간)
            verify(meeting).update(eq("오후 회의"), eq("같은 날 회의"), eq("회의실"),
                    eq(LocalDateTime.of(2026, 2, 20, 14, 0)), eq(120), eq(MeetingColor.YELLOW));
        }
    }

    // ==================== 회의 삭제 테스트 ====================

    @Nested
    @DisplayName("회의 삭제")
    class DeleteMeetingTest {

        private TeamMember createMockTeamMemberWithRole(Long memberId, User user, TeamRole teamRole) {
            TeamMember member = mock(TeamMember.class);
            given(member.getId()).willReturn(memberId);
            given(member.getUser()).willReturn(user);
            given(member.getTeamRole()).willReturn(teamRole);
            return member;
        }

        private MeetingSeries createMockMeetingSeriesWithType(Long seriesId, TeamRoom teamRoom, MeetingType meetingType) {
            MeetingSeries meetingSeries = mock(MeetingSeries.class);
            given(meetingSeries.getId()).willReturn(seriesId);
            given(meetingSeries.getTeamRoom()).willReturn(teamRoom);
            given(meetingSeries.getMeetingType()).willReturn(meetingType);
            return meetingSeries;
        }

        private Meeting createMockMeetingForDelete(Long meetingId, MeetingSeries meetingSeries, LocalDateTime startTime,
                                                    MeetingColor color, boolean isModified) {
            Meeting meeting = mock(Meeting.class);
            given(meeting.getId()).willReturn(meetingId);
            given(meeting.getMeetingSeries()).willReturn(meetingSeries);
            given(meeting.getTitle()).willReturn("테스트 회의");
            given(meeting.getDescription()).willReturn("회의 설명");
            given(meeting.getLocation()).willReturn("회의실");
            given(meeting.getStartTime()).willReturn(startTime);
            given(meeting.getDurationMinutes()).willReturn(60);
            given(meeting.getColor()).willReturn(color);
            given(meeting.isIndividuallyModified()).willReturn(isModified);
            given(meeting.isIndividuallyModified()).willReturn(isModified);
            return meeting;
        }

        @Test
        @DisplayName("단일 회의 삭제 성공")
        void deleteMeeting_SingleScope_Success() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_ONE_TIME);
            Meeting meeting = createMockMeetingForDelete(meetingId, meetingSeries, startTime, MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(true);
            given(meetingRepository.countByMeetingSeriesId(1L)).willReturn(0L);

            // when
            MeetingDeleteResponse response = meetingService.deleteMeeting(meetingId, UpdateScope.SINGLE, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.scope()).isEqualTo(UpdateScope.SINGLE);
            assertThat(response.deletedMeetingCount()).isEqualTo(1);
            assertThat(response.deletedMeetingIds()).containsExactly(meetingId);
            assertThat(response.seriesDeleted()).isTrue();

            verify(meetingParticipantRepository).deleteByMeetingId(meetingId);
            verify(meetingRepository).delete(meeting);
            verify(meetingSeriesRepository).delete(meetingSeries);
        }

        @Test
        @DisplayName("THIS_AND_FUTURE 범위 회의 삭제 성공")
        void deleteMeeting_ThisAndFuture_Success() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_RECURRING);
            Meeting meeting1 = createMockMeetingForDelete(1L, meetingSeries, startTime, MeetingColor.YELLOW, false);
            Meeting meeting2 = createMockMeetingForDelete(2L, meetingSeries, startTime.plusWeeks(1), MeetingColor.YELLOW, false);
            Meeting meeting3 = createMockMeetingForDelete(3L, meetingSeries, startTime.plusWeeks(2), MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting1));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(true);
            given(meetingRepository.findByMeetingSeriesIdAndStartTimeGreaterThanEqual(1L, startTime))
                    .willReturn(Arrays.asList(meeting1, meeting2, meeting3));
            given(meetingRepository.countByMeetingSeriesId(1L)).willReturn(0L);

            // when
            MeetingDeleteResponse response = meetingService.deleteMeeting(meetingId, UpdateScope.THIS_AND_FUTURE, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.scope()).isEqualTo(UpdateScope.THIS_AND_FUTURE);
            assertThat(response.deletedMeetingCount()).isEqualTo(3);
            assertThat(response.deletedMeetingIds()).containsExactly(1L, 2L, 3L);
            assertThat(response.seriesDeleted()).isTrue();

            verify(meetingParticipantRepository).deleteByMeetingIdIn(Arrays.asList(1L, 2L, 3L));
            verify(meetingRepository).deleteAll(Arrays.asList(meeting1, meeting2, meeting3));
            verify(meetingSeriesRepository).delete(meetingSeries);
        }

        @Test
        @DisplayName("THIS_AND_FUTURE 범위 삭제 시 개별 수정된 회의도 포함하여 삭제")
        void deleteMeeting_ThisAndFuture_IncludesIndividuallyModified() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_RECURRING);
            Meeting meeting1 = createMockMeetingForDelete(1L, meetingSeries, startTime, MeetingColor.YELLOW, false);
            Meeting meeting2 = createMockMeetingForDelete(2L, meetingSeries, startTime.plusWeeks(1), MeetingColor.YELLOW, true); // 개별 수정됨
            Meeting meeting3 = createMockMeetingForDelete(3L, meetingSeries, startTime.plusWeeks(2), MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting1));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(true);
            given(meetingRepository.findByMeetingSeriesIdAndStartTimeGreaterThanEqual(1L, startTime))
                    .willReturn(Arrays.asList(meeting1, meeting2, meeting3));
            given(meetingRepository.countByMeetingSeriesId(1L)).willReturn(0L);

            // when
            MeetingDeleteResponse response = meetingService.deleteMeeting(meetingId, UpdateScope.THIS_AND_FUTURE, userId);

            // then
            assertThat(response.deletedMeetingCount()).isEqualTo(3);
            assertThat(response.deletedMeetingIds()).containsExactly(1L, 2L, 3L); // 개별 수정된 회의도 포함

            verify(meetingParticipantRepository).deleteByMeetingIdIn(Arrays.asList(1L, 2L, 3L));
            verify(meetingRepository).deleteAll(Arrays.asList(meeting1, meeting2, meeting3));
        }

        @Test
        @DisplayName("시리즈에 회의가 남아있으면 시리즈는 삭제되지 않음")
        void deleteMeeting_SeriesNotDeleted_WhenMeetingsRemain() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_RECURRING);
            Meeting meeting = createMockMeetingForDelete(meetingId, meetingSeries, startTime, MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(true);
            given(meetingRepository.countByMeetingSeriesId(1L)).willReturn(2L); // 시리즈에 회의가 남아있음

            // when
            MeetingDeleteResponse response = meetingService.deleteMeeting(meetingId, UpdateScope.SINGLE, userId);

            // then
            assertThat(response.seriesDeleted()).isFalse();

            verify(meetingSeriesRepository, never()).delete(any());
        }

        @Test
        @DisplayName("시리즈에 회의가 0개 남으면 시리즈도 삭제됨")
        void deleteMeeting_SeriesDeleted_WhenNoMeetingsLeft() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_ONE_TIME);
            Meeting meeting = createMockMeetingForDelete(meetingId, meetingSeries, startTime, MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(true);
            given(meetingRepository.countByMeetingSeriesId(1L)).willReturn(0L);

            // when
            MeetingDeleteResponse response = meetingService.deleteMeeting(meetingId, UpdateScope.SINGLE, userId);

            // then
            assertThat(response.seriesDeleted()).isTrue();

            verify(meetingSeriesRepository).delete(meetingSeries);
        }

        @Test
        @DisplayName("INITIAL_REGULAR 회의 - LEADER만 삭제 가능")
        void deleteMeeting_InitialRegular_OnlyLeader() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.INITIAL_REGULAR);
            Meeting meeting = createMockMeetingForDelete(meetingId, meetingSeries, startTime, MeetingColor.PURPLE, false);

            User user = createMockUser(userId, "일반멤버");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));

            // when / then
            assertThatThrownBy(() -> meetingService.deleteMeeting(meetingId, UpdateScope.SINGLE, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.MEETING_DELETE_FORBIDDEN.getMessage());

            verify(meetingRepository, never()).delete(any());
        }

        @Test
        @DisplayName("ADDITIONAL 회의 - 참석자만 삭제 가능")
        void deleteMeeting_AdditionalMeeting_OnlyParticipant() {
            // given
            Long meetingId = 1L;
            Long userId = 2L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_ONE_TIME);
            Meeting meeting = createMockMeetingForDelete(meetingId, meetingSeries, startTime, MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "비참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(false);

            // when / then
            assertThatThrownBy(() -> meetingService.deleteMeeting(meetingId, UpdateScope.SINGLE, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.MEETING_DELETE_FORBIDDEN.getMessage());

            verify(meetingRepository, never()).delete(any());
        }

        @Test
        @DisplayName("ONE_TIME 회의는 SINGLE scope만 허용")
        void deleteMeeting_OneTimeMeeting_OnlySingleScope() {
            // given
            Long meetingId = 1L;
            Long userId = 1L;
            Long teamRoomId = 1L;
            LocalDateTime startTime = LocalDateTime.of(2026, 2, 15, 14, 0);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, LocalDateTime.now().plusDays(30));
            MeetingSeries meetingSeries = createMockMeetingSeriesWithType(1L, teamRoom, MeetingType.ADDITIONAL_ONE_TIME);
            Meeting meeting = createMockMeetingForDelete(meetingId, meetingSeries, startTime, MeetingColor.YELLOW, false);

            User user = createMockUser(userId, "참석자");
            TeamMember teamMember = createMockTeamMemberWithRole(1L, user, TeamRole.MEMBER);

            given(meetingRepository.findByIdWithSeriesAndTeamRoom(meetingId)).willReturn(Optional.of(meeting));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)).willReturn(true);

            // when / then
            assertThatThrownBy(() -> meetingService.deleteMeeting(meetingId, UpdateScope.THIS_AND_FUTURE, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.MEETING_INVALID_DELETE_SCOPE.getMessage());

            verify(meetingRepository, never()).delete(any());
            verify(meetingRepository, never()).deleteAll(any());
        }
    }
}
