package com.example.searchengine.models;

import lombok.Getter;

@Getter
public enum SessionStatus {
    RUNNING("RUNNING"),
    COMPLETED("COMPLETED"),
    STOPPED("STOPPED"),
    FAILED("FAILED");

    private final String status;

    SessionStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return status;
    }
}
