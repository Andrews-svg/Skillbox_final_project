package com.example.searchengine.exceptions;

public class InvalidSiteException extends Exception {

    public InvalidSiteException(String message) {
        super(message);
    }

    public InvalidSiteException(String message, Throwable cause) {
        super(message, cause);
    }
}