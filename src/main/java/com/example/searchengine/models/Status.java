package com.example.searchengine.models;

import lombok.Getter;

@Getter
public enum Status {
    INDEXING("INDEXING"),
    INDEXED("INDEXED"),
    FAILED("FAILED");




    private final String status;

    Status(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return status;
    }
}