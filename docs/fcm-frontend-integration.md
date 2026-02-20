# FCM 푸시 알림 프론트엔드 통합 가이드

## 1. 개요

Yamoyo 백엔드는 Firebase Cloud Messaging(FCM)을 통해 푸시 알림을 발송합니다. 이 문서는 프론트엔드에서 FCM을 통합하는 방법을 안내합니다.

### 아키텍처

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Frontend   │────▶│   Backend   │────▶│     FCM     │
│  (Web/App)  │     │   (Spring)  │     │  (Firebase) │
└─────────────┘     └─────────────┘     └─────────────┘
       │                   │                   │
       │  1. FCM 토큰 등록  │                   │
       │──────────────────▶│                   │
       │                   │                   │
       │                   │  2. 푸시 메시지    │
       │                   │──────────────────▶│
       │                   │                   │
       │                   │  3. 푸시 전달      │
       │◀──────────────────────────────────────│
       │                   │                   │
```

### 전체 플로우

1. 프론트엔드에서 Firebase SDK로 FCM 토큰 발급
2. 발급받은 토큰을 백엔드 API로 등록
3. 백엔드에서 이벤트 발생 시 FCM으로 푸시 발송
4. 프론트엔드에서 푸시 메시지 수신 및 처리

---

## 2. Firebase 프로젝트 설정

### 2.1 Firebase Console 설정

1. [Firebase Console](https://console.firebase.google.com)에서 프로젝트 선택
2. **프로젝트 설정 > 일반** 에서 웹 앱 추가
3. **Cloud Messaging** 탭에서 웹 푸시 인증서 설정

### 2.2 VAPID 키 발급 (웹 전용)

1. Firebase Console > 프로젝트 설정 > Cloud Messaging
2. **웹 구성** 섹션에서 "키 쌍 생성" 클릭
3. 발급된 공개 키(VAPID Key)를 프론트엔드에서 사용

### 2.3 프론트엔드 환경변수

```env
VITE_FIREBASE_API_KEY=your-api-key
VITE_FIREBASE_AUTH_DOMAIN=your-project.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=your-project-id
VITE_FIREBASE_STORAGE_BUCKET=your-project.appspot.com
VITE_FIREBASE_MESSAGING_SENDER_ID=123456789
VITE_FIREBASE_APP_ID=1:123456789:web:abc123
VITE_FIREBASE_VAPID_KEY=your-vapid-public-key
```

---

## 3. 프론트엔드 구현

### 3.1 Firebase 초기화

#### firebase-config.js

```javascript
import { initializeApp } from 'firebase/app';
import { getMessaging, getToken, onMessage } from 'firebase/messaging';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
};

const app = initializeApp(firebaseConfig);
export const messaging = getMessaging(app);
```

#### Service Worker 설정 (firebase-messaging-sw.js)

프로젝트 루트의 `public` 폴더에 생성:

```javascript
importScripts('https://www.gstatic.com/firebasejs/10.7.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.7.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: 'your-api-key',
  authDomain: 'your-project.firebaseapp.com',
  projectId: 'your-project-id',
  storageBucket: 'your-project.appspot.com',
  messagingSenderId: '123456789',
  appId: '1:123456789:web:abc123',
});

const messaging = firebase.messaging();

// 백그라운드 메시지 처리
messaging.onBackgroundMessage((payload) => {
  console.log('백그라운드 메시지 수신:', payload);

  const { title, body } = payload.notification;
  const { type, teamRoomId, targetId } = payload.data;

  self.registration.showNotification(title, {
    body,
    icon: '/icon-192x192.png',
    badge: '/badge-72x72.png',
    data: { type, teamRoomId, targetId },
    tag: `${type}-${teamRoomId}`, // 중복 알림 방지
  });
});

// 알림 클릭 처리
self.addEventListener('notificationclick', (event) => {
  event.notification.close();

  const { type, teamRoomId, targetId } = event.notification.data;
  const url = getNavigationUrl(type, teamRoomId, targetId);

  event.waitUntil(
    clients.openWindow(url)
  );
});

