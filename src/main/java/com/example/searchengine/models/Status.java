package com.example.searchengine.models;

public enum Status {
    PENDING("PENDING"),
    INDEXING("INDEXING"),
    INDEXED("INDEXED"),
    FAILED("FAILED"),
    NOT_FOUND("NOT_FOUND"),
    STOPPED("STOPPED"),
    SERVER_ERROR("SERVER_ERROR"),
    DELETED("DELETED"),
    SAVED("SAVED");



    private final String status;

    Status(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return status;
    }
}