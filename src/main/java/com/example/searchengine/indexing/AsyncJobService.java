package com.example.searchengine.indexing;

import com.example.searchengine.config.Site;
import com.example.searchengine.config.SitesList;
import com.example.searchengine.models.Status;
import com.example.searchengine.repository.SiteRepository;
import com.example.searchengine.services.DatabaseService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class AsyncJobService {

    private final SiteRepository siteRepository;
    private final SitesList sitesList;
    private final DatabaseService databaseService;
    private final IndexingServiceImpl indexingServiceImpl;

    private static final Logger logger = LoggerFactory.getLogger(AsyncJobService.class);

    public AsyncJobService(SiteRepository siteRepository, SitesList sitesList,
                           DatabaseService databaseService,
                           IndexingServiceImpl indexingServiceImpl) {
        this.siteRepository = siteRepository;
        this.sitesList = sitesList;
        this.databaseService = databaseService;
        this.indexingServiceImpl = indexingServiceImpl;
    }


    @Transactional
    @Async
    public void startFullIndexing() {
        Map<Integer, SitesList.SiteConfig> configuredSites = sitesList.getSites();

        for (Map.Entry<Integer, SitesList.SiteConfig> entry : configuredSites.entrySet()) {
            Integer siteKey = entry.getKey();
            SitesList.SiteConfig siteConfig = entry.getValue();

            Optional<Site> existingSiteOptional = siteRepository.findByUrl(siteConfig.getUrl());
            existingSiteOptional.ifPresent(databaseService::deleteEntireSiteData);

            Site newSite = indexingServiceImpl.generateNewSite(siteConfig.getUrl());
            siteRepository.save(newSite);

            Integer newSiteId = newSite.getId();
            startIndexing(newSiteId, false);
        }

        logger.info("Запущена полная индексация всех сайтов.");
    }

    @Transactional
    @Async
    public void startIndexing(Integer id, boolean isLemma) {
        if (!indexingServiceImpl.canStartIndexing(id)) {
            return;
        }
        try {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    databaseService.indexSite(id);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    databaseService.updateSiteStatus(id, Status.INDEXED, "");
                } else {
                    handleError(id, throwable);
                }
            });
        } catch (Exception e) {
            logger.error("Ошибка при запуске индексации сайта с ID {}", id, e);
            databaseService.updateSiteStatus(id, Status.FAILED, "Ошибка запуска индексации");
        }
        logger.info("Индексация для ID: {} начата.", id);
    }

    @Transactional
    public void stopIndexing() {
        if (!indexingServiceImpl.isIndexingInProgress()) {
            throw new IllegalStateException("Индексация не запущена");
        }
        indexingServiceImpl.setIndexingStopped();
        logger.info("Процесс индексации остановлен.");
    }

    @Transactional
    public void processIndexingTask(Integer id) {
        try {
            Optional<Site> siteOptional = siteRepository.findById(id);
            if (siteOptional.isEmpty()) {
                logger.error("Не найден сайт с ID={}", id);
                databaseService.finishIndexing(id, false);
                return;
            }
            Site site = siteOptional.get();
            databaseService.indexSite(id);
            databaseService.finishIndexing(id, true);
        } catch (Exception ex) {
            databaseService.finishIndexing(id, false);
            logger.error("Ошибка при индексе страницы с ID={}: {}", id, ex.getMessage(), ex);
        }
    }

    @Transactional
    public void handleError(Integer id, Throwable t) {
        StringBuilder errorMsg = new StringBuilder("Ошибка индексации сайта ");
        if (t instanceof HttpStatusCodeException hse) {
            errorMsg.append(hse.getStatusCode().value())
                    .append(": ")
                    .append(hse.getResponseBodyAsString());
        } else if (t instanceof IOException ioex) {
            errorMsg.append(ioex.getLocalizedMessage());
        } else {
            errorMsg.append(t.getLocalizedMessage());
        }
        databaseService.updateSiteStatus(id, Status.FAILED, errorMsg.toString());
    }
}