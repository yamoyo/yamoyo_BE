package com.yamoyo.be.domain.teamroom.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class InviteTokenServiceTest {

    @Autowired
    private InviteTokenService inviteTokenService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @AfterEach
    void cleanup() {
        // 테스트 후 invite 관련 키 정리
        Set<String> keys = redisTemplate.keys("invite:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("초대토큰_생성시_Redis에_저장되고_TTL이_설정된다")
    void 초대토큰_생성() {
        // given
        Long teamRoomId = 1L;

        // when
        String token = inviteTokenService.createInviteToken(teamRoomId);

        // then
        assertThat(token).isNotBlank();

        String tokenKey = "invite:token:" + token;
        String roomKey = "invite:teamRoom:" + teamRoomId;

        Object savedRoomId = redisTemplate.opsForValue().get(tokenKey);
        Object savedToken = redisTemplate.opsForValue().get(roomKey);

        assertThat(savedRoomId.toString()).isEqualTo(teamRoomId.toString());
        assertThat(savedToken).isEqualTo(token);

        Long tokenTtl = redisTemplate.getExpire(tokenKey, TimeUnit.HOURS);
        Long roomTtl = redisTemplate.getExpire(roomKey, TimeUnit.HOURS);

        assertThat(tokenTtl).isNotNull();
        assertThat(roomTtl).isNotNull();
        assertThat(tokenTtl).isGreaterThan(0);
        assertThat(roomTtl).isGreaterThan(0);

    }

    @Test
    @DisplayName("같은_팀룸에_대해_새_토큰_생성시_기존_토큰은_무효화된다")
    void 토큰_재발급() {
        // given
        Long teamRoomId = 2L;

        // when
        String firstToken = inviteTokenService.createInviteToken(teamRoomId);
        String secondToken = inviteTokenService.createInviteToken(teamRoomId);

        // then
        String firstTokenKey = "invite:token:" + firstToken;
        String secondTokenKey = "invite:token:" + secondToken;
        String roomKey = "invite:teamRoom:" + teamRoomId;

        // 첫 번째 토큰은 삭제되어야 함
        Object old = redisTemplate.opsForValue().get(firstTokenKey);
        assertThat(old).isNull();

        // 두 번째 토큰은 살아있어야 함
        Object newRoomId = redisTemplate.opsForValue().get(secondTokenKey);
        Object newToken = redisTemplate.opsForValue().get(roomKey);

        assertThat(newRoomId.toString()).isEqualTo(teamRoomId.toString());
        assertThat(newToken).isEqualTo(secondToken);
    }

    @Test
    @DisplayName("토큰으로_팀룸ID를_조회할_수_있다")
    void 초대된_팀룸_조회() {
        // given
        Long teamRoomId = 3L;
        String token = inviteTokenService.createInviteToken(teamRoomId);

        // when
        Long foundTeamRoomId =
                inviteTokenService.getTeamRoomIdByToken(token);

        // then
        assertThat(foundTeamRoomId).isEqualTo(teamRoomId);
    }

}