function getNavigationUrl(type, teamRoomId, targetId) {
  // 알림 타입에 따른 이동 URL 결정
  switch (type) {
    case 'MEETING_REMIND':
    case 'MEETING_CHANGE':
      return `/team/${teamRoomId}/meetings`;
    case 'RULE_CONFIRM':
    case 'RULE_CHANGE':
      return `/team/${teamRoomId}/rules`;
    case 'TOOL_SUGGESTION':
    case 'TOOL_APPROVED':
    case 'TOOL_REJECTED':
      return `/team/${teamRoomId}/tools`;
    default:
      return `/team/${teamRoomId}`;
  }
}
```

### 3.2 FCM 토큰 등록 API

#### API 스펙

```
POST /api/devices
```

**Request Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body:**
```json
{
  "fcmToken": "string (필수) - Firebase에서 발급받은 FCM 토큰",
  "deviceType": "string (선택) - ANDROID | IOS | WEB",
  "deviceName": "string (선택) - 기기 이름 (예: iPhone 15 Pro)"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "code": 200,
  "message": "Success"
}
```

#### 토큰 발급 및 등록 예시

```javascript
import { getToken } from 'firebase/messaging';
import { messaging } from './firebase-config';
import { apiClient } from './api-client';

// 알림 권한 요청 및 토큰 발급
export async function requestNotificationPermission() {
  try {
    const permission = await Notification.requestPermission();

    if (permission !== 'granted') {
      console.log('알림 권한이 거부되었습니다.');
      return null;
    }

    // FCM 토큰 발급
    const fcmToken = await getToken(messaging, {
      vapidKey: import.meta.env.VITE_FIREBASE_VAPID_KEY,
    });

    if (fcmToken) {
      // 백엔드에 토큰 등록
      await registerDevice(fcmToken);
      return fcmToken;
    }
  } catch (error) {
    console.error('FCM 토큰 발급 실패:', error);
  }
  return null;
}

// 기기 등록 API 호출
async function registerDevice(fcmToken) {
  await apiClient.post('/api/devices', {
    fcmToken,
    deviceType: 'WEB',
    deviceName: getBrowserName(),
  });
}

function getBrowserName() {
  const userAgent = navigator.userAgent;
  if (userAgent.includes('Chrome')) return 'Chrome';
  if (userAgent.includes('Firefox')) return 'Firefox';
  if (userAgent.includes('Safari')) return 'Safari';
  return 'Unknown Browser';
}
```

#### 토큰 갱신 처리

FCM 토큰은 주기적으로 갱신될 수 있으므로, 앱 시작 시 토큰을 확인하고 재등록하는 것을 권장합니다:

```javascript
import { onMessage } from 'firebase/messaging';
import { messaging } from './firebase-config';

// 앱 시작 시 토큰 등록 (로그인 후 호출)
export async function initializeFCM() {
  await requestNotificationPermission();
  setupForegroundMessageHandler();
}

// 포그라운드 메시지 핸들러 설정
function setupForegroundMessageHandler() {
  onMessage(messaging, (payload) => {
    console.log('포그라운드 메시지 수신:', payload);

    const { title, body } = payload.notification;
    const { type, teamRoomId, targetId } = payload.data;

    // 커스텀 UI로 알림 표시 (예: Toast, Snackbar)
    showInAppNotification({ title, body, type, teamRoomId, targetId });
  });
}
```

### 3.3 푸시 알림 수신 처리

#### 포그라운드 메시지 처리

앱이 활성화된 상태에서 메시지 수신 시:

```javascript
function showInAppNotification({ title, body, type, teamRoomId, targetId }) {
  // React 예시: Toast 컴포넌트 사용
  toast({
    title,
    description: body,
    action: (
      <button onClick={() => navigateToTarget(type, teamRoomId, targetId)}>
        확인
      </button>
    ),
  });
}

