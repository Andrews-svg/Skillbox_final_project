package com.example.searchengine.services.indexing;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class IndexingState {

    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);

    // ===========================================
    // 🟢 LIVE STATE СЕССИИ ИНДЕКСАЦИИ
    // ===========================================
    private final AtomicLong sessionStartTime = new AtomicLong(0);
    private final Set<Long> sessionSiteIds = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Long, AtomicLong> sessionPagesBySite = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicLong> sessionLemmasBySite = new ConcurrentHashMap<>();

    // ===========================================
    // ГЛОБАЛЬНЫЙ ФЛАГ (индексация идёт?)
    // ===========================================
    public boolean isActive() {
        return indexingInProgress.get();
    }

    public void setActive(boolean active) {
        indexingInProgress.set(active);
    }

    // ===========================================
    // СЕССИЯ: СТАРТ / ОБНУЛЕНИЕ
    // ===========================================
    public void startSession() {
        sessionStartTime.set(System.currentTimeMillis());
        sessionSiteIds.clear();
        sessionPagesBySite.clear();
        sessionLemmasBySite.clear();
    }

    public void addSessionSite(Long siteId) {
        sessionSiteIds.add(siteId);
        sessionPagesBySite.putIfAbsent(siteId, new AtomicLong(0));
        sessionLemmasBySite.putIfAbsent(siteId, new AtomicLong(0));
    }

    // ===========================================
    // ИНКРЕМЕНТЫ
    // ===========================================
    public void incrementPages(Long siteId, long delta) {
        sessionPagesBySite
                .computeIfAbsent(siteId, k -> new AtomicLong(0))
                .addAndGet(delta);
    }

    public void incrementLemmas(Long siteId, long delta) {
        sessionLemmasBySite
                .computeIfAbsent(siteId, k -> new AtomicLong(0))
                .addAndGet(delta);
    }

    // ===========================================
    // ГЕТТЕРЫ
    // ===========================================
    public long getSessionStartTime() {
        return sessionStartTime.get();
    }

    public Set<Long> getSessionSiteIds() {
        return sessionSiteIds;
    }

    public long getSessionPages(Long siteId) {
        AtomicLong counter = sessionPagesBySite.get(siteId);
        return counter != null ? counter.get() : 0;
    }

    public long getSessionLemmas(Long siteId) {
        AtomicLong counter = sessionLemmasBySite.get(siteId);
        return counter != null ? counter.get() : 0;
    }

    public long getTotalPages() {
        return sessionPagesBySite.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
    }

    public long getTotalLemmas() {
        return sessionLemmasBySite.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
    }
}