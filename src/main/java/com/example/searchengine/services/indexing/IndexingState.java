package com.example.searchengine.services.indexing;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class IndexingState {

    // ===========================================
    // ГЛОБАЛЬНЫЙ ФЛАГ ИНДЕКСАЦИИ (ref-count + stop)
    // ===========================================
    private final AtomicInteger activeCount = new AtomicInteger(0);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    private final ConcurrentHashMap<Long, AtomicLong> sessionErrorsBySite = new ConcurrentHashMap<>();

    // ===========================================
    // 🟢 LIVE STATE СЕССИИ ИНДЕКСАЦИИ
    // ===========================================
    private final AtomicLong sessionStartTime = new AtomicLong(0);
    private final Set<Long> sessionSiteIds = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Long, AtomicLong> sessionPagesBySite = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicLong> sessionLemmasBySite = new ConcurrentHashMap<>();

    // ===========================================
    // ФЛАГ: ИНДЕКСАЦИЯ АКТИВНА?
    // false если явно запрошен стоп, ИЛИ нет ни одной активной операции.
    // ===========================================
    public boolean isActive() {
        return !stopRequested.get() && activeCount.get() > 0;
    }

    // ===========================================
    // REF-COUNT: СТАРТ / ФИНИШ ОПЕРАЦИИ (full или page)
    // Безусловная пара — без wasActive. Инкремент/декремент всегда.
    // ===========================================
    public void startIndexing() {
        activeCount.incrementAndGet();
    }

    public void endIndexing() {
        activeCount.updateAndGet(c -> Math.max(0, c - 1));
    }

    // ===========================================
    // ЯВНЫЙ СТОП: новые/текущие операции увидят isActive()=false
    // ===========================================
    public void requestStop() {
        stopRequested.set(true);
    }

    // ===========================================
    // СБРОС: обнулить счётчик и снять stopRequested.
    // Используется после stopAllCrawling (пулы дожданы → воркеров нет)
    // и перед startFullIndexing (страховка от залипания).
    // ===========================================
    public void reset() {
        activeCount.set(0);
        stopRequested.set(false);
    }

    // ===========================================
    // СЕССИЯ: СТАРТ / ОБНУЛЕНИЕ
    // ===========================================
    public void startSession() {
        sessionStartTime.set(System.currentTimeMillis());
        sessionSiteIds.clear();
        sessionPagesBySite.clear();
        sessionLemmasBySite.clear();
        sessionErrorsBySite.clear();
    }

    public void addSessionSite(Long siteId) {
        sessionSiteIds.add(siteId);
        sessionPagesBySite.putIfAbsent(siteId, new AtomicLong(0));
        sessionLemmasBySite.putIfAbsent(siteId, new AtomicLong(0));
        sessionErrorsBySite.putIfAbsent(siteId, new AtomicLong(0));
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

    public void incrementErrors(Long siteId, long delta) {
        sessionErrorsBySite
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

    public long getSessionErrors(Long siteId) {
        AtomicLong counter = sessionErrorsBySite.get(siteId);
        return counter != null ? counter.get() : 0;
    }

    public long getTotalErrors() {
        return sessionErrorsBySite.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
    }
}