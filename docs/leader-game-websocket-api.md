# 팀장 선출 게임 WebSocket API 문서

## 1. WebSocket 연결 정보

### 연결 URL
```
ws://localhost:8080/ws-stomp      (WebSocket)
http://localhost:8080/ws-stomp    (SockJS fallback)
```

> **배포 환경**: `wss://your-domain.com/ws-stomp` (HTTPS/WSS 사용)

### 연결 방법 (JavaScript)
```javascript
const socket = new SockJS('http://localhost:8080/ws-stomp');
const stompClient = Stomp.over(socket);

stompClient.connect(
    { 'Authorization': 'Bearer ' + accessToken },  // JWT 토큰 필수
    onConnected,
    onError
);
```

### 연결 해제
```javascript
// 정상 종료 절차
stompClient.send('/pub/room/' + roomId + '/confirm-result', {}, '{}');  // 게임 정리
stompClient.disconnect();  // WebSocket 연결 종료
```

---

## 2. 구독 (Subscribe) 엔드포인트

클라이언트가 메시지를 **수신**하기 위해 구독하는 주소

| 구독 주소 | 설명 | 수신 메시지 타입 |
|----------|------|-----------------|
| `/sub/room/{roomId}` | 방 전체 브로드캐스트 | USER_JOINED, USER_LEFT, PHASE_CHANGE, GAME_RESULT, USER_STATUS_CHANGE |
| `/user/queue/vote-status` | 개인 투표 현황 (투표 완료자만) | VOTE_UPDATED |
| `/user/queue/join-response` | 입장 응답 (본인만) | JOIN_SUCCESS |

### 구독 예시
```javascript
// 1. 방 전체 메시지 구독
stompClient.subscribe('/sub/room/' + roomId, function(message) {
    const data = JSON.parse(message.body);
    console.log(data.type, data.payload);
});

// 2. 개인 투표 현황 구독 (convertAndSendToUser용)
stompClient.subscribe('/user/queue/vote-status', function(message) {
    const data = JSON.parse(message.body);
    console.log(data.type, data.payload);  // VOTE_UPDATED
});
```

---

## 3. 발행 (Publish) 엔드포인트

클라이언트가 서버로 메시지를 **전송**하는 주소

| 발행 주소 | 설명 | 요청 Body | 권한 |
|----------|------|----------|------|
| `/pub/room/{roomId}/join` | 입장 | `{}` | 팀원 |
| `/pub/room/{roomId}/volunteer` | 팀장 지원 | `{}` | 팀원 |
| `/pub/room/{roomId}/pass` | 지원 안함 | `{}` | 팀원 |
| `/pub/room/{roomId}/select-game` | 게임 선택 | `{"gameType": "LADDER"\|"ROULETTE"\|"TIMING"}` | **방장** |
| `/pub/room/{roomId}/start-timing` | 타이밍 게임 시작 | `{}` | **방장** |
| `/pub/room/{roomId}/timing-result` | 타이밍 결과 제출 | `{"timeDifference": 1.234}` | 팀원 |
| `/pub/room/{roomId}/leave` | 방 퇴장 | `{}` | 팀원 |
| `/pub/room/{roomId}/confirm-result` | 결과 확인 완료 | `{}` | 팀원 |

### 발행 예시
```javascript
// 게임 선택 (방장)
stompClient.send('/pub/room/' + roomId + '/select-game', {},
    JSON.stringify({ gameType: 'TIMING' })
);

// 타이밍 결과 제출
stompClient.send('/pub/room/' + roomId + '/timing-result', {},
    JSON.stringify({ timeDifference: Math.abs(7.777 - elapsedTime) })
);
```

---

## 4. HTTP API 엔드포인트

WebSocket이 아닌 일반 REST API

| Method | URL | 설명 | 권한 |
|--------|-----|------|------|
| `POST` | `/api/leader-games/rooms/{roomId}/start-volunteer` | VOLUNTEER 단계 시작 | **방장** |
| `GET` | `/api/leader-games/rooms/{roomId}/members` | 멤버 온라인 상태 조회 | 팀원 |

