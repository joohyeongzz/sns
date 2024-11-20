package com.joohyeong.sns.user.exception;

import com.joohyeong.sns.global.exception.ErrorCodeType;
import org.springframework.http.HttpStatus;

public enum UserErrorCode implements ErrorCodeType {

    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    EXISTS_EMAIL(HttpStatus.NOT_FOUND, "중복된 이메일입니다."),
    NEO4J_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Neo4j 저장 실패로 인해 작업이 취소되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    UserErrorCode(HttpStatus httpStatus, String message) {
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