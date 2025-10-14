package com.example.searchengine.exceptions;

public class IndexingAllSitesFailureException extends RuntimeException {
    public IndexingAllSitesFailureException(String message) {
        super(message);
    }
}