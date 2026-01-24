package com.yamoyo.be.domain.teamroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class InviteTokenService {

    private static final String INVITE_KEY_PREFIX = "invite:token:";
    private static final String TEAM_ROOM_KEY_PREFIX = "invite:teamRoom:";
    private static final long TOKEN_EXPIRATION_HOURS = 24;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 초대 링크 토큰 생성
     */
    public String createInviteToken(Long teamRoomId){

        log.info("초대링크 토큰 생성 시작 - TeamRoomId: {}", teamRoomId);
        // 1. 기존 토큰 무효화
        invalidateInviteToken(teamRoomId);

        // 2. 새 토큰 생성
        String token = generateToken();

        // 3. 레디스에 토큰 저장
        String tokenKey = INVITE_KEY_PREFIX + token;
        String roomKey = TEAM_ROOM_KEY_PREFIX + teamRoomId;
        redisTemplate.opsForValue().set(
                tokenKey, // Key : {invite:token:secure-random}
                teamRoomId, // Value : 1L
                TOKEN_EXPIRATION_HOURS, // 만료시간
                TimeUnit.HOURS // 시간단위
        );
        redisTemplate.opsForValue().set(
                roomKey, // Key : {invite:teamRoom:1L}
                token, // Value : {secure-random}
                TOKEN_EXPIRATION_HOURS,
                TimeUnit.HOURS
        );

        log.info("초대링크 토큰 생성 성공! - TeamRoomId: {}", teamRoomId);
        return token;
    }

    /**
     * 기존 토큰 무효화
     */
    public void invalidateInviteToken(Long teamRoomId){
        String roomKey = TEAM_ROOM_KEY_PREFIX + teamRoomId;
        String oldToken = (String) redisTemplate.opsForValue().get(roomKey);
        if(oldToken != null){
            log.info("기존 토큰 무효화 - TeamRoomId: {}", teamRoomId);
            redisTemplate.delete(INVITE_KEY_PREFIX + oldToken);
        }
    }

    /**
     * 토큰으로 팀룸 조회
     */
    public Long getTeamRoomIdByToken(String token){
        String key = INVITE_KEY_PREFIX + token;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.valueOf(value.toString()) : null;
    }

    /**
     * SecureRandom 32 bytes token 생성
     */
    private String generateToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
