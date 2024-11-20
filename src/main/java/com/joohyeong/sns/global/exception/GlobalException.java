package com.joohyeong.sns.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class GlobalException extends RuntimeException {

    private final ErrorCodeType errorCodeType;

    public GlobalException(ErrorCodeType errorCodeType) {
        super(errorCodeType.message());
        this.errorCodeType = errorCodeType;
    }

    public HttpStatus httpStatus() {
        return errorCodeType.httpStatus();
    }

    public String message() {
        return errorCodeType.message();
    }

    public String errorCode() {
        return errorCodeType.errorCode();
    }

}
