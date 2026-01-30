package com.yamoyo.be.domain.leadergame.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserStatusService {

    private final StringRedisTemplate redisTemplate;

    private static final String ROOM_ONLINE_KEY_PREFIX = "room:%d:online";

    public Set<Long> getOnlineUserIds(Long roomId) {
        String key = getRoomKey(roomId);
        Set<String> members = redisTemplate.opsForSet().members(key);

        if(members == null || members.isEmpty()) {
            return Collections.emptySet();
        }

        return members.stream()
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }

    /**
     * 유저를 온라인 상태로 등록합니다. (Subscribe 이벤트 시)
     */
    public void saveUserOnline(Long roomId, Long userId) {
        String key = getRoomKey(roomId);
        redisTemplate.opsForSet().add(key, userId.toString());
        log.debug("User {} marked as ONLINE in room {}", userId, roomId);
    }

    /**
     * 유저를 온라인 목록에서 제거합니다. (Disconnect/Unsubscribe 이벤트 시)
     */
    public void removeUserOffline(Long roomId, Long userId) {
        String key = getRoomKey(roomId);
        redisTemplate.opsForSet().remove(key, userId.toString());
        log.debug("User {} marked as OFFLINE in room {}", userId, roomId);
    }

    private String getRoomKey(Long roomId) {
        return String.format(ROOM_ONLINE_KEY_PREFIX, roomId);
    }
}
