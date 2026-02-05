# 팀장 선출 게임 WebSocket API 문서

## 1. WebSocket 연결

### 1.1 연결 정보

| 항목 | 값 |
|-----|---|
| WebSocket URL | `ws://localhost:8080/ws-stomp` |
| SockJS Fallback | `http://localhost:8080/ws-stomp` |
| 배포 환경 | `wss://your-domain.com/ws-stomp` |
| 프로토콜 | STOMP over WebSocket |
| 인증 방식 | CONNECT 프레임의 `Authorization` 헤더에 `Bearer {JWT}` |

### 1.2 CONNECT (연결)

STOMP CONNECT 시 JWT 토큰을 `Authorization` 헤더에 전달해야 합니다.
토큰이 없거나 유효하지 않으면 연결이 거부됩니다.

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws-stomp'),
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`,
  },
  onConnect: (frame) => {
    console.log('Connected:', frame);
    // 연결 성공 후 구독 시작
    subscribeAll(roomId);
  },
  onStompError: (frame) => {
    console.error('STOMP error:', frame.headers['message']);
  },
  onDisconnect: () => {
    console.log('Disconnected');
  },
});

client.activate();
```

### 1.3 DISCONNECT (연결 해제)

```javascript
// 게임 결과 확인 후 정상 종료
client.publish({ destination: `/pub/room/${roomId}/confirm-result`, body: '{}' });
client.deactivate();
```

비정상 종료(브라우저 종료, 네트워크 끊김)의 경우 서버가 `SessionDisconnectEvent`를 감지하여 자동으로 퇴장 처리합니다.

---

## 2. SUBSCRIBE (구독)

연결 성공 후 아래 2개 주소를 **모두** 구독해야 합니다.

### 2.1 구독 주소 목록

| 구독 주소 | 대상 | 수신 메시지 |
|----------|------|-----------|
| `/sub/room/{roomId}` | 방 전체 | USER_RECONNECTED, USER_LEFT, USER_STATUS_CHANGE, PHASE_CHANGE, GAME_RESULT |
| `/sub/room/{roomId}/user/{userId}` | 본인만 | RELOAD_SUCCESS, VOTE_UPDATED, ERROR |

> `/sub/room/{roomId}` 구독 시 서버가 **팀 멤버 여부를 DB에서 검증**합니다.
> 팀 멤버가 아니면 구독이 거부됩니다.

### 2.2 구독 코드 예시

```javascript
function subscribeAll(roomId, myUserId) {
  // 1. 방 전체 브로드캐스트
  client.subscribe(`/sub/room/${roomId}`, (message) => {
    const { type, payload } = JSON.parse(message.body);

    switch (type) {
      case 'USER_RECONNECTED':  handleUserReconnected(payload);  break;
      case 'USER_LEFT':         handleUserLeft(payload);         break;
      case 'USER_STATUS_CHANGE': handleStatusChange(payload);    break;
      case 'PHASE_CHANGE':      handlePhaseChange(payload);      break;
      case 'GAME_RESULT':       handleGameResult(payload);       break;
    }
  });

  // 2. 개인 메시지 (reload 응답, 투표 현황, 에러)
  client.subscribe(`/sub/room/${roomId}/user/${myUserId}`, (message) => {
    const { type, payload } = JSON.parse(message.body);

    switch (type) {
      case 'RELOAD_SUCCESS':  handleReloadSuccess(payload);  break;
      case 'VOTE_UPDATED':    handleVoteUpdated(payload);    break;
      case 'ERROR':           handleError(payload);          break;
    }
  });
}
```

---

## 3. PUBLISH (메시지 발행)

클라이언트 -> 서버로 메시지를 전송하는 엔드포인트입니다.

### 3.1 엔드포인트 목록

reload 는 새로고침이나 게임 중간에 나갔다 들어온 유저를 위한 Endpoint입니다.
현재 방의 전체 상태(멤버, 지원자, 투표 현황, 게임 상태 등)를 반환합니다.

팀룸 입장 처리 흐름:
1. TeamRoom API에서 입장 API 호출 후 팀룸 DB에 저장
2. WebSocket Connect
3. 팀룸 Subscribe ← 이때 기존 팀원들에게 입장 알림
4. `/pub/room/{roomId}/reload` 호출 → 현재 방 상태 수신
5. 이후 Disconnect 될 때마다 이벤트로 Disconnect 된 유저 정보를 publish 합니다.

