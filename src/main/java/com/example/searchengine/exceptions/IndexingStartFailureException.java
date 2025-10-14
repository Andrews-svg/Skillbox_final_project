package com.example.searchengine.exceptions;

public class IndexingStartFailureException extends RuntimeException {
    public IndexingStartFailureException(String message) {
        super(message);
    }
}