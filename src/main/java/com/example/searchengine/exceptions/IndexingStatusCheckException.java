package com.example.searchengine.exceptions;

public class IndexingStatusCheckException extends RuntimeException {
    public IndexingStatusCheckException(String message) {
        super(message);
    }
}