| 발행 주소 | 설명 | Body | 권한 |
|----------|------|------|------|
| `/pub/room/{roomId}/reload` | 방 상태 조회 (새로고침/재접속) | `{}` | 팀원 |
| `/pub/room/{roomId}/volunteer` | 팀장 지원 | `{}` | 팀원 |
| `/pub/room/{roomId}/pass` | 지원 안함 | `{}` | 팀원 |
| `/pub/room/{roomId}/select-game` | 게임 선택 | `{"gameType": "LADDER" \| "ROULETTE" \| "TIMING"}` | **방장** |
| `/pub/room/{roomId}/timing-result` | 타이밍 결과 제출 | `{"timeDifference": 1.234}` | 팀원 |
| `/pub/room/{roomId}/leave` | 방 퇴장 | `{}` | 팀원 |
| `/pub/room/{roomId}/confirm-result` | 결과 확인 완료 | `{}` | 팀원 |

### 3.2 발행 코드 예시

```javascript
// 방 상태 조회 (새로고침/재접속 시)
client.publish({ destination: `/pub/room/${roomId}/reload`, body: '{}' });

// 팀장 지원
client.publish({ destination: `/pub/room/${roomId}/volunteer`, body: '{}' });

// 팀장 지원 안함
client.publish({ destination: `/pub/room/${roomId}/pass`, body: '{}' });

// 게임 선택 (방장만)
client.publish({
  destination: `/pub/room/${roomId}/select-game`,
  body: JSON.stringify({ gameType: 'LADDER' }),  // 'LADDER' | 'ROULETTE' | 'TIMING'
});

// 타이밍 결과 제출 (각 유저가 개별적으로 게임 후 제출)
client.publish({
  destination: `/pub/room/${roomId}/timing-result`,
  body: JSON.stringify({ timeDifference: Math.abs(7.777 - elapsedSeconds) }),
});

// 결과 확인 후 정리
client.publish({ destination: `/pub/room/${roomId}/confirm-result`, body: '{}' });
```

---

## 4. HTTP REST API

WebSocket과 함께 사용되는 HTTP 엔드포인트입니다.

### 4.1 `POST /api/leader-games/rooms/{roomId}/start-volunteer`

VOLUNTEER 단계를 시작합니다. **방장만** 호출 가능합니다.
전원이 WebSocket에 접속한 상태여야 합니다.

**Request**
```
POST /api/leader-games/rooms/{roomId}/start-volunteer
Authorization: Bearer {accessToken}
```

**Response `200 OK`**
```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": {
    "phase": "VOLUNTEER",
    "phaseStartTime": 1706123456789,
    "durationSeconds": 60000,
    "participants": [
      { "userId": 1, "name": "홍길동", "profileImageId": "1" },
      { "userId": 2, "name": "김철수", "profileImageId": "2" }
    ]
  }
}
```

**Error**

| HTTP Status | ErrorCode | 조건 |
|-------------|-----------|------|
| 404 | TEAMROOM_NOT_FOUND | 팀룸 없음 |
| 400 | GAME_ALREADY_IN_PROGRESS | 이미 게임 진행 중 |
| 403 | NOT_ROOM_HOST | 방장 아님 |
| 400 | NOT_ALL_MEMBERS_CONNECTED | 전원 미접속 |

### 4.2 `POST /test/dummy-user` (테스트 전용)

**인증 없이** 더미 유저를 생성하고 JWT 토큰을 발급받습니다.
프론트엔드에서 WebSocket 테스트 시 OAuth2 로그인 없이 빠르게 테스트할 수 있습니다.

> ⚠️ **개발/테스트 환경 전용 API입니다. 프로덕션에서는 비활성화해야 합니다.**

**Request**
```
POST /test/dummy-user
Content-Type: application/json

(Body 없음)
```

