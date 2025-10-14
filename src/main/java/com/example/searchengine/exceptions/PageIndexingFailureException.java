package com.example.searchengine.exceptions;

public class PageIndexingFailureException extends RuntimeException {
    public PageIndexingFailureException(String message) {
        super(message);
    }
}