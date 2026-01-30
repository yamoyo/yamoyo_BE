package com.yamoyo.be.domain.leadergame.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserStatusService 테스트")
class UserStatusServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private UserStatusService userStatusService;

    private static final String ROOM_ONLINE_KEY_PREFIX = "room:%d:online";

    private String getRoomKey(Long roomId) {
        return String.format(ROOM_ONLINE_KEY_PREFIX, roomId);
    }

    @Nested
    @DisplayName("getOnlineUserIds - 온라인 유저 ID 조회")
    class GetOnlineUserIdsTest {

        @Test
        @DisplayName("온라인 유저 ID 조회 성공")
        void getOnlineUserIds_Success() {
            // given
            Long roomId = 1L;
            String key = getRoomKey(roomId);

            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members(key)).willReturn(Set.of("100", "200", "300"));

            // when
            Set<Long> result = userStatusService.getOnlineUserIds(roomId);

            // then
            assertThat(result).hasSize(3);
            assertThat(result).containsExactlyInAnyOrder(100L, 200L, 300L);
        }

        @Test
        @DisplayName("온라인 유저가 없는 경우 빈 Set 반환")
        void getOnlineUserIds_NoUsers_ReturnsEmptySet() {
            // given
            Long roomId = 1L;
            String key = getRoomKey(roomId);

            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members(key)).willReturn(Collections.emptySet());

            // when
            Set<Long> result = userStatusService.getOnlineUserIds(roomId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Redis에서 null 반환 시 빈 Set 반환")
        void getOnlineUserIds_NullFromRedis_ReturnsEmptySet() {
            // given
            Long roomId = 1L;
            String key = getRoomKey(roomId);

            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members(key)).willReturn(null);

            // when
            Set<Long> result = userStatusService.getOnlineUserIds(roomId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("단일 유저만 온라인인 경우")
        void getOnlineUserIds_SingleUser() {
            // given
            Long roomId = 1L;
            String key = getRoomKey(roomId);

            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members(key)).willReturn(Set.of("999"));

            // when
            Set<Long> result = userStatusService.getOnlineUserIds(roomId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result).contains(999L);
        }
    }

    @Nested
    @DisplayName("saveUserOnline - 유저 온라인 상태 저장")
    class SaveUserOnlineTest {

        @Test
        @DisplayName("유저 온라인 상태 저장 성공")
        void saveUserOnline_Success() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            String key = getRoomKey(roomId);

            given(redisTemplate.opsForSet()).willReturn(setOperations);

            // when
            userStatusService.saveUserOnline(roomId, userId);

            // then
            verify(setOperations).add(key, userId.toString());
        }

        @Test
        @DisplayName("다른 방에 유저 온라인 저장")
        void saveUserOnline_DifferentRoom() {
            // given
            Long roomId = 999L;
            Long userId = 100L;
            String key = getRoomKey(roomId);

            given(redisTemplate.opsForSet()).willReturn(setOperations);

            // when
            userStatusService.saveUserOnline(roomId, userId);

            // then
            verify(setOperations).add(key, userId.toString());
        }
    }

    @Nested
    @DisplayName("removeUserOffline - 유저 오프라인 처리")
    class RemoveUserOfflineTest {

        @Test
        @DisplayName("유저 오프라인 처리 성공")
        void removeUserOffline_Success() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            String key = getRoomKey(roomId);

            given(redisTemplate.opsForSet()).willReturn(setOperations);

            // when
            userStatusService.removeUserOffline(roomId, userId);

            // then
            verify(setOperations).remove(key, userId.toString());
        }

        @Test
        @DisplayName("존재하지 않는 유저 오프라인 처리 (에러 없이 처리)")
        void removeUserOffline_NonExistentUser() {
            // given
            Long roomId = 1L;
            Long userId = 999L;
            String key = getRoomKey(roomId);

            given(redisTemplate.opsForSet()).willReturn(setOperations);

            // when
            userStatusService.removeUserOffline(roomId, userId);

            // then
            verify(setOperations).remove(key, userId.toString());
        }
    }

    @Nested
    @DisplayName("Room Key 생성")
    class RoomKeyTest {

        @Test
        @DisplayName("올바른 Room Key 형식 생성")
        void roomKey_CorrectFormat() {
            // given
            Long roomId = 123L;

            given(redisTemplate.opsForSet()).willReturn(setOperations);

            // when
            userStatusService.saveUserOnline(roomId, 1L);

            // then
            verify(setOperations).add("room:123:online", "1");
        }

        @Test
        @DisplayName("큰 Room ID로도 올바른 Key 생성")
        void roomKey_LargeRoomId() {
            // given
            Long roomId = 9999999999L;

            given(redisTemplate.opsForSet()).willReturn(setOperations);

            // when
            userStatusService.saveUserOnline(roomId, 1L);

            // then
            verify(setOperations).add("room:9999999999:online", "1");
        }
    }
}
