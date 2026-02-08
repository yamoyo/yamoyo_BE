package com.yamoyo.be.exception;

import com.yamoyo.be.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // Validation은 의도된 4xx인 경우가 대부분이라 스택트레이스 없이 요약만 남긴다.
        log.warn(
                "[Validation] method={}, uri={}, errors={}",
                request.getMethod(),
                request.getRequestURI(),
                errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.BAD_REQUEST, errors));
    }

    @ExceptionHandler(YamoyoException.class)
    public ResponseEntity<ApiResponse<Object>> handleYamoyoException(
            YamoyoException e,
            HttpServletRequest request
    ) {
        HttpStatus status = e.getErrorCode().getHttpStatus();
        boolean hasCause = e.getCause() != null;

        // "의도된" 비즈니스 예외(주로 4xx)는 스택트레이스 없이 코드/메시지만 남긴다.
        // 5xx이거나 cause가 달린 경우는 디버깅을 위해 스택트레이스를 남긴다.
        if (status.is5xxServerError() || hasCause) {
            log.error(
                    "[YamoyoException] method={}, uri={}, status={}, code={}, message={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    status.value(),
                    e.getErrorCode(),
                    e.getMessage(),
                    e
            );
        } else {
            log.warn(
                    "[YamoyoException] method={}, uri={}, status={}, code={}, message={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    status.value(),
                    e.getErrorCode(),
                    e.getMessage()
            );
        }

        if (e.getDetails() == null || e.getDetails().isEmpty()) {
            return ResponseEntity
                    .status(status)
                    .body(ApiResponse.fail(e.getErrorCode()));
        }
        return ResponseEntity
                .status(status)
                .body(ApiResponse.fail(e.getErrorCode(), e.getDetails()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnhandledException(
            Exception e,
            HttpServletRequest request
    ) {
        log.error(
                "[UnhandledException] method={}, uri={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                e.getMessage(),
                e
        );

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }



}
