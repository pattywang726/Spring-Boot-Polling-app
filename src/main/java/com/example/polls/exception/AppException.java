package com.example.polls.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus
public class AppException extends RuntimeException{
    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
