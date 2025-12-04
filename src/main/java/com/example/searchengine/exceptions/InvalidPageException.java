package com.example.searchengine.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPageException extends RuntimeException {

    public InvalidPageException(String message) {
        super(message);
    }

    public InvalidPageException(String message, Throwable cause) {
        super(message, cause);
    }
}