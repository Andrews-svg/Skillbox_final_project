package com.example.searchengine.services.indexing;

import com.example.searchengine.models.CrawlerMode;
import com.example.searchengine.models.IndexingSession;
import com.example.searchengine.models.SessionStatus;
import com.example.searchengine.repositories.IndexingSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessionHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(SessionHistoryService.class);

    private final IndexingSessionRepository sessionRepository;

    public SessionHistoryService(IndexingSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public IndexingSession saveSession(LocalDateTime startedAt,
                                       LocalDateTime endedAt,
                                       CrawlerMode mode,
                                       int totalSites,
                                       int totalPages,
                                       int totalLemmas,
                                       int totalErrors,
                                       int durationSeconds,
                                       SessionStatus status,
                                       String startedBy) {
        IndexingSession session = new IndexingSession();
        session.setStartedAt(startedAt);
        session.setEndedAt(endedAt);
        session.setMode(mode);
        session.setTotalSites(totalSites);
        session.setTotalPages(totalPages);
        session.setTotalLemmas(totalLemmas);
        session.setTotalErrors(totalErrors);
        session.setDurationSeconds(durationSeconds);
        session.setStatus(status);
        session.setStartedBy(startedBy);

        IndexingSession saved = sessionRepository.save(session);
        logger.info("💾 Сохранена сессия ID={} | mode={} | status={} | pages={} | lemmas={} | errors={}",
                saved.getId(), mode, status, totalPages, totalLemmas, totalErrors);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<IndexingSession> getRecentSessions(int limit) {
        return sessionRepository.findAll().stream()
                .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()))
                .limit(limit)
                .toList();
    }
}
