package com.joohyeong.sns.global.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCodeType {

    HttpStatus httpStatus();

    String message();

    String errorCode();

}
