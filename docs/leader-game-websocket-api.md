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

연결 성공 후 아래 3개 주소를 **모두** 구독해야 합니다.

### 2.1 구독 주소 목록

| 구독 주소 | 대상 | 수신 메시지 |
|----------|------|-----------|
| `/sub/room/{roomId}` | 방 전체 | USER_JOINED, USER_LEFT, USER_STATUS_CHANGE, PHASE_CHANGE, GAME_RESULT |
| `/user/queue/join-response` | 본인만 | JOIN_SUCCESS |
| `/user/queue/vote-status` | 투표 완료자만 | VOTE_UPDATED |

> `/sub/room/{roomId}` 구독 시 서버가 **팀 멤버 여부를 DB에서 검증**합니다.
> 팀 멤버가 아니면 구독이 거부됩니다.

> 에러 메시지는 `/sub/room/{roomId}/user/{userId}` 로 전달됩니다. 필요 시 추가 구독하세요.

### 2.2 구독 코드 예시

```javascript
function subscribeAll(roomId) {
  // 1. 방 전체 브로드캐스트
  client.subscribe(`/sub/room/${roomId}`, (message) => {
    const { type, payload } = JSON.parse(message.body);

    switch (type) {
      case 'USER_JOINED':       handleUserJoined(payload);       break;
      case 'USER_LEFT':         handleUserLeft(payload);         break;
      case 'USER_STATUS_CHANGE': handleStatusChange(payload);    break;
      case 'PHASE_CHANGE':      handlePhaseChange(payload);      break;
      case 'GAME_RESULT':       handleGameResult(payload);       break;
    }
  });

  // 2. 입장 응답 (본인에게만 전달)
  client.subscribe('/user/queue/join-response', (message) => {
    const { type, payload } = JSON.parse(message.body);
    // type === 'JOIN_SUCCESS'
    handleJoinSuccess(payload);
  });

  // 3. 투표 현황 (투표 완료 후부터 수신)
  client.subscribe('/user/queue/vote-status', (message) => {
    const { type, payload } = JSON.parse(message.body);
    // type === 'VOTE_UPDATED'
    handleVoteUpdated(payload);
  });

  // 4. (선택) 에러 메시지 수신
  client.subscribe(`/sub/room/${roomId}/user/${myUserId}`, (message) => {
    const { type, payload } = JSON.parse(message.body);
    // type === 'ERROR'
    handleError(payload);
  });
}
```

---

## 3. PUBLISH (메시지 발행)

클라이언트 -> 서버로 메시지를 전송하는 엔드포인트입니다.

### 3.1 엔드포인트 목록

join 은 게임 중간에 들어오는 유저를 위한 Endpoint 라서 사용할 일 없으실 것 같습니다.
그럼 팀룸 입장 처리는 어떻게 되는거냐?
TeamRoom API 에서 입장 API 호출 후 팀룸 DB에 저장
-> WebSocket Connect
-> 팀룸 Subscribe <- 이때 기존 팀원들에게 입장 알림
-> 기존 팀원들 목록은 GET /room/{roomId}/members 로 불러옴 (온오프라인 표시 되어있음.)
-> 이 members는 팀룸 입장 최초에만 호출하면 될 것 같습니다.
-> 이후에는 Disconnect 될 때마다 이벤트로 Disconnect 된 유저 정보를 publish 합니다. (오프라인 상태 필드 포함)

| 발행 주소 | 설명 | Body | 권한 |
|----------|------|------|------|
| `/pub/room/{roomId}/join` | 방 입장 | `{}` | 팀원 |
| `/pub/room/{roomId}/volunteer` | 팀장 지원 | `{}` | 팀원 |
| `/pub/room/{roomId}/pass` | 지원 안함 | `{}` | 팀원 |
| `/pub/room/{roomId}/select-game` | 게임 선택 | `{"gameType": "LADDER" \| "ROULETTE" \| "TIMING"}` | **방장** |
| `/pub/room/{roomId}/start-timing` | 타이밍 게임 시작 | `{}` | **방장** |
| `/pub/room/{roomId}/timing-result` | 타이밍 결과 제출 | `{"timeDifference": 1.234}` | 팀원 |
| `/pub/room/{roomId}/leave` | 방 퇴장 | `{}` | 팀원 |
| `/pub/room/{roomId}/confirm-result` | 결과 확인 완료 | `{}` | 팀원 |

