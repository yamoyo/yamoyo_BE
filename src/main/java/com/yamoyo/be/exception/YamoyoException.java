package com.yamoyo.be.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class YamoyoException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public YamoyoException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    public YamoyoException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details;
    }

    public YamoyoException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }
}
