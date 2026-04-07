package com.example.searchengine.services.indexing;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class IndexingState {
    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);

    public boolean isActive() { return indexingInProgress.get(); }
    public void setActive(boolean active) { indexingInProgress.set(active); }
}