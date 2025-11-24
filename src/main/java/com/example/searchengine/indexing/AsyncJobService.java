package com.example.searchengine.indexing;

import com.example.searchengine.config.Site;
import com.example.searchengine.config.SitesList;
import com.example.searchengine.models.Status;
import com.example.searchengine.repository.SiteRepository;
import com.example.searchengine.services.CrawlerService;
import com.example.searchengine.services.DatabaseService;
import com.example.searchengine.services.ErrorHandler;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
public class AsyncJobService {

    private final SiteRepository siteRepository;
    private final SitesList sitesList;
    private final IndexingServiceImpl indexingServiceImpl;
    private final ErrorHandler errorHandler;
    private final CrawlerService crawlerService;
    private final DatabaseService databaseService;

    private static final Logger logger = LoggerFactory.getLogger(AsyncJobService.class);

    @Autowired
    public AsyncJobService(
            SiteRepository siteRepository,
            SitesList sitesList,
            IndexingServiceImpl indexingServiceImpl,
            ErrorHandler errorHandler,
            CrawlerService crawlerService,
            DatabaseService databaseService
    ) {
        this.siteRepository = siteRepository;
        this.sitesList = sitesList;
        this.indexingServiceImpl = indexingServiceImpl;
        this.errorHandler = errorHandler;
        this.crawlerService = crawlerService;
        this.databaseService = databaseService;
    }



    @Transactional
    @Async
    public void indexPage(String url) throws Exception {
        logger.info("Начало индексации страницы: {}", url);
        Set<String> links = crawlerService.startParsing(url);
        List<CompletableFuture<Void>> tasks = new ArrayList<>();

        for (String link : links) {
            CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                try {
                    databaseService.addPagesToDatabase(link);
                    logger.info("Страница успешно проиндексирована: {}", link);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            });
            tasks.add(task);
        }
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    return null;
                })
                .exceptionally(ex -> {
                    if (ex instanceof CompletionException && ex.getCause() != null) {
                        Throwable cause = ex.getCause();
                        logger.error("Ошибка при индексации страниц: {}", cause.getMessage(), cause);
                        throw new RuntimeException(cause);
                    } else {
                        logger.error("Непредвиденная ошибка: {}", ex.getMessage(), ex);
                        throw new RuntimeException(ex);
                    }
                }).join();
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
                    errorHandler.handleError(id, throwable);
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
}