function navigateToTarget(type, teamRoomId, targetId) {
  // 라우터를 사용하여 해당 페이지로 이동
  switch (type) {
    case 'MEETING_REMIND':
    case 'MEETING_CHANGE':
      router.push(`/team/${teamRoomId}/meetings`);
      break;
    case 'RULE_CONFIRM':
    case 'RULE_CHANGE':
      router.push(`/team/${teamRoomId}/rules`);
      break;
    // ... 기타 타입 처리
    default:
      router.push(`/team/${teamRoomId}`);
  }
}
```

---

## 4. 알림 API

### 4.1 알림 목록 조회

```
GET /api/notifications
```

**Request Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": [
    {
      "notificationId": 1,
      "teamRoomId": 10,
      "targetId": 5,
      "type": "MEETING_REMIND",
      "title": "[프로젝트A] 회의 10분 전 리마인드",
      "message": "프로젝트A 팀의 회의 시간 10분 전입니다! 잊지 말고 회의실로 입장해주세요.",
      "isRead": false,
      "createdAt": "2024-01-15T14:30:00"
    }
  ]
}
```

### 4.2 알림 읽음 처리

#### 단일 알림 읽음 처리

```
PATCH /api/notifications/{notificationId}/read
```

**Request Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": {
    "notificationId": 1,
    "teamRoomId": 10,
    "targetId": 5,
    "type": "MEETING_REMIND",
    "title": "[프로젝트A] 회의 10분 전 리마인드",
    "message": "프로젝트A 팀의 회의 시간 10분 전입니다!",
    "isRead": true,
    "createdAt": "2024-01-15T14:30:00"
  }
}
```

#### 모든 알림 읽음 처리

```
PATCH /api/notifications/read-all
```

**Request Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": 5
}
```

`data`는 읽음 처리된 알림 개수를 반환합니다.

---

## 5. 알림 타입 목록

| 타입 | 설명 | 제목 형식 | 메시지 |
|------|------|-----------|--------|
| `TEAM_JOIN` | 팀원 신규 참여 | [팀룸명] 팀룸 참여 | 새로운 팀원이 참여했습니다. 함께 인사하여 프로젝트를 시작해보세요! |
| `TEAM_LEADER_CONFIRM` | 팀장 선정 완료 | [팀룸명] 팀장 확정 알림 | {팀룸명} 팀의 리더가 최종 확정되었습니다. |
| `TEAM_LEADER_CHANGE` | 팀장 변경 | [팀룸명] 팀장 변경 알림 | {팀룸명} 팀의 리더가 변경되었습니다. |
| `TEAM_DEADLINE_REMIND` | 마감 D-1 리마인드 | [팀룸명] 팀룸 마감 D-1 | 마감까지 단 하루! 마지막으로 놓친 부분은 없는지 점검해보세요. |
| `TEAM_ARCHIVED` | 팀룸 아카이빙 | [팀룸명] 팀룸 아카이빙 알림 | {팀룸명} 팀의 프로젝트가 종료되어 아카이빙되었습니다. 그동안의 기록을 추억해보세요. |
| `RULE_CONFIRM` | 규칙 확정 | [팀룸명] 규칙 확정 안내 | {팀룸명} 팀의 기본 규칙이 최종 확정되었습니다. |
| `RULE_CHANGE` | 규칙 변경 | [팀룸명] 규칙 변경 알림 | {팀룸명} 팀의 규칙이 업데이트되었습니다. 변경된 내용을 확인하고 피드백을 남겨주세요! |
| `MEETING_CHANGE` | 회의 일정 변경 | [팀룸명] 회의 변경 알림 | {팀룸명} 팀의 회의 일정이 변경되었습니다. 캘린더를 확인하여 스케줄을 조정해주세요. |
| `MEETING_REMIND` | 회의 10분 전 리마인드 | [팀룸명] 회의 10분 전 리마인드 | {팀룸명} 팀의 회의 시간 10분 전입니다! 잊지 말고 회의실로 입장해주세요. |
| `TOOL_SUGGESTION` | 협업 툴 제안 (팀장에게) | [팀룸명] 협업 툴 제안 | 새로운 협업 툴 제안이 도착했습니다. 확인 후 승인 여부를 결정해주세요. |
| `TOOL_APPROVED` | 제안 승인 (팀원 전원) | [팀룸명] 제안 승인 알림 | 제안하신 협업 툴이 승인되었습니다. 이제 팀룸에서 함께 사용할 수 있습니다! |
| `TOOL_REJECTED` | 제안 반려 (제안자에게) | [팀룸명] 제안 반려 알림 | 제안하신 협업 툴이 반려되었습니다. 대시보드에서 상세 내용을 확인해주세요. |