### HTTP 요청 예시
```javascript
// VOLUNTEER 단계 시작 (방장)
fetch('/api/leader-games/rooms/' + roomId + '/start-volunteer', {
    method: 'POST',
    headers: {
        'Authorization': 'Bearer ' + accessToken,
        'Content-Type': 'application/json'
    }
});
```

---

## 5. 서버 → 클라이언트 메시지 타입

`/sub/room/{roomId}`로 수신되는 메시지

| type | payload | 발생 시점 |
|------|---------|----------|
| `USER_JOINED` | `{userId, name, profileImageId}` | 유저 입장 |
| `USER_LEFT` | `{userId}` | 유저 퇴장 |
| `USER_STATUS_CHANGE` | `{type, userId, profileImageId, status}` | 온라인/오프라인 변경 |
| `PHASE_CHANGE` | `{phase, phaseStartTime, durationSeconds?, gameType?}` | 게임 단계 변경 |
| `GAME_RESULT` | `{gameType, winnerId, winnerName, participants, gameData}` | 게임 결과 |
| `ERROR` | `{message}` | 에러 발생 |

`/user/queue/vote-status`로 수신되는 메시지 (투표 완료자만)

| type | payload | 발생 시점 |
|------|---------|----------|
| `VOTE_UPDATED` | `{votedUserIds, unvotedUserIds, volunteerIds, totalCount, votedCount}` | 누군가 투표할 때 |

---

## 6. 게임 흐름 및 엔드포인트 매핑

```
[대기 상태]
    │
    ├─ WebSocket 연결: new SockJS('/ws-stomp')
    ├─ 구독: /sub/room/{roomId}, /user/queue/vote-status, /user/queue/join-response
    ├─ 발행: SubscribeEvent 자동 발행 (입장한 유저는 /api/leader-games/rooms/{roomId}/members 로 이미 존재하는 유저 목록 불러오기
    │
    ▼
[VOLUNTEER 단계] ─── HTTP: POST /api/leader-games/rooms/{roomId}/start-volunteer (방장)
    │
    ├─ 수신: PHASE_CHANGE {phase: "VOLUNTEER", durationSeconds: 10}
    ├─ 발행: /pub/room/{roomId}/volunteer 또는 /pub/room/{roomId}/pass
    ├─ 수신: VOTE_UPDATED (투표자만)
    │
    ▼ (10초 후 자동 종료)

[분기]
    ├─ 지원자 1명 → 즉시 GAME_RESULT
    │
    ▼ 지원자 2명 이상 (0명은 전원)

[GAME_SELECT 단계]
    │
    ├─ 수신: PHASE_CHANGE {phase: "GAME_SELECT"}
    ├─ 발행: /pub/room/{roomId}/select-game {gameType: "..."} (방장)
    │
    ▼

[분기: 게임 타입별]
    │
    ├─ LADDER/ROULETTE → 즉시 GAME_RESULT 수신
    │
    ▼ TIMING 선택 시

[GAME_READY 단계]
    │
    ├─ 수신: PHASE_CHANGE {phase: "GAME_READY", gameType: "TIMING"}
    ├─ 발행: /pub/room/{roomId}/start-timing (방장)
    │
    ▼

[GAME_PLAYING 단계]
    │
    ├─ 수신: PHASE_CHANGE {phase: "GAME_PLAYING", gameType: "TIMING"}
    ├─ [프론트] 각 유저 개별 START → STOP
    ├─ 발행: /pub/room/{roomId}/timing-result {timeDifference: ...}
    │
    ▼ (전원 제출 시)

[RESULT 단계]
    │
    ├─ 수신: GAME_RESULT {winnerId, winnerName, gameData, ...}
    ├─ 발행: /pub/room/{roomId}/confirm-result
    ├─ WebSocket 연결 종료: stompClient.disconnect()
    │
    ▼
[종료]
```

---

## 7. 누락/추가 필요 항목

### 7.1. [해결됨] `/pub/room/{roomId}/join` 엔드포인트

