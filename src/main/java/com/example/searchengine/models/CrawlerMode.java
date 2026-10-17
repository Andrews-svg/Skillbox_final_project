package com.example.searchengine.models;

import lombok.Getter;

@Getter
public enum CrawlerMode {
    SINGLE("SINGLE"),
    MULTI("MULTI");

    private final String mode;

    CrawlerMode(String mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return mode;
    }
}
