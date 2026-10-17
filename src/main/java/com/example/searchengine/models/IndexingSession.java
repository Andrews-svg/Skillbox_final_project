package com.example.searchengine.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "indexing_session")
public class IndexingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrawlerMode mode;

    @Column(name = "total_sites", nullable = false)
    private Integer totalSites = 0;

    @Column(name = "total_pages", nullable = false)
    private Integer totalPages = 0;

    @Column(name = "total_lemmas", nullable = false)
    private Integer totalLemmas = 0;

    @Column(name = "total_errors", nullable = false)
    private Integer totalErrors = 0;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "started_by", length = 100)
    private String startedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public IndexingSession() {}

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // ===========================================
    // ГЕТТЕРЫ И СЕТТЕРЫ
    // ===========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public CrawlerMode getMode() {
        return mode;
    }

    public void setMode(CrawlerMode mode) {
        this.mode = mode;
    }

    public Integer getTotalSites() {
        return totalSites;
    }

    public void setTotalSites(Integer totalSites) {
        this.totalSites = totalSites;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Integer getTotalLemmas() {
        return totalLemmas;
    }

    public void setTotalLemmas(Integer totalLemmas) {
        this.totalLemmas = totalLemmas;
    }

    public Integer getTotalErrors() {
        return totalErrors;
    }

    public void setTotalErrors(Integer totalErrors) {
        this.totalErrors = totalErrors;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public String getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(String startedBy) {
        this.startedBy = startedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}