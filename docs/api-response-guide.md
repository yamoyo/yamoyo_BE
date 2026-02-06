# API Response & Exception 가이드

이 문서는 프로젝트의 통일된 API 응답 형식과 예외 처리 방법을 설명합니다.

---

## 목차

1. [API 응답 구조](#api-응답-구조)
2. [ApiResponse 사용법](#apiresponse-사용법)
3. [예외 처리](#예외-처리)
4. [ErrorCode 추가하기](#errorcode-추가하기)
5. [전체 사용 예시](#전체-사용-예시)

---

## API 응답 구조

모든 API 응답은 다음과 같은 통일된 형식을 따릅니다:

```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": { ... }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | boolean | 요청 성공 여부 |
| `code` | int | HTTP 상태 코드 |
| `message` | string | 응답 메시지 |
| `data` | T (제네릭) | 응답 데이터 (없으면 생략됨) |

---

## ApiResponse 사용법

### 성공 응답

```java
// 데이터와 함께 성공 응답
@GetMapping("/users/{id}")
public ApiResponse<UserDto> getUser(@PathVariable Long id) {
    UserDto userDto = userService.findById(id);
    return ApiResponse.success(userDto);
}

// 데이터 없이 성공 응답 (생성, 수정, 삭제 등)
@DeleteMapping("/users/{id}")
public ApiResponse<Void> deleteUser(@PathVariable Long id) {
    userService.delete(id);
    return ApiResponse.success();
}
```

### 응답 예시

**성공 (데이터 있음)**
```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "name": "홍길동",
    "email": "hong@example.com"
  }
}
```

**성공 (데이터 없음)**
```json
{
  "success": true,
  "code": 200,
  "message": "Success"
}
```

---

## 예외 처리

### YamoyoException 사용법

비즈니스 로직에서 예외가 발생하면 `YamoyoException`을 던지세요.

```java
// 기본 사용법
@Service
public class UserService {

    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));
    }
}
```

### 상세 정보와 함께 예외 던지기

추가 정보가 필요한 경우 `details` Map을 함께 전달할 수 있습니다.

```java
// 상세 정보 포함
Map<String, Object> details = new HashMap<>();
details.put("requestedId", id);
details.put("availableIds", List.of(1, 2, 3));

throw new YamoyoException(ErrorCode.USER_NOT_FOUND, details);
```

### 원인 예외와 함께 던지기 (Exception Chaining)

외부 시스템이나 라이브러리에서 발생한 예외를 비즈니스 예외로 변환할 때 사용합니다.

```java
public YamoyoException(ErrorCode errorCode, Throwable cause)
```

#### 언제 사용하나요?

| 상황 | 설명 |
|------|------|
| **외부 API 호출 실패** | RestTemplate, WebClient 등에서 발생한 예외를 감싸서 던질 때 |
| **데이터베이스 오류** | JPA, JDBC 등에서 발생한 예외를 비즈니스 예외로 변환할 때 |
| **파일 I/O 오류** | IOException 등을 감싸서 던질 때 |
| **파싱/변환 오류** | JSON 파싱, 날짜 변환 등에서 발생한 예외를 감쌀 때 |

#### 왜 사용해야 하나요?

1. **근본 원인 추적**: 로그에서 원래 어떤 예외가 발생했는지 스택 트레이스로 확인 가능
2. **디버깅 용이성**: `cause`가 없으면 실제 오류 원인을 파악하기 어려움
3. **예외 체이닝**: Java의 표준 예외 처리 패턴으로, 예외의 인과관계를 명확히 표현

#### 사용 예시

```java
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ExternalPaymentClient paymentClient;

    public PaymentResult processPayment(PaymentRequest request) {
        try {
            return paymentClient.requestPayment(request);
        } catch (PaymentGatewayException e) {
            // 외부 결제 API 예외를 비즈니스 예외로 변환
            // cause를 전달하여 원본 예외 정보 보존
            throw new YamoyoException(ErrorCode.PAYMENT_FAILED, e);
        }
    }
}
```

```java
@Service
public class FileService {

    public byte[] readFile(String path) {
        try {
            return Files.readAllBytes(Path.of(path));
        } catch (IOException e) {
            // 파일 I/O 예외를 비즈니스 예외로 변환
            throw new YamoyoException(ErrorCode.FILE_READ_ERROR, e);
        }
    }
}
```

```java
@Service
public class UserService {

