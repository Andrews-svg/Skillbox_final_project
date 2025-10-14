package com.example.searchengine.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.example.searchengine.indexing.IndexingHistoryRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@Transactional
public class IndexingHistoryService {

    @PersistenceContext
    private EntityManager entityManager;

    private final Map<UUID, List<IndexingHistoryRecord>> sessionHistories =
            new ConcurrentHashMap<>();

    public UUID startIndexingSession() {
        UUID sessionId = UUID.randomUUID();
        sessionHistories.put(sessionId, new ArrayList<>());
        return sessionId;
    }

    public void addRecord(UUID sessionId, IndexingHistoryRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Record must not be null");
        }
        List<IndexingHistoryRecord> history = sessionHistories.get(sessionId);
        if (history != null) {
            history.add(record);
        } else {
            throw new IllegalArgumentException(
                    "No indexing session found for sessionId: " + sessionId);
        }
    }

    public List<IndexingHistoryRecord> getHistory(UUID sessionId) {
        return new ArrayList<>(sessionHistories.getOrDefault(sessionId,
                new ArrayList<>()));
    }

    @Transactional
    public void completeIndexingSession(UUID sessionId, boolean saveHistory) {
        if (saveHistory) {
            List<IndexingHistoryRecord> history = sessionHistories.get(sessionId);
            if (history != null) {
                for (IndexingHistoryRecord record : history) {
                    try {
                        entityManager.persist(record);
                    } catch (Exception e) {
                        throw new RuntimeException(
                                "Error persisting record: " + record, e);
                    }
                }
            } else {
                throw new IllegalArgumentException(
                        "No indexing session found for sessionId: " + sessionId);
            }
        }
        clearHistory(sessionId);
    }

    private void clearHistory(UUID sessionId) {
        sessionHistories.remove(sessionId);
    }
}