### 3.2 발행 코드 예시

```javascript
// 방 입장
client.publish({ destination: `/pub/room/${roomId}/join`, body: '{}' });

// 팀장 지원
client.publish({ destination: `/pub/room/${roomId}/volunteer`, body: '{}' });

// 팀장 지원 안함
client.publish({ destination: `/pub/room/${roomId}/pass`, body: '{}' });

// 게임 선택 (방장만)
client.publish({
  destination: `/pub/room/${roomId}/select-game`,
  body: JSON.stringify({ gameType: 'LADDER' }),  // 'LADDER' | 'ROULETTE' | 'TIMING'
});

// 타이밍 게임 시작 (방장만)
client.publish({ destination: `/pub/room/${roomId}/start-timing`, body: '{}' });

// 타이밍 결과 제출
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

### 4.2 `GET /api/leader-games/rooms/{roomId}/members`

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

### 5.1 `JOIN_SUCCESS`

**수신 경로**: `/user/queue/join-response` (본인만)
**발생 시점**: `/pub/room/{roomId}/join` 발행 후

현재 방의 상태를 스냅샷으로 전달합니다. 중간 입장 시 현재 상태를 복원하는 용도입니다.
(위에서 말한 게임 도중 입장에 관한 join 이라서 신경쓰지 않으셔도 될 것 같습니다.
여기서는 유저들의 온오프라인 정보가 포함되어있지 않습니다. - 왜냐하면 게임 도중에 입장할 때를 가정하고 만들었기 때문입니다.
또한, participants는 게임 참여자이지 팀룸 멤버 전원이 아닙니다.)

```json
{
  "type": "JOIN_SUCCESS",
  "payload": {
    "user": {
      "userId": 1,
      "name": "홍길동",
      "profileImageId": "1"
    },
    "participants": [
      { "userId": 1, "name": "홍길동", "profileImageId": "1" },
      { "userId": 2, "name": "김철수", "profileImageId": "2" },
      { "userId": 3, "name": "이영희", "profileImageId": "3" }
    ],
    "connectedUserIds": [1, 2],
    "currentPhase": null,
    "phaseStartTime": null,
    "remainingTime": null
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| user | GameParticipant | 입장한 본인 정보 |
| participants | GameParticipant[] | 전체 참가자 목록 |
| connectedUserIds | Long[] | 현재 접속 중인 사용자 ID |
| currentPhase | String \| null | 현재 게임 단계 (게임 시작 전이면 null) |
| phaseStartTime | Long \| null | 현재 단계 시작 시간 (ms timestamp) |
| remainingTime | Long \| null | VOLUNTEER 단계 잔여 시간 (초) |

### 5.2 `USER_JOINED`

**수신 경로**: `/sub/room/{roomId}`
**발생 시점**: 다른 유저가 `/pub/room/{roomId}/join` 발행 시

```json
{
  "type": "USER_JOINED",
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
| `GAME_READY` | 타이밍 게임 대기 | `TIMING` | null |
| `GAME_PLAYING` | 타이밍 게임 진행 | `TIMING` | null |

### 5.6 `VOTE_UPDATED`

**수신 경로**: `/user/queue/vote-status` (투표 완료자만 수신)
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
    │        /user/queue/join-response
    │        /user/queue/vote-status
    ├─ 3. HTTP: GET /api/leader-games/rooms/{roomId}/members (멤버 목록 조회)
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
│  TODO: 전원 투표 완료 시 종료 추가 예정                │
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
│ GAME_READY 단계                                    │
│                                                    │
│  수신: PHASE_CHANGE {phase: "GAME_READY"}          │
│  발행: /start-timing (방장만)                       │
└──────────────────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────────────────┐
│ GAME_PLAYING 단계                                  │
│                                                    │
│  수신: PHASE_CHANGE {phase: "GAME_PLAYING"}        │
│  [프론트] 각 유저 개별 타이머 START → STOP           │
│  발행: /timing-result {timeDifference: ...}        │
│                                                    │
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
      // Step 1: 구독 (3개 필수 + 1개 선택)
      subscribeRoom(roomId);
      subscribeJoinResponse();
      subscribeVoteStatus();
      subscribeError(roomId, myUserId);  // 선택

      // Step 2: 입장
      client.publish({ destination: `/pub/room/${roomId}/join`, body: '{}' });
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
      case 'USER_JOINED':
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

function subscribeJoinResponse() {
  client.subscribe('/user/queue/join-response', (message) => {
    const { payload } = JSON.parse(message.body);
    // payload: { user, participants, connectedUserIds, currentPhase, ... }
    initializeRoomState(payload);
  });
}

function subscribeVoteStatus() {
  client.subscribe('/user/queue/vote-status', (message) => {
    const { payload } = JSON.parse(message.body);
    // payload: { votedUserIds, unvotedUserIds, volunteerIds, totalCount, votedCount }
    updateVoteUI(payload);
  });
}

function subscribeError(roomId, myUserId) {
  client.subscribe(`/sub/room/${roomId}/user/${myUserId}`, (message) => {
    const { payload } = JSON.parse(message.body);
    alert(payload.message);
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

// 타이밍 게임 시작 (방장만)
function startTiming(roomId) {
  client.publish({ destination: `/pub/room/${roomId}/start-timing`, body: '{}' });
}

// 타이밍 결과 제출
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

### 7.2 타이밍 게임 시간 계산

```javascript
let startTime = null;

// PHASE_CHANGE {phase: "GAME_PLAYING"} 수신 후
function onTimingStart() {
  startTime = performance.now();
}

// 유저가 STOP 버튼 클릭 시
function onTimingStop(roomId) {
  const elapsedMs = performance.now() - startTime;
  const elapsedSeconds = elapsedMs / 1000;
  const timeDifference = Math.abs(7.777 - elapsedSeconds);

  submitTimingResult(roomId, elapsedSeconds);
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
| **SUBSCRIBE** | `/user/queue/join-response` | 입장 응답 (본인만) |
| **SUBSCRIBE** | `/user/queue/vote-status` | 투표 현황 (본인만) |
| **SUBSCRIBE** | `/sub/room/{roomId}/user/{userId}` | 에러 메시지 (선택) |
| **PUBLISH** | `/pub/room/{roomId}/join` | 입장 |
| **PUBLISH** | `/pub/room/{roomId}/volunteer` | 팀장 지원 |
| **PUBLISH** | `/pub/room/{roomId}/pass` | 지원 안함 |
| **PUBLISH** | `/pub/room/{roomId}/select-game` | 게임 선택 (방장) |
| **PUBLISH** | `/pub/room/{roomId}/start-timing` | 타이밍 시작 (방장) |
| **PUBLISH** | `/pub/room/{roomId}/timing-result` | 타이밍 결과 제출 |
| **PUBLISH** | `/pub/room/{roomId}/leave` | 퇴장 |
| **PUBLISH** | `/pub/room/{roomId}/confirm-result` | 결과 확인 |

### HTTP REST

| Method | URL | 설명 | 권한 |
|--------|-----|------|------|
| `POST` | `/api/leader-games/rooms/{roomId}/start-volunteer` | VOLUNTEER 단계 시작 | 방장 |
| `GET` | `/api/leader-games/rooms/{roomId}/members` | 멤버 온라인 상태 조회 | 팀원 |