    public User parseUserJson(String json) {
        try {
            return objectMapper.readValue(json, User.class);
        } catch (JsonProcessingException e) {
            // JSON 파싱 예외를 비즈니스 예외로 변환
            throw new YamoyoException(ErrorCode.INVALID_INPUT, e);
        }
    }
}
```

#### 로그 출력 예시

`cause`를 전달하면 로그에 다음과 같이 원인 예외까지 함께 출력됩니다:

```
[Exception] Code: PAYMENT_FAILED, Message: 결제 처리에 실패했습니다.
com.yamoyo.be.exception.YamoyoException: 결제 처리에 실패했습니다.
    at com.yamoyo.be.service.PaymentService.processPayment(PaymentService.java:25)
    ...
Caused by: com.example.payment.PaymentGatewayException: Connection timeout
    at com.example.payment.PaymentClient.request(PaymentClient.java:42)
    ...
```

> **Tip**: `cause` 없이 예외를 던지면 "Caused by" 부분이 출력되지 않아 실제 오류 원인을 파악하기 어렵습니다.

### 실패 응답 예시

**기본 실패 응답**
```json
{
  "success": false,
  "code": 404,
  "message": "사용자를 찾을 수 없습니다."
}
```

**상세 정보 포함 실패 응답**
```json
{
  "success": false,
  "code": 404,
  "message": "사용자를 찾을 수 없습니다.",
  "data": {
    "requestedId": 999,
    "availableIds": [1, 2, 3]
  }
}
```

---

## ErrorCode 추가하기

새로운 에러 코드가 필요하면 `ErrorCode` enum에 추가하세요.

### 파일 위치
`src/main/java/com/yamoyo/be/exception/ErrorCode.java`

### 추가 방법

```java
@Getter
@AllArgsConstructor
public enum ErrorCode {
    // ===== User =====
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 사용자입니다."),

    // ===== Auth =====
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    // ===== Validation =====
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "필수 항목이 누락되었습니다."),

    // ===== Resource =====
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "중복된 리소스입니다."),

    // ===== Server =====
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
```

### 네이밍 규칙

- **대문자 + 언더스코어** 형식 사용 (예: `USER_NOT_FOUND`)
- **도메인별로 그룹화**하여 주석으로 구분
- **메시지는 한글**로 사용자 친화적으로 작성

### 자주 사용하는 HTTP 상태 코드

| 코드 | HttpStatus | 용도 |
|------|------------|------|
| 400 | `BAD_REQUEST` | 잘못된 요청 (유효성 검증 실패) |
| 401 | `UNAUTHORIZED` | 인증 필요 |
| 403 | `FORBIDDEN` | 권한 없음 |
| 404 | `NOT_FOUND` | 리소스 없음 |
| 409 | `CONFLICT` | 중복/충돌 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 오류 |

---

## 전체 사용 예시

### Controller

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ApiResponse<UserDto> getUser(@PathVariable Long id) {
        UserDto userDto = userService.findById(id);
        return ApiResponse.success(userDto);
    }

    @PostMapping
    public ApiResponse<UserDto> createUser(@RequestBody @Valid CreateUserRequest request) {
        UserDto userDto = userService.create(request);
        return ApiResponse.success(userDto);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success();
    }
}
```

### Service

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserDto findById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));
        return UserDto.from(user);
    }

    public UserDto create(CreateUserRequest request) {
        // 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new YamoyoException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = User.builder()
            .email(request.getEmail())
            .name(request.getName())
            .build();

        return UserDto.from(userRepository.save(user));
    }

    public void delete(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));
        userRepository.delete(user);
    }
}
```

---

## 참고 사항

### GlobalExceptionHandler 동작 방식

- `YamoyoException` 발생 시: ErrorCode에 정의된 상태 코드와 메시지로 응답
- 그 외 예외 발생 시: 보안을 위해 `500 Internal Server Error`로 통일 (상세 메시지 노출 X)
- 모든 예외는 로그에 스택 트레이스와 함께 기록됨

### 파일 구조

```
src/main/java/com/yamoyo/be/
├── common/
│   └── dto/
│       └── ApiResponse.java      # 통일된 API 응답 형식
└── exception/
    ├── ErrorCode.java            # 에러 코드 정의
    ├── YamoyoException.java      # 커스텀 예외 클래스
    └── GlobalExceptionHandler.java  # 전역 예외 처리
```