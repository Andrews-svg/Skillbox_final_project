package com.example.searchengine.services.crawler;

import com.example.searchengine.models.Status;
import com.example.searchengine.repositories.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class IndexingRecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingRecoveryService.class);

    private final SiteRepository siteRepository;

    public IndexingRecoveryService(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void reconcileStaleIndexing() {
        try {
            int fixed = siteRepository.markStaleIndexingAsFailed(
                    Status.FAILED,
                    Status.INDEXING,
                    LocalDateTime.now(),
                    "Индексация прервана рестартом приложения"
            );
            if (fixed > 0) {
                logger.warn("🛠️ Recovery: {} сайт(ов) переведено из INDEXING в FAILED " +
                        "(индексация была прервана рестартом)", fixed);
            } else {
                logger.info("🛠️ Recovery: зависших INDEXING не найдено, состояние консистентно");
            }
        } catch (Exception e) {
            logger.error("🛠️ Recovery: ошибка примирения статусов — {}", e.getMessage(), e);
        }
    }
}