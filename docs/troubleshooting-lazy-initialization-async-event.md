# LazyInitializationException in Async Event Listener

## 문제 상황

`TOOL_SUGGESTION` 알림 발송 시 `LazyInitializationException` 발생으로 알림이 전송되지 않음.

### 에러 로그

```
org.hibernate.LazyInitializationException: could not initialize proxy - no Session
    at org.hibernate.proxy.AbstractLazyInitializer.initialize(...)
    at com.yamoyo.be.event.listener.NotificationEventListener.handleNotificationEvent(...)
```

---

## 원인 분석

### 1. 비동기 이벤트 리스너 환경

```java
@Async("notificationExecutor")  // 별도 스레드에서 실행
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)  // 트랜잭션 종료 후 실행
public void handleNotificationEvent(NotificationEvent event) { ... }
```

- `@Async`: 별도 스레드 풀에서 실행
- `TransactionPhase.AFTER_COMMIT`: 원본 트랜잭션 커밋 후 실행

→ **Hibernate 세션이 없는 상태**에서 이벤트 리스너가 실행됨

### 2. Lazy 프록시 반환

```java
// TeamMemberRepository.java - fetch join 없음
Optional<TeamMember> findByTeamRoomIdAndTeamRole(Long teamRoomId, TeamRole teamRole);
```

- Spring Data JPA의 메서드 이름 기반 쿼리 생성
- `User` 연관관계가 Lazy Loading으로 설정됨
- 반환된 `TeamMember.user`는 **초기화되지 않은 프록시**

### 3. 프록시 초기화 시도

```java
// NotificationEventListener.java:143-146
case TOOL_SUGGESTION -> {
    yield teamMemberRepository
        .findByTeamRoomIdAndTeamRole(event.teamRoomId(), TeamRole.LEADER)
        .map(leader -> List.of(leader.getUser()))  // User는 프록시
        .orElse(Collections.emptyList());
}

// NotificationEventListener.java:97
if (user.isAlarmOn()) {  // 세션 없음 → LazyInitializationException
```

---

## 해결 방안

### Fetch Join 메서드 추가

**TeamMemberRepository.java**

```java
/**
 * 특정 역할 조회 (User Fetch Join)
 * - 비동기 이벤트 리스너에서 사용 (Hibernate 세션 없는 환경)
 */
@Query("""
    SELECT tm
    FROM TeamMember tm
    JOIN FETCH tm.user
    WHERE tm.teamRoom.id = :teamRoomId
      AND tm.teamRole = :teamRole
    """)
Optional<TeamMember> findByTeamRoomIdAndTeamRoleWithUser(
    @Param("teamRoomId") Long teamRoomId,
    @Param("teamRole") TeamRole teamRole
);
```

### 이벤트 리스너 수정

**NotificationEventListener.java**

```java
case TOOL_SUGGESTION -> {
    // LEADER에게만 알림 (비동기 환경에서 User Fetch Join 필요)
    yield teamMemberRepository
            .findByTeamRoomIdAndTeamRoleWithUser(event.teamRoomId(), TeamRole.LEADER)
            .map(leader -> List.of(leader.getUser()))
            .orElse(Collections.emptyList());
}
```

---

## 핵심 원리

| 구분 | 기존 메서드 | 새 메서드 |
|------|------------|----------|
| 쿼리 | `SELECT tm FROM TeamMember tm WHERE ...` | `SELECT tm FROM TeamMember tm JOIN FETCH tm.user WHERE ...` |
| User 로딩 | Lazy (프록시) | Eager (즉시 로딩) |
| 비동기 환경 | LazyInitializationException | 정상 동작 |

---

## 일반화된 가이드라인

### 비동기 이벤트 리스너에서 엔티티 사용 시

1. **연관 엔티티 접근이 필요하면 Fetch Join 사용**
   - `@Async` + `@TransactionalEventListener`는 세션이 없음
   - 프록시가 아닌 실제 객체가 필요

2. **명명 규칙**
   - `findByXXXWithUser()`: User Fetch Join
   - `findByXXXWithTeamRoom()`: TeamRoom Fetch Join

3. **대안적 접근**
   - ID만 전달하고 리스너에서 새로 조회 (새 트랜잭션)
   - DTO 프로젝션 사용

---

## 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `TeamMemberRepository.java` | `findByTeamRoomIdAndTeamRoleWithUser()` 메서드 추가 |
| `NotificationEventListener.java` | `TOOL_SUGGESTION` 케이스에서 새 메서드 사용 |

---

## 검증

```bash
./gradlew test
```

### 시나리오 테스트

1. MEMBER가 협업 도구 제안
2. LEADER에게 알림 전송 확인
3. 예외 없이 FCM 발송 확인
