package com.yamoyo.be.domain.leadergame.controller;

import com.yamoyo.be.domain.leadergame.dto.GameParticipant;
import com.yamoyo.be.domain.leadergame.dto.message.ReloadPayload;
import com.yamoyo.be.domain.leadergame.enums.GameType;
import com.yamoyo.be.domain.leadergame.service.LeaderGameService;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LeaderGameWebSocketController 테스트")
class LeaderGameWebSocketControllerTest {

    @Mock
    private LeaderGameService leaderGameService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LeaderGameWebSocketController controller;

    private SimpMessageHeaderAccessor createHeaderAccessor(Long userId) {
        SimpMessageHeaderAccessor headerAccessor = mock(SimpMessageHeaderAccessor.class);
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("userId", userId);
        given(headerAccessor.getSessionAttributes()).willReturn(sessionAttributes);
        return headerAccessor;
    }

    private User createMockUser(Long userId, String name) {
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);
        given(user.getName()).willReturn(name);
        given(user.getProfileImageId()).willReturn(1L);
        return user;
    }

    @Nested
    @DisplayName("reload - 새로고침/재접속")
    class ReloadTest {

        @Test
        @DisplayName("재접속 성공")
        void reload_Success() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);
            User user = createMockUser(userId, "TestUser");

            ReloadPayload mockPayload = ReloadPayload.of(
                    GameParticipant.of(userId, "TestUser", "1"),
                    List.of(GameParticipant.of(userId, "TestUser", "1")),
                    Set.of(userId),
                    Set.of(), Set.of(),
                    null, null, null,
                    null, null, null
            );

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(leaderGameService.handleReload(roomId, user)).willReturn(mockPayload);

            // when
            controller.reload(roomId, headerAccessor);

            // then
            verify(leaderGameService).handleReload(roomId, user);
            verify(messagingTemplate).convertAndSend(
                    eq("/sub/room/" + roomId + "/user/" + userId),
                    any(Object.class)
            );
        }

        @Test
        @DisplayName("재접속 실패 - 사용자 없음")
        void reload_UserNotFound_SendsError() {
            // given
            Long roomId = 1L;
            Long userId = 999L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when
            controller.reload(roomId, headerAccessor);

            // then
            verify(messagingTemplate).convertAndSend(
                    eq("/sub/room/" + roomId + "/user/" + userId),
                    any(Object.class)
            );
        }

        @Test
        @DisplayName("재접속 실패 - 서비스 예외 발생 시 에러 전송")
        void reload_ServiceException_SendsError() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);
            User user = createMockUser(userId, "TestUser");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(leaderGameService.handleReload(roomId, user))
                    .willThrow(new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

            // when
            controller.reload(roomId, headerAccessor);

            // then
            verify(messagingTemplate).convertAndSend(
                    eq("/sub/room/" + roomId + "/user/" + userId),
                    any(Object.class)
            );
        }
    }

    @Nested
    @DisplayName("volunteer - 팀장 지원")
    class VolunteerTest {

        @Test
        @DisplayName("지원 성공")
        void volunteer_Success() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);

            // when
            controller.volunteer(roomId, headerAccessor);

            // then
            verify(leaderGameService).vote(roomId, userId, true);
        }

        @Test
        @DisplayName("지원 실패 - 서비스 예외 발생 시 에러 전송")
        void volunteer_ServiceException_SendsError() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);

            doThrow(new YamoyoException(ErrorCode.ALREADY_VOLUNTEER))
                    .when(leaderGameService).vote(roomId, userId, true);

            // when
            controller.volunteer(roomId, headerAccessor);

            // then
            verify(messagingTemplate).convertAndSend(
                    eq("/sub/room/" + roomId + "/user/" + userId),
                    any(Object.class)
            );
        }
    }

    @Nested
    @DisplayName("pass - 팀장 패스")
    class PassTest {

        @Test
        @DisplayName("패스 성공")
        void pass_Success() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);

            // when
            controller.pass(roomId, headerAccessor);

            // then
            verify(leaderGameService).vote(roomId, userId, false);
        }

        @Test
        @DisplayName("패스 실패 - 서비스 예외 발생 시 에러 전송")
        void pass_ServiceException_SendsError() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);

            doThrow(new YamoyoException(ErrorCode.INVALID_GAME_PHASE))
                    .when(leaderGameService).vote(roomId, userId, false);

            // when
            controller.pass(roomId, headerAccessor);

            // then
            verify(messagingTemplate).convertAndSend(
                    eq("/sub/room/" + roomId + "/user/" + userId),
                    any(Object.class)
            );
        }
    }

    @Nested
    @DisplayName("submitTimingResult - 타이밍 게임 결과 제출")
    class SubmitTimingResultTest {

        @Test
        @DisplayName("결과 제출 성공")
        void submitTimingResult_Success() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);
            Map<String, Object> payload = Map.of("timeDifference", 1.234);

            // when
            controller.submitTimingResult(roomId, headerAccessor, payload);

            // then
            verify(leaderGameService).submitTimingResult(roomId, userId, 1.234);
        }

        @Test
        @DisplayName("정수 값도 Double로 처리")
        void submitTimingResult_IntegerValue_Success() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);
            Map<String, Object> payload = Map.of("timeDifference", 2);

            // when
            controller.submitTimingResult(roomId, headerAccessor, payload);

            // then
            verify(leaderGameService).submitTimingResult(roomId, userId, 2.0);
        }

        @Test
        @DisplayName("결과 제출 실패 - 에러 전송")
        void submitTimingResult_ServiceException_SendsError() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);
            Map<String, Object> payload = Map.of("timeDifference", 1.0);

            doThrow(new YamoyoException(ErrorCode.TIMING_ALREADY_STOPPED))
                    .when(leaderGameService).submitTimingResult(roomId, userId, 1.0);

            // when
            controller.submitTimingResult(roomId, headerAccessor, payload);

            // then
            verify(messagingTemplate).convertAndSend(
                    eq("/sub/room/" + roomId + "/user/" + userId),
                    any(Object.class)
            );
        }
    }

    @Nested
    @DisplayName("selectGame - 게임 선택")
    class SelectGameTest {

        @Test
        @DisplayName("사다리 게임 선택 성공")
        void selectGame_Ladder_Success() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);
            Map<String, Object> payload = Map.of("gameType", "LADDER");

            // when
            controller.selectGame(roomId, headerAccessor, payload);

            // then
            verify(leaderGameService).selectGame(roomId, userId, GameType.LADDER);
        }

        @Test
        @DisplayName("룰렛 게임 선택 성공")
        void selectGame_Roulette_Success() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);
            Map<String, Object> payload = Map.of("gameType", "ROULETTE");

            // when
            controller.selectGame(roomId, headerAccessor, payload);

            // then
            verify(leaderGameService).selectGame(roomId, userId, GameType.ROULETTE);
        }

        @Test
        @DisplayName("타이밍 게임 선택 성공")
        void selectGame_Timing_Success() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);
            Map<String, Object> payload = Map.of("gameType", "TIMING");

            // when
            controller.selectGame(roomId, headerAccessor, payload);

            // then
            verify(leaderGameService).selectGame(roomId, userId, GameType.TIMING);
        }

        @Test
        @DisplayName("게임 선택 실패 - 에러 전송")
        void selectGame_ServiceException_SendsError() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);
            Map<String, Object> payload = Map.of("gameType", "LADDER");

            doThrow(new YamoyoException(ErrorCode.NOT_ROOM_HOST))
                    .when(leaderGameService).selectGame(roomId, userId, GameType.LADDER);

            // when
            controller.selectGame(roomId, headerAccessor, payload);

            // then
            verify(messagingTemplate).convertAndSend(
                    eq("/sub/room/" + roomId + "/user/" + userId),
                    any(Object.class)
            );
        }
    }

    @Nested
    @DisplayName("leave - 방 퇴장")
    class LeaveTest {

        @Test
        @DisplayName("퇴장 성공")
        void leave_Success() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);

            // when
            controller.leave(roomId, headerAccessor);

            // then
            verify(leaderGameService).handleLeave(roomId, userId);
        }

        @Test
        @DisplayName("퇴장 실패해도 예외 발생하지 않음")
        void leave_ServiceException_NoError() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);

            doThrow(new RuntimeException("Unexpected error"))
                    .when(leaderGameService).handleLeave(roomId, userId);

            // when - 예외가 발생해도 catch됨
            controller.leave(roomId, headerAccessor);

            // then - 에러 메시지 전송 없음 (조용히 실패)
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("confirmResult - 결과 확인")
    class ConfirmResultTest {

        @Test
        @DisplayName("결과 확인 성공")
        void confirmResult_Success() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);

            // when
            controller.confirmResult(roomId, headerAccessor);

            // then
            verify(leaderGameService).confirmResult(roomId, userId);
        }

        @Test
        @DisplayName("결과 확인 실패해도 예외 발생하지 않음")
        void confirmResult_ServiceException_NoError() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor(userId);

            doThrow(new RuntimeException("Unexpected error"))
                    .when(leaderGameService).confirmResult(roomId, userId);

            // when - 예외가 발생해도 catch됨
            controller.confirmResult(roomId, headerAccessor);

            // then - 에러 메시지 전송 없음 (조용히 실패)
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }
}
