package com.yamoyo.be.event.listener;

import com.yamoyo.be.domain.leadergame.service.GameStateRedisService;
import com.yamoyo.be.domain.leadergame.service.LeaderGameService;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WebSocketEventListener 테스트")
class WebSocketEventListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameStateRedisService gameStateRedisService;

    @Mock
    private LeaderGameService leaderGameService;

    @InjectMocks
    private WebSocketEventListener webSocketEventListener;

    private User createMockUser(Long userId, String name, Long profileImageId) {
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);
        given(user.getName()).willReturn(name);
        given(user.getProfileImageId()).willReturn(profileImageId);
        return user;
    }

    @Nested
    @DisplayName("parseRoomId - 방 ID 파싱")
    class ParseRoomIdTest {

        @Test
        @DisplayName("정상적인 destination에서 roomId 파싱 성공")
        void parseRoomId_ValidDestination_Success() {
            // when
            Long roomId = webSocketEventListener.parseRoomId("/sub/room/123");

            // then
            assertThat(roomId).isEqualTo(123L);
        }

        @Test
        @DisplayName("잘못된 형식의 destination은 null 반환")
        void parseRoomId_InvalidFormat_ReturnsNull() {
            // when
            Long roomId = webSocketEventListener.parseRoomId("/sub/room/invalid");

            // then
            assertThat(roomId).isNull();
        }

        @Test
        @DisplayName("큰 숫자의 roomId도 정상 파싱")
        void parseRoomId_LargeNumber_Success() {
            // when
            Long roomId = webSocketEventListener.parseRoomId("/sub/room/9999999999");

            // then
            assertThat(roomId).isEqualTo(9999999999L);
        }

        @Test
        @DisplayName("빈 roomId는 null 반환")
        void parseRoomId_EmptyRoomId_ReturnsNull() {
            // when
            Long roomId = webSocketEventListener.parseRoomId("/sub/room/");

            // then
            assertThat(roomId).isNull();
        }

        @Test
        @DisplayName("음수 roomId는 정상 파싱")
        void parseRoomId_NegativeNumber_Success() {
            // when
            Long roomId = webSocketEventListener.parseRoomId("/sub/room/-1");

            // then
            assertThat(roomId).isEqualTo(-1L);
        }

        @Test
        @DisplayName("소수점 포함 시 null 반환")
        void parseRoomId_DecimalNumber_ReturnsNull() {
            // when
            Long roomId = webSocketEventListener.parseRoomId("/sub/room/1.5");

            // then
            assertThat(roomId).isNull();
        }
    }
}
