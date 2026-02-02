package com.yamoyo.be.event.listener;

import com.yamoyo.be.domain.fcm.entity.UserDevice;
import com.yamoyo.be.domain.fcm.repository.UserDeviceRepository;
import com.yamoyo.be.domain.fcm.service.FcmService;
import com.yamoyo.be.domain.notification.entity.Notification;
import com.yamoyo.be.domain.notification.entity.NotificationType;
import com.yamoyo.be.domain.notification.service.NotificationService;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.event.event.NotificationEvent;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * NotificationEventListener 단위 테스트
 *
 * 테스트 내용:
 * 1. handleNotificationEvent() - 알림 이벤트 처리
 *    - 알림 저장
 *    - FCM 발송
 *    - TEAM_JOIN 시 본인 제외
 *    - 알람 꺼진 사용자 FCM 제외
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener 단위 테스트")
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private FcmService fcmService;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @Mock
    private TeamRoomRepository teamRoomRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    private static final Long TEAM_ROOM_ID = 1L;
    private static final Long TARGET_ID = 100L;
    private static final String TEAM_ROOM_TITLE = "테스트팀";

    @Nested
    @DisplayName("handleNotificationEvent() - 알림 이벤트 처리")
    class HandleNotificationEventTest {

        @Test
        @DisplayName("성공: 알림 저장 및 FCM 발송")
        void handleNotificationEvent_Success() {
            // given
            NotificationEvent event = new NotificationEvent(
                    TEAM_ROOM_ID, TARGET_ID, NotificationType.MEETING_REMIND, null, null
            );

            TeamRoom teamRoom = createTeamRoom();
            User user1 = createUser(1L, "user1@test.com", true);
            User user2 = createUser(2L, "user2@test.com", true);
            TeamMember member1 = createTeamMember(user1);
            TeamMember member2 = createTeamMember(user2);
            UserDevice device1 = createUserDevice(user1, "token1");
            UserDevice device2 = createUserDevice(user2, "token2");

            given(teamRoomRepository.findById(TEAM_ROOM_ID)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomId(TEAM_ROOM_ID)).willReturn(List.of(member1, member2));
            given(userDeviceRepository.findAllByUserIdIn(anyList())).willReturn(List.of(device1, device2));

            // when
            notificationEventListener.handleNotificationEvent(event);

            // then
            ArgumentCaptor<List<Notification>> notificationCaptor = ArgumentCaptor.forClass(List.class);
            then(notificationService).should().saveAll(notificationCaptor.capture());
            assertThat(notificationCaptor.getValue()).hasSize(2);

            ArgumentCaptor<List<String>> tokenCaptor = ArgumentCaptor.forClass(List.class);
            then(fcmService).should().sendMessage(tokenCaptor.capture(), anyString(), anyString(), any(Map.class));
            assertThat(tokenCaptor.getValue()).containsExactlyInAnyOrder("token1", "token2");
        }

        @Test
        @DisplayName("성공: TEAM_JOIN 이벤트 시 본인 제외")
        void handleNotificationEvent_TeamJoin_ExcludeSelf() {
            // given
            Long joinedUserId = 1L;
            NotificationEvent event = new NotificationEvent(
                    TEAM_ROOM_ID, joinedUserId, NotificationType.TEAM_JOIN, null, null
            );

            TeamRoom teamRoom = createTeamRoom();
            User joinedUser = createUser(joinedUserId, "joined@test.com", true);
            User otherUser = createUser(2L, "other@test.com", true);
            TeamMember member1 = createTeamMember(joinedUser);
            TeamMember member2 = createTeamMember(otherUser);
            UserDevice device2 = createUserDevice(otherUser, "token2");

            given(teamRoomRepository.findById(TEAM_ROOM_ID)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomId(TEAM_ROOM_ID)).willReturn(List.of(member1, member2));
            given(userDeviceRepository.findAllByUserIdIn(anyList())).willReturn(List.of(device2));

            // when
            notificationEventListener.handleNotificationEvent(event);

            // then
            ArgumentCaptor<List<Notification>> notificationCaptor = ArgumentCaptor.forClass(List.class);
            then(notificationService).should().saveAll(notificationCaptor.capture());
            // 본인 제외하고 1명만 알림 저장
            assertThat(notificationCaptor.getValue()).hasSize(1);
        }

        @Test
        @DisplayName("성공: 알람 꺼진 사용자는 FCM 발송 제외")
        void handleNotificationEvent_AlarmOff_NoFcm() {
            // given
            NotificationEvent event = new NotificationEvent(
                    TEAM_ROOM_ID, TARGET_ID, NotificationType.MEETING_REMIND, null, null
            );

            TeamRoom teamRoom = createTeamRoom();
            User userAlarmOn = createUser(1L, "alarmon@test.com", true);
            User userAlarmOff = createUser(2L, "alarmoff@test.com", false);
            TeamMember member1 = createTeamMember(userAlarmOn);
            TeamMember member2 = createTeamMember(userAlarmOff);
            UserDevice device1 = createUserDevice(userAlarmOn, "token1");
            UserDevice device2 = createUserDevice(userAlarmOff, "token2");

            given(teamRoomRepository.findById(TEAM_ROOM_ID)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomId(TEAM_ROOM_ID)).willReturn(List.of(member1, member2));
            given(userDeviceRepository.findAllByUserIdIn(anyList())).willReturn(List.of(device1, device2));

            // when
            notificationEventListener.handleNotificationEvent(event);

            // then
            // 알림은 2명 모두에게 저장
            ArgumentCaptor<List<Notification>> notificationCaptor = ArgumentCaptor.forClass(List.class);
            then(notificationService).should().saveAll(notificationCaptor.capture());
            assertThat(notificationCaptor.getValue()).hasSize(2);

            // FCM은 알람 켜진 사용자만
            ArgumentCaptor<List<String>> tokenCaptor = ArgumentCaptor.forClass(List.class);
            then(fcmService).should().sendMessage(tokenCaptor.capture(), anyString(), anyString(), any(Map.class));
            assertThat(tokenCaptor.getValue()).containsExactly("token1");
        }

        @Test
        @DisplayName("성공: 팀원이 없는 경우 아무 작업 안 함")
        void handleNotificationEvent_NoMembers() {
            // given
            NotificationEvent event = new NotificationEvent(
                    TEAM_ROOM_ID, TARGET_ID, NotificationType.MEETING_REMIND, null, null
            );

            // receivers가 비어있으면 getTitle() 호출 전에 return하므로 getId()만 stubbing
            TeamRoom teamRoom = mock(TeamRoom.class);

            given(teamRoomRepository.findById(TEAM_ROOM_ID)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomId(TEAM_ROOM_ID)).willReturn(Collections.emptyList());

            // when
            notificationEventListener.handleNotificationEvent(event);

            // then
            then(notificationService).should(never()).saveAll(anyList());
            then(fcmService).should(never()).sendMessage(anyList(), anyString(), anyString(), any(Map.class));
        }

        @Test
        @DisplayName("성공: 등록된 기기가 없는 경우 FCM 발송 안 함")
        void handleNotificationEvent_NoDevices() {
            // given
            NotificationEvent event = new NotificationEvent(
                    TEAM_ROOM_ID, TARGET_ID, NotificationType.MEETING_REMIND, null, null
            );

            TeamRoom teamRoom = createTeamRoom();
            User user = createUser(1L, "user@test.com", true);
            TeamMember member = createTeamMember(user);

            given(teamRoomRepository.findById(TEAM_ROOM_ID)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomId(TEAM_ROOM_ID)).willReturn(List.of(member));
            given(userDeviceRepository.findAllByUserIdIn(anyList())).willReturn(Collections.emptyList());

            // when
            notificationEventListener.handleNotificationEvent(event);

            // then
            then(notificationService).should().saveAll(anyList());
            then(fcmService).should(never()).sendMessage(anyList(), anyString(), anyString(), any(Map.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 팀룸")
        void handleNotificationEvent_TeamRoomNotFound() {
            // given
            NotificationEvent event = new NotificationEvent(
                    TEAM_ROOM_ID, TARGET_ID, NotificationType.MEETING_REMIND, null, null
            );

            given(teamRoomRepository.findById(TEAM_ROOM_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationEventListener.handleNotificationEvent(event))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TEAMROOM_NOT_FOUND));
        }

        @Test
        @DisplayName("성공: 알림 저장 실패해도 FCM 발송은 진행")
        void handleNotificationEvent_SaveFails_FcmContinues() {
            // given
            NotificationEvent event = new NotificationEvent(
                    TEAM_ROOM_ID, TARGET_ID, NotificationType.MEETING_REMIND, null, null
            );

            TeamRoom teamRoom = createTeamRoom();
            User user = createUser(1L, "user@test.com", true);
            TeamMember member = createTeamMember(user);
            UserDevice device = createUserDevice(user, "token1");

            given(teamRoomRepository.findById(TEAM_ROOM_ID)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomId(TEAM_ROOM_ID)).willReturn(List.of(member));
            given(userDeviceRepository.findAllByUserIdIn(anyList())).willReturn(List.of(device));
            willThrow(new RuntimeException("DB 오류")).given(notificationService).saveAll(anyList());

            // when - 예외가 발생하지 않음 (catch로 처리됨)
            notificationEventListener.handleNotificationEvent(event);

            // then - FCM 발송은 진행됨
            then(fcmService).should().sendMessage(anyList(), anyString(), anyString(), any(Map.class));
        }

        @Test
        @DisplayName("성공: FCM data에 type, teamRoomId, targetId 포함")
        void handleNotificationEvent_FcmDataContainsCorrectFields() {
            // given
            NotificationEvent event = new NotificationEvent(
                    TEAM_ROOM_ID, TARGET_ID, NotificationType.RULE_CHANGE, null, null
            );

            TeamRoom teamRoom = createTeamRoom();
            User user = createUser(1L, "user@test.com", true);
            TeamMember member = createTeamMember(user);
            UserDevice device = createUserDevice(user, "token1");

            given(teamRoomRepository.findById(TEAM_ROOM_ID)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomId(TEAM_ROOM_ID)).willReturn(List.of(member));
            given(userDeviceRepository.findAllByUserIdIn(anyList())).willReturn(List.of(device));

            // when
            notificationEventListener.handleNotificationEvent(event);

            // then
            ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
            then(fcmService).should().sendMessage(anyList(), anyString(), anyString(), dataCaptor.capture());

            Map<String, String> capturedData = dataCaptor.getValue();
            assertThat(capturedData.get("type")).isEqualTo("RULE_CHANGE");
            assertThat(capturedData.get("teamRoomId")).isEqualTo(String.valueOf(TEAM_ROOM_ID));
            assertThat(capturedData.get("targetId")).isEqualTo(String.valueOf(TARGET_ID));
        }

        @Test
        @DisplayName("성공: targetId가 null인 경우 FCM data에 targetId 미포함")
        void handleNotificationEvent_NullTargetId_NoTargetIdInData() {
            // given
            NotificationEvent event = new NotificationEvent(
                    TEAM_ROOM_ID, null, NotificationType.TEAM_ARCHIVED, null, null
            );

            TeamRoom teamRoom = createTeamRoom();
            User user = createUser(1L, "user@test.com", true);
            TeamMember member = createTeamMember(user);
            UserDevice device = createUserDevice(user, "token1");

            given(teamRoomRepository.findById(TEAM_ROOM_ID)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomId(TEAM_ROOM_ID)).willReturn(List.of(member));
            given(userDeviceRepository.findAllByUserIdIn(anyList())).willReturn(List.of(device));

            // when
            notificationEventListener.handleNotificationEvent(event);

            // then
            ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
            then(fcmService).should().sendMessage(anyList(), anyString(), anyString(), dataCaptor.capture());

            Map<String, String> capturedData = dataCaptor.getValue();
            assertThat(capturedData).doesNotContainKey("targetId");
        }
    }

    // ========== Helper Methods ==========

    private TeamRoom createTeamRoom() {
        TeamRoom teamRoom = mock(TeamRoom.class);
        // 리스너에서 teamRoom.getTitle()만 호출됨
        given(teamRoom.getTitle()).willReturn(TEAM_ROOM_TITLE);
        return teamRoom;
    }

    private User createUser(Long id, String email, boolean alarmOn) {
        User user = User.create(email, "테스트" + id);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "isAlarmOn", alarmOn);
        return user;
    }

    private TeamMember createTeamMember(User user) {
        TeamMember teamMember = mock(TeamMember.class);
        given(teamMember.getUser()).willReturn(user);
        return teamMember;
    }

    private UserDevice createUserDevice(User user, String fcmToken) {
        UserDevice userDevice = UserDevice.create(user, fcmToken, "ANDROID", "Test Device");
        return userDevice;
    }
}