**Response `200 OK`**
```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": {
    "userId": 123,
    "email": "test-a1b2c3d4@yamoyo.test",
    "name": "테스트유저3",
    "major": "컴퓨터공학",
    "mbti": "INTJ",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| userId | Long | 생성된 유저 ID |
| email | String | 랜덤 생성된 이메일 (test-{UUID}@yamoyo.test) |
| name | String | 랜덤 선택된 이름 (테스트유저1~10) |
| major | String | 랜덤 선택된 전공 |
| mbti | String | 랜덤 선택된 MBTI |
| accessToken | String | JWT Access Token (WebSocket 연결 시 사용) |
| refreshToken | String | JWT Refresh Token |

**사용 예시 (프론트엔드)**
```javascript
// 1. 더미 유저 생성
const res = await fetch('http://localhost:8080/test/dummy-user', {
  method: 'POST',
});
const { data } = await res.json();

console.log('생성된 유저:', data.userId, data.name);
console.log('Access Token:', data.accessToken);

// 2. WebSocket 연결 시 사용
const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws-stomp'),
  connectHeaders: {
    Authorization: `Bearer ${data.accessToken}`,
  },
  onConnect: () => {
    console.log('WebSocket 연결 성공!');
    // 구독 및 테스트 진행...
  },
});

client.activate();
```

**테스트 시나리오 예시**
```javascript
// 여러 유저로 팀장 게임 테스트
async function setupTestUsers(count) {
  const users = [];
  for (let i = 0; i < count; i++) {
    const res = await fetch('http://localhost:8080/test/dummy-user', { method: 'POST' });
    const { data } = await res.json();
    users.push(data);
  }
  return users;
}

// 3명의 더미 유저 생성
const [user1, user2, user3] = await setupTestUsers(3);