**상태**: 추가 완료

- 발행: `/pub/room/{roomId}/join`
- 응답 구독: `/user/queue/join-response`
- 응답 타입: `JOIN_SUCCESS`

### 7.2. [확인 필요] 두 개의 Redis 접속 관리 시스템

**현재 상태**:
| 서비스 | Redis Key | 용도 |
|-------|-----------|------|
| `UserStatusService` | `room:{roomId}:online` | WebSocket 구독/해제 시 자동 관리 |
| `GameStateRedisService` | `room:{roomId}:connections` | 게임 로직에서 수동 관리 |

**문제점**: 두 시스템이 동기화되지 않아 불일치 발생 가능

**해결 방안**: 하나로 통합하거나, 명확한 역할 분리 필요

### 7.3. [확인 필요] WebSocket 비정상 종료 시 게임 정리

**현재 상태**:
- `SessionDisconnectEvent`에서 `UserStatusService.removeUserOffline()` 호출
- 하지만 `GameStateRedisService.removeConnection()`은 호출되지 않음

**영향**:
- `room:{roomId}:game` 데이터가 TTL(30분)까지 남아있음
- `confirmResult()` 미호출 시 게임 데이터 미정리

---

## 8. 메시지 포맷 예시

### PHASE_CHANGE
```json
{
  "type": "PHASE_CHANGE",
  "payload": {
    "phase": "VOLUNTEER",
    "phaseStartTime": 1706123456789,
    "durationSeconds": 10,
    "gameType": null
  }
}
```

### VOTE_UPDATED
```json
{
  "type": "VOTE_UPDATED",
  "payload": {
    "votedUserIds": [1, 2],
    "unvotedUserIds": [3],
    "volunteerIds": [1],
    "totalCount": 3,
    "votedCount": 2
  }
}
```

### GAME_RESULT (TIMING)
```json
{
  "type": "GAME_RESULT",
  "payload": {
    "gameType": "TIMING",
    "winnerId": 2,
    "winnerName": "홍길동",
    "participants": [
      {"userId": 1, "name": "김철수", "profileImageId": "1"},
      {"userId": 2, "name": "홍길동", "profileImageId": "2"}
    ],
    "gameData": {
      "1": 0.543,
      "2": 2.891
    }
  }
}
```

---

## 9. 전체 엔드포인트 요약

### WebSocket (STOMP)
| 유형 | 주소 | 설명 |
|-----|------|------|
| **CONNECT** | `ws://localhost:8080/ws-stomp` | WebSocket 연결 |
| **DISCONNECT** | - | `stompClient.disconnect()` 호출 |
| **SUBSCRIBE** | `/sub/room/{roomId}` | 방 브로드캐스트 구독 |
| **SUBSCRIBE** | `/user/queue/vote-status` | 개인 투표 현황 구독 |
| **SUBSCRIBE** | `/user/queue/join-response` | 입장 응답 구독 |
| **PUBLISH** | `/pub/room/{roomId}/join` | 입장 (JOIN_SUCCESS 응답) |
| **PUBLISH** | `/pub/room/{roomId}/volunteer` | 팀장 지원 |
| **PUBLISH** | `/pub/room/{roomId}/pass` | 지원 안함 |
| **PUBLISH** | `/pub/room/{roomId}/select-game` | 게임 선택 |
| **PUBLISH** | `/pub/room/{roomId}/start-timing` | 타이밍 시작 |
| **PUBLISH** | `/pub/room/{roomId}/timing-result` | 타이밍 결과 |
| **PUBLISH** | `/pub/room/{roomId}/leave` | 퇴장 |
| **PUBLISH** | `/pub/room/{roomId}/confirm-result` | 결과 확인 |

### HTTP REST
| Method | URL | 설명 |
|--------|-----|------|
| `POST` | `/api/leader-games/rooms/{roomId}/start-volunteer` | VOLUNTEER 시작 |
| `GET` | `/api/leader-games/rooms/{roomId}/members` | 멤버 조회 |
