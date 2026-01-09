package com.yamoyo.be.oauth.domain.security.exception;

/**
 * Refresh Token 관련 예외
 *
 * Role:
 * - Refresh Token 만료, 존재하지 않음, 불일치 등을 처리하는 커스텀 예외
 */
public class RefreshTokenException extends RuntimeException {

    public RefreshTokenException(String message) {
        super(message);
    }

    public RefreshTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