// 각 유저로 WebSocket 연결하여 테스트
```

### 4.3 `GET /api/leader-games/rooms/{roomId}/members`

팀 멤버 목록과 온라인 상태를 조회합니다.

**Request**
```
GET /api/leader-games/rooms/{roomId}/members
Authorization: Bearer {accessToken}
```

**Response `200 OK`**
```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": [
    {
      "userId": 1,
      "name": "홍길동",
      "role": "HOST",
      "major": "컴퓨터공학",
      "profileImageId": 1,
      "status": "ONLINE"
    },
    {
      "userId": 2,
      "name": "김철수",
      "role": "MEMBER",
      "major": "경영학",
      "profileImageId": 2,
      "status": "OFFLINE"
    }
  ]
}
```

---

## 5. 서버 -> 클라이언트 메시지 상세

모든 메시지는 아래 포맷으로 전달됩니다:

```json
{
  "type": "MESSAGE_TYPE",
  "payload": { ... }
}
```

### 5.1 `RELOAD_SUCCESS`

**수신 경로**: `/sub/room/{roomId}/user/{userId}` (본인만)
**발생 시점**: `/pub/room/{roomId}/reload` 발행 후

현재 방의 전체 상태를 스냅샷으로 전달합니다. 새로고침/재접속 시 현재 상태를 복원하는 용도입니다.

```json
{
  "type": "RELOAD_SUCCESS",
  "payload": {
    "currentUser": {
      "userId": 1,
      "name": "홍길동",
      "profileImageId": "1"
    },
    "members": [
      { "userId": 1, "name": "홍길동", "profileImageId": "1" },
      { "userId": 2, "name": "김철수", "profileImageId": "2" },
      { "userId": 3, "name": "이영희", "profileImageId": "3" }
    ],
    "connectedUserIds": [1, 2],
    "volunteers": [1],
    "votedUsers": [1, 2],
    "currentPhase": "VOLUNTEER",
    "phaseStartTime": 1706123456789,
    "remainingTime": 45,
    "selectedGame": null,
    "winnerId": null,
    "winnerName": null
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| currentUser | GameParticipant | 현재 유저 정보 |
| members | GameParticipant[] | 전체 팀 멤버 목록 |
| connectedUserIds | Long[] | 현재 접속 중인 사용자 ID |
| volunteers | Long[] | 팀장 지원자 ID 목록 |
| votedUsers | Long[] | 투표 완료한 사용자 ID 목록 |
| currentPhase | String \| null | 현재 게임 단계 (게임 시작 전이면 null) |
| phaseStartTime | Long \| null | 현재 단계 시작 시간 (ms timestamp) |
| remainingTime | Long \| null | VOLUNTEER 단계 잔여 시간 (초) |
| selectedGame | String \| null | 선택된 게임 타입 (LADDER, ROULETTE, TIMING) |
| winnerId | Long \| null | 당첨자(팀장) ID (RESULT 단계에서만) |
| winnerName | String \| null | 당첨자(팀장) 이름 (RESULT 단계에서만) |

> **참고**: `members`는 팀룸의 전체 멤버이고, `volunteers`는 팀장 지원자입니다.
> VOLUNTEER 단계에서는 전체 멤버가 투표하고, 이후 게임 단계에서는 지원자(volunteers)만 게임에 참여합니다.

### 5.2 `USER_RECONNECTED`

**수신 경로**: `/sub/room/{roomId}`
**발생 시점**: 다른 유저가 `/pub/room/{roomId}/reload` 발행 시

```json
{
  "type": "USER_RECONNECTED",
  "payload": {
    "userId": 2,
    "name": "김철수",
    "profileImageId": "2"
  }
}
```

### 5.3 `USER_LEFT`

**수신 경로**: `/sub/room/{roomId}`
**발생 시점**: 유저가 `/pub/room/{roomId}/leave` 발행 또는 연결 끊김

```json
{
  "type": "USER_LEFT",
  "payload": {
    "userId": 2
  }
}
```

### 5.4 `USER_STATUS_CHANGE`

**수신 경로**: `/sub/room/{roomId}`
**발생 시점**: `/sub/room/{roomId}` 구독/해제 시 자동 발생

```json
{
  "type": "USER_STATUS_CHANGE",
  "payload": {
    "type": "ONLINE",
    "userId": 2,
    "profileImageId": 1,
    "status": "ONLINE"
  }
}
```

| status 값 | 설명 |
|-----------|------|
| `ONLINE` | 유저 접속 |
| `OFFLINE` | 유저 접속 해제 |

> `payload.type`은 `ONLINE`, `OFFLINE` 외에 `LEAVE`, `KICK`일 수도 있습니다 (팀룸 퇴장/강퇴 시).

### 5.5 `PHASE_CHANGE`

**수신 경로**: `/sub/room/{roomId}`
**발생 시점**: 게임 단계가 전환될 때

```json
{
  "type": "PHASE_CHANGE",
  "payload": {
    "phase": "VOLUNTEER",
    "phaseStartTime": 1706123456789,
    "durationSeconds": 60000,
    "selectedGame": null
  }
}
```

| phase 값 | 설명 | selectedGame | durationSeconds |
|----------|------|-------------|-----------------|
| `VOLUNTEER` | 지원 단계 (자동 종료) | null | 60000 |
| `GAME_SELECT` | 방장이 게임 선택 | null | null |
| `GAME_PLAYING` | 타이밍 게임 진행 (각자 개별 플레이) | `TIMING` | null |
| `RESULT` | 결과 발표 | 선택된 게임 | null |

### 5.6 `VOTE_UPDATED`

**수신 경로**: `/sub/room/{roomId}/user/{userId}` (투표 완료자만 수신)
**발생 시점**: 누군가 volunteer 또는 pass 할 때

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

| 필드 | 타입 | 설명 |
|------|------|------|
| votedUserIds | Long[] | 투표 완료한 사용자 ID |
| unvotedUserIds | Long[] | 아직 투표하지 않은 사용자 ID |
| volunteerIds | Long[] | 지원한 사용자 ID |
| totalCount | int | 전체 참가자 수 |
| votedCount | int | 투표 완료자 수 |

> 투표하지 않은 유저는 이 메시지를 수신하지 못합니다.
> 투표 전에는 다른 사람의 투표 현황을 알 수 없습니다.

### 5.7 `GAME_RESULT`

**수신 경로**: `/sub/room/{roomId}`
**발생 시점**: 게임 종료 시 (팀장 확정)

#### LADDER 게임 결과

```json
{
  "type": "GAME_RESULT",
  "payload": {
    "gameType": "LADDER",
    "winnerId": 1,
    "winnerName": "홍길동",
    "participants": [
      { "userId": 1, "name": "홍길동", "profileImageId": "1" },
      { "userId": 2, "name": "김철수", "profileImageId": "2" }
    ],
    "gameData": {
      "ladderLines": [[true, false], [false, true]],
      "mappings": { "0": 1, "1": 0 },
      "winnerStartIndex": 0
    }
  }
}
```

#### ROULETTE 게임 결과

```json
{
  "type": "GAME_RESULT",
  "payload": {
    "gameType": "ROULETTE",
    "winnerId": 2,
    "winnerName": "김철수",
    "participants": [
      { "userId": 1, "name": "홍길동", "profileImageId": "1" },
      { "userId": 2, "name": "김철수", "profileImageId": "2" }
    ],
    "gameData": 1
  }
}
```

> `gameData`는 participants 배열에서 당첨자의 인덱스입니다.

#### TIMING 게임 결과

```json
{
  "type": "GAME_RESULT",
  "payload": {
    "gameType": "TIMING",
    "winnerId": 2,
    "winnerName": "김철수",
    "participants": [
      { "userId": 1, "name": "홍길동", "profileImageId": "1" },
      { "userId": 2, "name": "김철수", "profileImageId": "2" }
    ],
    "gameData": {
      "1": 0.543,
      "2": 2.891
    }
  }
}
```

> `gameData`는 `{ userId: timeDifference }` 맵입니다.
> `timeDifference`가 **가장 큰** 유저가 팀장이 됩니다 (7.777초에서 가장 멀리 벗어난 사람).

### 5.8 `ERROR`

**수신 경로**: `/sub/room/{roomId}/user/{userId}` (해당 유저만)
**발생 시점**: PUBLISH 요청 처리 중 오류 발생 시

```json
{
  "type": "ERROR",
  "payload": {
    "message": "현재 게임 단계에서 해당 작업을 수행할 수 없습니다."
  }
}
```

---

## 6. 게임 흐름 다이어그램

```
[대기 상태]
    │
    ├─ 1. WebSocket 연결: new SockJS('/ws-stomp') + STOMP CONNECT (JWT 필수)
    ├─ 2. 구독: /sub/room/{roomId}
    │        /sub/room/{roomId}/user/{myUserId}
    ├─ 3. 발행: /pub/room/{roomId}/reload (현재 방 상태 조회)
    │
    ▼ 방장이 HTTP: POST /start-volunteer 호출
┌──────────────────────────────────────────────────┐
│ VOLUNTEER 단계                                     │
│                                                    │
│  수신: PHASE_CHANGE {phase: "VOLUNTEER"}           │
│  발행: /volunteer 또는 /pass                        │
│  수신: VOTE_UPDATED (투표 완료 후부터)               │
│                                                    │
│  ※ 자동 종료: durationSeconds 후 서버가 자동 처리    │
│  ※ 전원 투표 완료 시 즉시 종료                       │
└──────────────────────────────────────────────────┘
    │
    ├─ 지원자 0명 → 전원 자동 지원 → GAME_SELECT로
    ├─ 지원자 1명 → 즉시 GAME_RESULT (팀장 확정)
    ▼ 지원자 2명 이상
┌──────────────────────────────────────────────────┐
│ GAME_SELECT 단계                                   │
│                                                    │
│  수신: PHASE_CHANGE {phase: "GAME_SELECT"}         │
│  발행: /select-game {gameType: "..."} (방장만)      │
└──────────────────────────────────────────────────┘
    │
    ├─ LADDER  → 즉시 GAME_RESULT 수신 → RESULT로
    ├─ ROULETTE → 즉시 GAME_RESULT 수신 → RESULT로
    ▼ TIMING 선택 시
┌──────────────────────────────────────────────────┐
│ GAME_PLAYING 단계 (타이밍 게임)                     │
│                                                    │
│  수신: PHASE_CHANGE {phase: "GAME_PLAYING"}        │
│  [프론트] 각 유저가 개별적으로 START → STOP 버튼    │
│  발행: /timing-result {timeDifference: ...}        │
│                                                    │
│  ※ 서버가 타이밍을 관리하지 않음                    │
│  ※ 각 유저가 자신의 타이머를 관리하고 결과만 제출    │
│  ※ 전원 제출 시 서버가 자동으로 결과 계산            │
└──────────────────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────────────────┐
│ RESULT 단계                                        │
│                                                    │
│  수신: GAME_RESULT {winnerId, winnerName, ...}     │
│  발행: /confirm-result (결과 확인 후)               │
│  WebSocket 연결 종료: client.deactivate()          │
└──────────────────────────────────────────────────┘
```

---

## 7. 프론트엔드 구현 가이드

### 7.1 전체 연결 ~ 게임 완료 코드

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const BASE_URL = 'http://localhost:8080';
let client = null;

// ──────────────────────── 연결 ────────────────────────

function connect(accessToken, roomId, myUserId) {
  client = new Client({
    webSocketFactory: () => new SockJS(`${BASE_URL}/ws-stomp`),
    connectHeaders: {
      Authorization: `Bearer ${accessToken}`,
    },
    onConnect: () => {
      // Step 1: 구독 (2개 필수)
      subscribeRoom(roomId);
      subscribeUserMessages(roomId, myUserId);

      // Step 2: 방 상태 조회
      client.publish({ destination: `/pub/room/${roomId}/reload`, body: '{}' });
    },
    onStompError: (frame) => {
      console.error('연결 실패:', frame.headers['message']);
    },
  });

  client.activate();
}

// ──────────────────────── 구독 ────────────────────────

function subscribeRoom(roomId) {
  client.subscribe(`/sub/room/${roomId}`, (message) => {
    const { type, payload } = JSON.parse(message.body);

    switch (type) {
      case 'USER_RECONNECTED':
        // payload: { userId, name, profileImageId }
        break;
      case 'USER_LEFT':
        // payload: { userId }
        break;
      case 'USER_STATUS_CHANGE':
        // payload: { type, userId, profileImageId, status }
        break;
      case 'PHASE_CHANGE':
        // payload: { phase, phaseStartTime, durationSeconds, selectedGame }
        handlePhaseChange(payload);
        break;
      case 'GAME_RESULT':
        // payload: { gameType, winnerId, winnerName, participants, gameData }
        handleGameResult(payload);
        break;
    }
  });
}

function subscribeUserMessages(roomId, myUserId) {
  // 개인 메시지 (reload 응답, 투표 현황, 에러) - 단일 구독으로 통합
  client.subscribe(`/sub/room/${roomId}/user/${myUserId}`, (message) => {
    const { type, payload } = JSON.parse(message.body);

    switch (type) {
      case 'RELOAD_SUCCESS':
        // payload: { currentUser, members, connectedUserIds, volunteers, votedUsers, currentPhase, ... }
        initializeRoomState(payload);
        break;
      case 'VOTE_UPDATED':
        // payload: { votedUserIds, unvotedUserIds, volunteerIds, totalCount, votedCount }
        updateVoteUI(payload);
        break;
      case 'ERROR':
        alert(payload.message);
        break;
    }
  });
}

// ──────────────────────── 발행 ────────────────────────

// VOLUNTEER 단계 시작 (방장만, HTTP 호출)
async function startVolunteerPhase(roomId, accessToken) {
  const res = await fetch(`${BASE_URL}/api/leader-games/rooms/${roomId}/start-volunteer`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  return res.json();
}

// 팀장 지원
function volunteer(roomId) {
  client.publish({ destination: `/pub/room/${roomId}/volunteer`, body: '{}' });
}

// 지원 안함
function pass(roomId) {
  client.publish({ destination: `/pub/room/${roomId}/pass`, body: '{}' });
}

// 게임 선택 (방장만)
function selectGame(roomId, gameType) {
  // gameType: 'LADDER' | 'ROULETTE' | 'TIMING'
  client.publish({
    destination: `/pub/room/${roomId}/select-game`,
    body: JSON.stringify({ gameType }),
  });
}

// 타이밍 결과 제출 (개별 게임 완료 후)
function submitTimingResult(roomId, elapsedSeconds) {
  const timeDifference = Math.abs(7.777 - elapsedSeconds);
  client.publish({
    destination: `/pub/room/${roomId}/timing-result`,
    body: JSON.stringify({ timeDifference }),
  });
}

// 결과 확인 후 종료
function confirmAndDisconnect(roomId) {
  client.publish({ destination: `/pub/room/${roomId}/confirm-result`, body: '{}' });
  client.deactivate();
}
```

### 7.2 타이밍 게임 구현 (개별 플레이)

타이밍 게임은 서버가 시간을 관리하지 않습니다. 각 유저가 개별적으로 게임을 진행합니다.

```javascript
let startTime = null;
let isPlaying = false;

// PHASE_CHANGE {phase: "GAME_PLAYING"} 수신 후 게임 화면 표시
function onTimingGameReady() {
  // 게임 UI 표시 (START 버튼 활성화)
  showTimingGameUI();
}

// 유저가 START 버튼 클릭 시
function onTimingStart() {
  startTime = performance.now();
  isPlaying = true;
  // UI 업데이트 (타이머 시작, STOP 버튼 표시)
}

// 유저가 STOP 버튼 클릭 시
function onTimingStop(roomId) {
  if (!isPlaying) return;

  const elapsedMs = performance.now() - startTime;
  const elapsedSeconds = elapsedMs / 1000;
  const timeDifference = Math.abs(7.777 - elapsedSeconds);

  isPlaying = false;

  // 서버에 결과 제출
  client.publish({
    destination: `/pub/room/${roomId}/timing-result`,
    body: JSON.stringify({ timeDifference }),
  });

  // UI 업데이트 (대기 화면으로 전환, 다른 유저 대기 중 표시)
}
```

### 7.3 방 상태 초기화 (reload 응답 처리)

```javascript
function initializeRoomState(payload) {
  const {
    currentUser,
    members,
    connectedUserIds,
    volunteers,
    votedUsers,
    currentPhase,
    phaseStartTime,
    remainingTime,
    selectedGame,
    winnerId,
    winnerName
  } = payload;

  // 멤버 목록 초기화 (온라인 상태 포함)
  setMembers(members.map(member => ({
    ...member,
    isOnline: connectedUserIds.includes(member.userId)
  })));

  // 현재 게임 단계에 따른 UI 복원
  switch (currentPhase) {
    case null:
      // 게임 시작 전 - 대기 화면
      showWaitingScreen();
      break;
    case 'VOLUNTEER':
      // 투표 단계 - 투표 여부에 따라 UI 분기
      const hasVoted = votedUsers.includes(currentUser.userId);
      if (hasVoted) {
        showVoteStatus({ volunteers, votedUsers });
      } else {
        showVoteScreen(remainingTime);
      }
      break;
    case 'GAME_SELECT':
      // 게임 선택 대기 (방장이면 선택 UI, 아니면 대기)
      showGameSelectScreen();
      break;
    case 'GAME_PLAYING':
      // 타이밍 게임 진행 중
      showTimingGameUI();
      break;
    case 'RESULT':
      // 결과 화면
      showResultScreen({ winnerId, winnerName, selectedGame });
      break;
  }
}
```

---

## 8. 엔드포인트 전체 요약

### WebSocket STOMP

| 유형 | 주소 | 설명 |
|-----|------|------|
| **CONNECT** | `ws://localhost:8080/ws-stomp` | JWT 인증과 함께 연결 |
| **DISCONNECT** | - | `client.deactivate()` |
| **SUBSCRIBE** | `/sub/room/{roomId}` | 방 브로드캐스트 (팀 멤버 검증) |
| **SUBSCRIBE** | `/sub/room/{roomId}/user/{userId}` | 개인 메시지 (reload, 투표, 에러) |
| **PUBLISH** | `/pub/room/{roomId}/reload` | 방 상태 조회 (새로고침/재접속) |
| **PUBLISH** | `/pub/room/{roomId}/volunteer` | 팀장 지원 |
| **PUBLISH** | `/pub/room/{roomId}/pass` | 지원 안함 |
| **PUBLISH** | `/pub/room/{roomId}/select-game` | 게임 선택 (방장) |
| **PUBLISH** | `/pub/room/{roomId}/timing-result` | 타이밍 결과 제출 |
| **PUBLISH** | `/pub/room/{roomId}/leave` | 퇴장 |
| **PUBLISH** | `/pub/room/{roomId}/confirm-result` | 결과 확인 |

### HTTP REST

| Method | URL | 설명 | 권한 |
|--------|-----|------|------|
| `POST` | `/test/dummy-user` | 테스트용 더미 유저 생성 | 없음 (테스트 전용) |
| `POST` | `/api/leader-games/rooms/{roomId}/start-volunteer` | VOLUNTEER 단계 시작 | 방장 |
| `GET` | `/api/leader-games/rooms/{roomId}/members` | 멤버 온라인 상태 조회 | 팀원 |
