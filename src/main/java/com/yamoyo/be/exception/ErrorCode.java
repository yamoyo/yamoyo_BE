package com.yamoyo.be.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // Onboarding
    TERMS_NOT_FOUND(HttpStatus.NOT_FOUND, "약관을 찾을 수 없습니다."),
    MANDATORY_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "필수 약관에 모두 동의해야 합니다."),
    TERMS_NOT_AGREED(HttpStatus.FORBIDDEN, "약관에 동의하지 않아 프로필 설정을 진행할 수 없습니다."),

    // Auth
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    // Timepick
    TIMEPICK_NOT_FOUND(HttpStatus.NOT_FOUND, "타임픽을 찾을 수 없습니다."),
    TIMEPICK_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "타임픽 참가자가 아닙니다."),

    // Server
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다.");


    private final HttpStatus httpStatus;
    private final String message;
}
