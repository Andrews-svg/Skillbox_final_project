package com.example.searchengine.exceptions;

public class IndexingStopFailureException extends RuntimeException {
    public IndexingStopFailureException(String message) {
        super(message);
    }
}