package com.joohyeong.sns.global.exception;

import org.springframework.http.HttpStatus;

public enum GlobalErrorCode implements ErrorCodeType {

    JSON_PARSING_FAILED(HttpStatus.BAD_REQUEST, "JSON 파싱에 실패하였습니다."),
    KAFKA_SEND_FAILED(HttpStatus.BAD_REQUEST, "JSON 파싱에 실패하였습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    GlobalErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }


    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public String errorCode() {
        return name();
    }

}