package com.yamoyo.be.domain.leadergame.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yamoyo.be.domain.leadergame.dto.GameParticipant;
import com.yamoyo.be.domain.leadergame.enums.GamePhase;
import com.yamoyo.be.domain.leadergame.enums.GameType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 팀장 선출 게임 상태를 Redis에서 관리하는 서비스
 *
 * <h2>Redis 데이터 구조</h2>
 * <pre>
 * 1. 게임 상태 (Hash)
 *    Key: room:{roomId}:game
 *    Fields:
 *      - phase: 현재 게임 단계 (VOLUNTEER, GAME_SELECT, GAME_PLAYING, RESULT)
 *      - phaseStartTime: 현재 단계 시작 시간 (timestamp)
 *      - participants: 참가자 목록 (JSON)
 *      - volunteers: 팀장 지원자 ID 목록 (JSON)
 *      - selectedGame: 선택된 게임 타입 (LADDER, ROULETTE, TIMING)
 *      - gameStartTime: 게임(타이밍) 시작 시간 (timestamp)
 *      - timingRecords: 타이밍 게임 기록 {userId: stoppedTime} (JSON)
 *      - winnerId: 당첨자(팀장) ID
 *      - gameResult: 게임 결과 데이터 (JSON)
 *
 * 2. 접속자 관리 (Set)
 *    Key: room:{roomId}:connections
 *    Members: 접속 중인 userId 목록
 * </pre>
 *
 * <h2>TTL</h2>
 * 모든 키는 30분 후 자동 만료됨 (게임 비정상 종료 시 정리용)
 *
 * <h2>게임 흐름별 사용 메서드</h2>
 * <pre>
 * 1. 입장 시
 *    → addConnection(roomId, userId)
 *    → getParticipants(roomId), getPhase(roomId), getConnectedUsers(roomId)
 *
 * 2. 게임 시작 시 (방장)
 *    → areAllMembersConnected(roomId, memberIds) // 전원 접속 확인
 *    → initializeGame(roomId, participants)      // 게임 초기화
 *
 * 3. 지원 단계
 *    → addVolunteer(roomId, userId)
 *    → getVolunteers(roomId)
 *
 * 4. 게임 선택 (방장)
 *    → setSelectedGame(roomId, gameType)
 *    → setPhase(roomId, GamePhase.GAME_PLAYING) // 타이밍 게임인 경우
 *
 * 5. 타이밍 게임
 *    → setGameStartTime(roomId)               // 게임 시작 시간 기록
 *    → recordTimingStop(roomId, userId, time) // 각 유저 기록
 *    → getTimingRecords(roomId)               // 결과 계산용
 *
 * 6. 결과
 *    → setWinner(roomId, winnerId)
 *    → setPhase(roomId, GamePhase.RESULT)
 *
 * 7. 퇴장 시
 *    → removeConnection(roomId, userId)
 *    → getConnectedUsers(roomId).isEmpty() 이면 deleteGame(roomId)
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameStateRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GAME_KEY_PREFIX = "room:%d:game";
    private static final String CONNECTION_KEY_PREFIX = "room:%d:connections";
    private static final long GAME_TTL_MINUTES = 30;

    private String getGameKey(Long roomId) {
        return String.format(GAME_KEY_PREFIX, roomId);
    }

    private String getConnectionKey(Long roomId) {
        return String.format(CONNECTION_KEY_PREFIX, roomId);
    }

    // ========================================================================
    // 게임 초기화 / 삭제
    // ========================================================================

    /**
     * 게임을 초기화하고 VOLUNTEER 단계로 시작
     *
     * 사용 시점: 방장이 "게임 시작" 버튼을 눌렀을 때
     * 저장 데이터:
     *   - phase: VOLUNTEER
     *   - phaseStartTime: 현재 시간
     *   - participants: 참가자 목록
     *   - volunteers: 빈 배열
     * TTL: 30분
     *
     * @param roomId 팀룸 ID
     * @param participants 게임 참가자 목록 (DB에서 조회한 TeamMember 기반)
     */
    public void initializeGame(Long roomId, List<GameParticipant> participants) {
        String key = getGameKey(roomId);
        Map<String, Object> gameState = new HashMap<>();
        gameState.put("phase", GamePhase.VOLUNTEER.name());
        gameState.put("phaseStartTime", System.currentTimeMillis());
        gameState.put("volunteers", "[]");
        try {
            gameState.put("participants", objectMapper.writeValueAsString(participants));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize participants", e);
            gameState.put("participants", "[]");
        }
        redisTemplate.opsForHash().putAll(key, gameState);
        redisTemplate.expire(key, GAME_TTL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 게임이 존재하는지 확인
     *
     * <pre>
     * 사용 시점: 게임 시작 전 중복 실행 방지
     * 예시:
     *   if (isGameExists(roomId)) {
     *       throw new YamoyoException(ErrorCode.GAME_ALREADY_IN_PROGRESS);
     *   }
     * </pre>
     */
    public boolean isGameExists(Long roomId) {
        String key = getGameKey(roomId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 게임 데이터 전체 삭제 (게임 상태 + 접속자 정보)
     *
     * 사용 시점: 모든 참가자가 결과 확인 후 퇴장했을 때
     * 예시:
     *   removeConnection(roomId, userId);
     *   if (getConnectedUsers(roomId).isEmpty()) {
     *       deleteGame(roomId);
     *   }
     */
    public void deleteGame(Long roomId) {
        redisTemplate.delete(getGameKey(roomId));
        redisTemplate.delete(getConnectionKey(roomId));
    }

    // ========================================================================
    // Phase (게임 단계) 관리
    // ========================================================================

    /**
     * 게임 단계 변경
     *
     * Phase 흐름:
     *   VOLUNTEER → GAME_SELECT → GAME_PLAYING(타이밍만) → RESULT
     *   (사다리/룰렛은 GAME_SELECT에서 바로 RESULT로)
     *
     * 사용 예시:
     *   setPhase(roomId, GamePhase.GAME_SELECT);  // 지원 마감 후
     *   setPhase(roomId, GamePhase.GAME_PLAYING); // 타이밍 게임 선택 시
     *   setPhase(roomId, GamePhase.RESULT);       // 게임 종료
     */
    public void setPhase(Long roomId, GamePhase phase) {
        String key = getGameKey(roomId);
        redisTemplate.opsForHash().put(key, "phase", phase.name());
        redisTemplate.opsForHash().put(key, "phaseStartTime", System.currentTimeMillis());
    }

    /**
     * 현재 게임 단계 조회
     *
     * 사용 시점: 각 액션 전 현재 단계 검증
     * 예시:
     *   if (getPhase(roomId) != GamePhase.VOLUNTEER) {
     *       throw new YamoyoException(ErrorCode.INVALID_GAME_PHASE);
     *   }
     *
     * @return 현재 Phase, 게임이 없으면 null
     */
    public GamePhase getPhase(Long roomId) {
        String key = getGameKey(roomId);
        Object phase = redisTemplate.opsForHash().get(key, "phase");
        if (phase == null) return null;
        return GamePhase.valueOf(phase.toString());
    }

    /**
     * 현재 단계 시작 시간 조회
     *
     * 사용 시점: 남은 시간 계산 (재입장 시 동기화)
     */
    public Long getPhaseStartTime(Long roomId) {
        String key = getGameKey(roomId);
        Object time = redisTemplate.opsForHash().get(key, "phaseStartTime");
        if (time == null) return null;
        return Long.parseLong(time.toString());
    }

    // ========================================================================
    // 참가자 / 지원자 관리
    // ========================================================================

    /**
     * 게임 참가자 목록 조회
     *
     * 저장 시점: initializeGame() 호출 시 DB에서 조회하여 저장
     * 사용 시점:
     *   - 입장 시 현재 참가자 목록 전송
     *   - 게임 결과 계산 시 후보자 필터링
     */
    public List<GameParticipant> getParticipants(Long roomId) {
        String key = getGameKey(roomId);
        Object participantsJson = redisTemplate.opsForHash().get(key, "participants");
        if (participantsJson == null) return new ArrayList<>();
        try {
            return objectMapper.readValue(participantsJson.toString(), new TypeReference<List<GameParticipant>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize participants", e);
            return new ArrayList<>();
        }
    }

    /**
     * 팀장 지원자 추가
     *
     * <pre>
     * 사용 시점: VOLUNTEER 단계에서 유저가 "팀장 지원" 클릭
     * 예시:
     *   if (getPhase(roomId) == GamePhase.VOLUNTEER) {
     *       addVolunteer(roomId, userId);
     *       broadcast("VOLUNTEER", getVolunteers(roomId));
     *   }
     * </pre>
     */
    public void addVolunteer(Long roomId, Long userId) {
        String key = getGameKey(roomId);
        Set<Long> volunteers = getVolunteers(roomId);
        volunteers.add(userId);
        try {
            redisTemplate.opsForHash().put(key, "volunteers", objectMapper.writeValueAsString(volunteers));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize volunteers", e);
        }
    }

    /**
     * 팀장 지원자 목록 조회
     *
     * <pre>
     * 사용 시점:
     *   - 지원 시 현재 지원자 수 브로드캐스트
     *   - 지원 마감 후 후보자 결정
     *     (0명 → 전원 자동 지원, 1명 → 즉시 당첨, 2명+ → 게임 진행)
     * </pre>
     */
    public Set<Long> getVolunteers(Long roomId) {
        String key = getGameKey(roomId);
        Object volunteersJson = redisTemplate.opsForHash().get(key, "volunteers");
        if (volunteersJson == null) return new HashSet<>();
        try {
            return objectMapper.readValue(volunteersJson.toString(), new TypeReference<Set<Long>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize volunteers", e);
            return new HashSet<>();
        }
    }

    // ========================================================================
    // 투표 완료 유저 관리 (HTTP 투표용)
    // ========================================================================

    /**
     * 투표 완료 유저 추가
     *
     * 사용 시점: HTTP로 지원하기/지원안하기 요청 시
     * volunteers와 별개로 "투표를 완료했는지" 추적
     */
    public void addVotedUser(Long roomId, Long userId) {
        String key = getGameKey(roomId);
        Set<Long> votedUsers = getVotedUsers(roomId);
        votedUsers.add(userId);
        try {
            redisTemplate.opsForHash().put(key, "votedUsers", objectMapper.writeValueAsString(votedUsers));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize votedUsers", e);
        }
    }

    /**
     * 투표 완료 유저 목록 조회
     */
    public Set<Long> getVotedUsers(Long roomId) {
        String key = getGameKey(roomId);
        Object votedUsersJson = redisTemplate.opsForHash().get(key, "votedUsers");
        if (votedUsersJson == null) return new HashSet<>();
        try {
            return objectMapper.readValue(votedUsersJson.toString(), new TypeReference<Set<Long>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize votedUsers", e);
            return new HashSet<>();
        }
    }

    /**
     * 유저가 이미 투표했는지 확인
     */
    public boolean hasVoted(Long roomId, Long userId) {
        return getVotedUsers(roomId).contains(userId);
    }

    // ========================================================================
    // 게임 타입 / 결과 관리
    // ========================================================================

    /**
     * 선택된 게임 타입 저장
     *
     * 사용 시점: GAME_SELECT 단계에서 방장이 게임 선택
     * 예시:
     *   setSelectedGame(roomId, GameType.TIMING);
     *   if (gameType == GameType.TIMING) {
     *       setPhase(roomId, GamePhase.GAME_PLAYING);  // 타이밍: 개별 플레이
     *   } else {
     *       // 사다리/룰렛은 즉시 결과 계산 후 RESULT
     *   }
     */
    public void setSelectedGame(Long roomId, GameType gameType) {
        String key = getGameKey(roomId);
        redisTemplate.opsForHash().put(key, "selectedGame", gameType.name());
    }

    /**
     * 선택된 게임 타입 조회
     *
     * <pre>
     * 사용 시점: 타이밍 게임 진행 중 검증
     * 예시:
     *   if (getSelectedGame(roomId) != GameType.TIMING) {
     *       throw new YamoyoException(ErrorCode.INVALID_GAME_PHASE);
     *   }
     * </pre>
     */
    public GameType getSelectedGame(Long roomId) {
        String key = getGameKey(roomId);
        Object game = redisTemplate.opsForHash().get(key, "selectedGame");
        if (game == null) return null;
        return GameType.valueOf(game.toString());
    }

    /**
     * 당첨자(팀장) ID 저장
     *
     * <pre>
     * 사용 시점: 게임 결과 확정 후
     * 예시:
     *   setWinner(roomId, winnerId);
     *   setPhase(roomId, GamePhase.RESULT);
     *   dbService.applyLeaderResult(roomId, winnerId);  // DB 반영
     * </pre>
     */
    public void setWinner(Long roomId, Long winnerId) {
        String key = getGameKey(roomId);
        redisTemplate.opsForHash().put(key, "winnerId", winnerId.toString());
    }

    /**
     * 당첨자(팀장) ID 조회
     */
    public Long getWinner(Long roomId) {
        String key = getGameKey(roomId);
        Object winnerId = redisTemplate.opsForHash().get(key, "winnerId");
        if (winnerId == null) return null;
        return Long.parseLong(winnerId.toString());
    }

    /**
     * 게임 결과 데이터 저장 (사다리 매핑, 룰렛 인덱스 등)
     *
     * <pre>
     * 현재 미사용 - GameResultPayload로 직접 브로드캐스트 중
     * 필요 시 결과 재조회용으로 활용 가능
     * </pre>
     */
    public void setGameResult(Long roomId, Object result) {
        String key = getGameKey(roomId);
        try {
            redisTemplate.opsForHash().put(key, "gameResult", objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize game result", e);
        }
    }

    // ========================================================================
    // 타이밍 게임 전용 (Redis SortedSet 사용)
    // ========================================================================

    private static final String TIMING_KEY_PREFIX = "room:%d:timing";

    private String getTimingKey(Long roomId) {
        return String.format(TIMING_KEY_PREFIX, roomId);
    }

    /**
     * 타이밍 게임 기록 저장 (SortedSet)
     *
     * 사용 시점: 유저가 STOP 버튼 클릭 또는 시간 종료
     * 저장 방식: Redis SortedSet (score = timeDifference, member = oderId)
     * 예시:
     *   recordTimingResult(roomId, userId, 1.234);  // |7.777 - 실제시간| 차이값
     *   if (getTimingRecordCount(roomId) >= candidates.size()) {
     *       finishTimingGame(roomId);
     *   }
     *
     * @param timeDifference 7.777초와의 차이 (프론트에서 계산하여 전송)
     */
    public void recordTimingResult(Long roomId, Long userId, Double timeDifference) {
        String key = getTimingKey(roomId);
        redisTemplate.opsForZSet().add(key, userId.toString(), timeDifference);
        redisTemplate.expire(key, GAME_TTL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 유저가 이미 타이밍 기록을 제출했는지 확인
     */
    public boolean hasTimingRecord(Long roomId, Long userId) {
        String key = getTimingKey(roomId);
        Double score = redisTemplate.opsForZSet().score(key, userId.toString());
        return score != null;
    }

    /**
     * 타이밍 게임 기록 개수 조회
     */
    public long getTimingRecordCount(Long roomId) {
        String key = getTimingKey(roomId);
        Long count = redisTemplate.opsForZSet().zCard(key);
        return count != null ? count : 0;
    }

    /**
     * 타이밍 게임 전체 기록 조회 (점수 내림차순 - 차이가 큰 순)
     *
     * 사용 시점: 타이밍 게임 결과 계산
     * 반환: {userId: timeDifference} 맵 (차이가 큰 순서대로)
     * 예시:
     *   Map<Long, Double> records = getTimingRecords(roomId);
     *   // 첫 번째 = 차이가 가장 큰 사람 = 팀장
     */
    public Map<Long, Double> getTimingRecords(Long roomId) {
        String key = getTimingKey(roomId);
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, -1);

        Map<Long, Double> records = new LinkedHashMap<>();  // 순서 유지
        if (tuples != null) {
            for (var tuple : tuples) {
                Long oderId = Long.parseLong(tuple.getValue().toString());
                Double score = tuple.getScore();
                records.put(oderId, score);
            }
        }
        return records;
    }

    /**
     * 가장 차이가 큰 유저 ID 조회 (팀장 후보)
     *
     * @return 차이가 가장 큰 유저 ID, 없으면 null
     */
    public Long getMaxDifferenceUserId(Long roomId) {
        String key = getTimingKey(roomId);
        Set<Object> result = redisTemplate.opsForZSet().reverseRange(key, 0, 0);
        if (result == null || result.isEmpty()) return null;
        return Long.parseLong(result.iterator().next().toString());
    }

    /**
     * 타이밍 게임 기록 삭제
     */
    public void deleteTimingRecords(Long roomId) {
        redisTemplate.delete(getTimingKey(roomId));
    }

    // ========================================================================
    // 접속자(Connection) 관리 - Redis Set 사용
    // ========================================================================

    /**
     * 유저 접속 등록
     *
     * 사용 시점: WebSocket 연결 후 /join 메시지 수신 시
     * 예시:
     *   addConnection(roomId, userId);
     *   broadcast("USER_JOINED", participant);
     */
    public void addConnection(Long roomId, Long userId) {
        String key = getConnectionKey(roomId);
        redisTemplate.opsForSet().add(key, userId.toString());
        redisTemplate.expire(key, GAME_TTL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 유저 접속 해제
     *
     * 사용 시점:
     *   - /leave 메시지 수신 시
     *   - /confirm-result 후 퇴장 시
     *   - WebSocket 연결 끊김 감지 시
     */
    public void removeConnection(Long roomId, Long userId) {
        String key = getConnectionKey(roomId);
        redisTemplate.opsForSet().remove(key, userId.toString());
    }

    /**
     * 현재 접속 중인 유저 ID 목록 조회
     *
     * <pre>
     * 사용 시점:
     *   - 입장 시 현재 접속자 정보 전송
     *   - 게임 종료 후 전원 퇴장 여부 확인
     * </pre>
     */
    public Set<Long> getConnectedUsers(Long roomId) {
        String key = getConnectionKey(roomId);
        Set<Object> members = redisTemplate.opsForSet().members(key);
        if (members == null) return new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        for (Object member : members) {
            userIds.add(Long.parseLong(member.toString()));
        }
        return userIds;
    }

    /**
     * 모든 팀원이 접속했는지 확인
     *
     * <pre>
     * 사용 시점: 게임 시작 전 전원 접속 검증
     * 예시:
     *   List<Long> memberIds = members.stream().map(m -> m.getUser().getId()).toList();
     *   if (!areAllMembersConnected(roomId, memberIds)) {
     *       throw new YamoyoException(ErrorCode.NOT_ALL_MEMBERS_CONNECTED);
     *   }
     * </pre>
     *
     * @param memberIds DB에서 조회한 팀룸 멤버 ID 목록
     */
    public boolean areAllMembersConnected(Long roomId, List<Long> memberIds) {
        Set<Long> connectedUsers = getConnectedUsers(roomId);
        return connectedUsers.containsAll(memberIds);
    }
}