---

## 6. 푸시 메시지 데이터 구조

FCM에서 수신되는 메시지 구조:

```json
{
  "notification": {
    "title": "[프로젝트A] 회의 10분 전 리마인드",
    "body": "프로젝트A 팀의 회의 시간 10분 전입니다! 잊지 말고 회의실로 입장해주세요."
  },
  "data": {
    "type": "MEETING_REMIND",
    "teamRoomId": "10",
    "targetId": "5"
  }
}
```

### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `notification.title` | string | 알림 제목 (팀룸명 포함) |
| `notification.body` | string | 알림 본문 메시지 |
| `data.type` | string | NotificationType enum 값 |
| `data.teamRoomId` | string | 관련 팀룸 ID |
| `data.targetId` | string? | 대상 ID (규칙 ID, 회의 ID 등) - 선택적 |

> **참고**: `data` 필드의 모든 값은 문자열로 전달됩니다. 숫자로 사용하려면 파싱이 필요합니다.

---

## 7. 체크리스트

프론트엔드 통합 시 확인해야 할 항목:

### 초기 설정
- [ ] Firebase 프로젝트 생성 및 웹 앱 등록
- [ ] VAPID 키 발급 (웹의 경우)
- [ ] 환경변수 설정
- [ ] Service Worker 파일 생성 (`firebase-messaging-sw.js`)

### 기능 구현
- [ ] Firebase SDK 초기화
- [ ] 알림 권한 요청 UI
- [ ] FCM 토큰 발급 및 백엔드 등록
- [ ] 포그라운드 메시지 핸들러
- [ ] 백그라운드 메시지 핸들러
- [ ] 알림 클릭 시 페이지 이동

### 알림 센터 UI
- [ ] 알림 목록 조회 (`GET /api/notifications`)
- [ ] 알림 읽음 처리 (`PATCH /api/notifications/{id}/read`)
- [ ] 모든 알림 읽음 처리 (`PATCH /api/notifications/read-all`)
- [ ] 읽지 않은 알림 개수 표시

### 테스트
- [ ] 로컬 환경에서 푸시 알림 수신 테스트
- [ ] 포그라운드/백그라운드 동작 확인
- [ ] 알림 클릭 시 올바른 페이지로 이동 확인
- [ ] 여러 기기에서 동일 계정 로그인 시 동작 확인

### 에러 처리
- [ ] 알림 권한 거부 시 안내 메시지
- [ ] 토큰 발급 실패 시 재시도 로직
- [ ] 오프라인 상태 처리

---

## 8. FAQ

### Q: FCM 토큰은 언제 갱신해야 하나요?
A: 앱 시작 시(로그인 후)마다 토큰을 발급받아 등록하면 됩니다. 백엔드에서 동일 토큰이 이미 등록되어 있으면 마지막 로그인 시간만 업데이트합니다.

### Q: 여러 기기에서 로그인하면 어떻게 되나요?
A: 각 기기의 FCM 토큰이 별도로 저장되어, 모든 기기에서 푸시 알림을 수신할 수 있습니다.

### Q: 알림을 끄려면 어떻게 해야 하나요?
A: 사용자 프로필의 `isAlarmOn` 설정을 `false`로 변경하면 FCM 푸시가 발송되지 않습니다. 단, 알림 목록(DB)에는 계속 저장됩니다.

### Q: Safari에서 웹 푸시가 작동하나요?
A: Safari 16.4+ (iOS 16.4+, macOS Ventura+)부터 Web Push를 지원합니다. 그 이전 버전에서는 작동하지 않습니다.
