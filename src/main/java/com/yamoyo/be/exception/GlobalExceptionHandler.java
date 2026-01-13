package com.yamoyo.be.exception;

import com.yamoyo.be.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(YamoyoException.class)
    public ApiResponse<Object> handleException(YamoyoException e) {
        // 스택 트레이스 남기기
        log.error("[Exception🚨] Code: {}, Message: {}", e.getErrorCode(), e.getMessage(), e);

        // details 가 비어있으면 데이터 없는 ApiResponse.fail(errorCode) 호출
        if(e.getDetails() == null || e.getDetails().isEmpty()) {
            return ApiResponse.fail(e.getErrorCode());
        }
        // details 가 있으면 데이터 있는 ApiResponse.fail(errorCode, details) 호출
        return ApiResponse.fail(e.getErrorCode(), e.getDetails());
    }

    // 미처 잡지 못한 예외 처리
    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> handleException(Exception e) {
        // 스택 트레이스 남기기
        log.error("[Exception🚨] Message: {}", e.getMessage(), e);

        // 보안 이슈로 에러 메시지 그대로 보내지 않고 500 에러로 처리
        return ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR);
    }



}
