package com.yamoyo.be.domain.meeting.service;

import com.yamoyo.be.domain.meeting.dto.request.MeetingCreateRequest;
import com.yamoyo.be.domain.meeting.dto.response.MeetingCreateResponse;
import com.yamoyo.be.domain.meeting.dto.response.MeetingListResponse;
import com.yamoyo.be.domain.meeting.entity.Meeting;
import com.yamoyo.be.domain.meeting.entity.MeetingSeries;
import com.yamoyo.be.domain.meeting.entity.enums.MeetingColor;
import com.yamoyo.be.domain.meeting.repository.MeetingParticipantRepository;
import com.yamoyo.be.domain.meeting.repository.MeetingRepository;
import com.yamoyo.be.domain.meeting.repository.MeetingSeriesRepository;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
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

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    private MeetingCreateRequest createValidRequest(LocalDateTime startTime, boolean isRecurring) {
        return new MeetingCreateRequest(
                "스프린트 회고",
                "2주차 스프린트 회고",
                "줌 회의실",
                startTime,
                60,
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
            LocalDateTime startTime = LocalDateTime.now().plusDays(7)
                    .withHour(14).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime deadline = LocalDateTime.now().plusDays(30);

            MeetingCreateRequest request = createValidRequest(startTime, false);

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
            LocalDateTime startTime = LocalDateTime.now().plusDays(1)
                    .withHour(14).withMinute(30).withSecond(0).withNano(0);
            LocalDateTime deadline = LocalDateTime.now().plusWeeks(4); // 4주 후 deadline

            MeetingCreateRequest request = createValidRequest(startTime, true);

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
            LocalDateTime startTime = LocalDateTime.now().plusDays(7)
                    .withHour(14).withMinute(0).withSecond(0).withNano(0);
            MeetingCreateRequest request = createValidRequest(startTime, false);

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
            LocalDateTime startTime = LocalDateTime.now().plusDays(7)
                    .withHour(14).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime deadline = LocalDateTime.now().plusDays(30);
            MeetingCreateRequest request = createValidRequest(startTime, false);

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
            LocalDateTime startTime = LocalDateTime.now().plusDays(7)
                    .withHour(14).withMinute(15).withSecond(0).withNano(0); // 15분 - 잘못된 시간
            LocalDateTime deadline = LocalDateTime.now().plusDays(30);

            MeetingCreateRequest request = createValidRequest(startTime, false);

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
            LocalDateTime startTime = LocalDateTime.now().plusDays(7)
                    .withHour(14).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime deadline = LocalDateTime.now().plusDays(30);

            MeetingCreateRequest request = new MeetingCreateRequest(
                    "스프린트 회고",
                    "2주차 스프린트 회고",
                    "줌 회의실",
                    startTime,
                    45, // 45분 - 잘못된 시간
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
            LocalDateTime startTime = LocalDateTime.now().plusDays(7)
                    .withHour(14).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime deadline = LocalDateTime.now().plusDays(30);

            MeetingCreateRequest request = new MeetingCreateRequest(
                    "스프린트 회고",
                    "2주차 스프린트 회고",
                    "줌 회의실",
                    startTime,
                    60,
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
            LocalDateTime startTime = LocalDateTime.now().plusDays(7)
                    .withHour(14).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime deadline = LocalDateTime.now().plusDays(30);

            MeetingCreateRequest request = new MeetingCreateRequest(
                    "스프린트 회고",
                    "2주차 스프린트 회고",
                    "줌 회의실",
                    startTime,
                    60,
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
            given(meeting.getIsIndividuallyModified()).willReturn(false);
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
            given(meeting2.getIsIndividuallyModified()).willReturn(false);

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
}
