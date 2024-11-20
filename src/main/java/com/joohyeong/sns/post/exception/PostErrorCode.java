package com.joohyeong.sns.post.exception;

import com.joohyeong.sns.global.exception.ErrorCodeType;
import org.springframework.http.HttpStatus;

public enum PostErrorCode implements ErrorCodeType {

    NOT_FOUND_POST(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    PostErrorCode(HttpStatus httpStatus, String message) {
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