package com.example.searchengine.services.indexing;

import com.example.searchengine.repositories.LemmaRepository;
import com.example.searchengine.repositories.PageRepository;
import com.example.searchengine.repositories.SiteRepository;
import com.example.searchengine.services.data.DatabaseService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.example.searchengine.config.SitesList;
import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.repositories.IndexRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class IndexingServiceImpl implements IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingServiceImpl.class);

    private final DatabaseService databaseService;
    private final SiteRepository siteRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesList;
    private final IndexServiceImpl indexServiceImpl;
    private final LemmaRepository lemmaRepository;

    private final ConcurrentHashMap<Long, Status> indexingStatuses = new ConcurrentHashMap<>();
    private boolean indexingInProgress = false;

    @PersistenceContext
    private EntityManager entityManager;

    public IndexingServiceImpl(DatabaseService databaseService, SiteRepository siteRepository,
                               IndexRepository indexRepository, PageRepository pageRepository,
                               SitesList sitesList, IndexServiceImpl indexServiceImpl,
                               LemmaRepository lemmaRepository) {
        this.databaseService = databaseService;
        this.siteRepository = siteRepository;
        this.indexRepository = indexRepository;
        this.pageRepository = pageRepository;
        this.sitesList = sitesList;
        this.indexServiceImpl = indexServiceImpl;
        this.lemmaRepository = lemmaRepository;
    }


    @Override
    @Transactional
    public void init() {
        try {
            logger.info("=== Инициализация поискового движка ===");
            List<SitesList.SiteConfig> configuredSites = sitesList.getSites();
            if (configuredSites.isEmpty()) {
                logger.warn("В конфигурации нет сайтов для индексации!");
            } else {
                logger.info("В конфигурации настроено {} сайтов", configuredSites.size());
            }
            long totalSites = siteRepository.count();
            long totalPages = pageRepository.count();
            long totalLemmas = lemmaRepository.count();
            logger.info("Текущее состояние БД:");
            logger.info("  - Сайтов: {}", totalSites);
            logger.info("  - Страниц: {}", totalPages);
            logger.info("  - Лемм: {}", totalLemmas);
            if (totalSites > 0) {
                List<Site> dbSites = siteRepository.findAll();
                logger.info("Сайты в БД:");
                dbSites.forEach(site ->
                        logger.info("  - {} (статус: {})", site.getUrl(), site.getStatus())
                );
            }
            syncSitesWithConfig();
            if (totalPages == 0 && totalLemmas == 0) {
                logger.info("База данных пуста. Готов к индексации реальных сайтов.");
            }
            logger.info("=== Инициализация завершена ===");
        } catch (Exception e) {
            logger.error("Критическая ошибка при инициализации движка: {}", e.getMessage(), e);
        }
    }


    @Transactional
    private void syncSitesWithConfig() {
        try {
            List<SitesList.SiteConfig> configuredSites = sitesList.getSites();
            List<Site> sitesToAdd = new ArrayList<>();
            for (SitesList.SiteConfig config : configuredSites) {
                Optional<Site> existing = siteRepository.findByUrl(config.getUrl());
                if (existing.isEmpty()) {
                    Site newSite = new Site();
                    newSite.setName(config.getName());
                    newSite.setUrl(config.getUrl());
                    newSite.setStatus(Status.FAILED);
                    newSite.setStatusTime(java.time.LocalDateTime.now());
                    newSite.setLastError("Сайт добавлен в конфигурации, но еще не индексирован");
                    sitesToAdd.add(newSite);
                    logger.info("Добавлен новый сайт из конфига: {}", config.getUrl());
                }
            }
            if (!sitesToAdd.isEmpty()) {
                siteRepository.saveAll(sitesToAdd);
            }
            List<Site> allDBSites = siteRepository.findAll();
            List<Site> sitesToDeactivate = new ArrayList<>();
            for (Site dbSite : allDBSites) {
                boolean existsInConfig = configuredSites.stream()
                        .anyMatch(config -> config.getUrl().equals(dbSite.getUrl()));
                if (!existsInConfig) {
                    logger.warn("Сайт {} есть в БД, но отсутствует в конфигурации", dbSite.getUrl());
                    dbSite.setStatus(Status.FAILED);
                    dbSite.setLastError("Сайт удален из конфигурации");
                    dbSite.setStatusTime(java.time.LocalDateTime.now());
                    sitesToDeactivate.add(dbSite);
                }
            }
            if (!sitesToDeactivate.isEmpty()) {
                siteRepository.saveAll(sitesToDeactivate);
                logger.info("Деактивировано {} сайтов, " +
                        "отсутствующих в конфигурации", sitesToDeactivate.size());
            }
            logger.info("Синхронизация конфигурации и БД завершена");
        } catch (Exception e) {
            logger.error("Ошибка при синхронизации сайтов с конфигурацией: {}", e.getMessage(), e);
        }
    }


    @Override
    @Transactional
    public boolean canStartIndexing(long id) {
        if (id <= 0) {
            logger.warn("Недопустимый ID: {}", id);
            return false;
        }
        if (indexingStatuses.putIfAbsent(id, Status.INDEXING) != null) {
            logger.warn("Индексация уже активна для ID: {}", id);
            return false;
        }
        logger.info("Индексирование успешно начато для ID: {}", id);
        return true;
    }

    @Override
    @Transactional
    public String getSiteUrlForId(long id) {
        Optional<Site> siteOptional = siteRepository.findById(id);
        return siteOptional.map(Site::getUrl).orElse(null);
    }


    @Override
    @Transactional
    public Site generateNewSite(String url) {
        Site newSite = new Site();
        newSite.setUrl(url);
        newSite.setStatus(Status.INDEXING);
        newSite.setStatusTime(LocalDateTime.now());
        return newSite;
    }


    @Override
    public void setIndexingStarted() {
        indexingInProgress = true;
        logger.info("Глобальная индексация начата (indexingInProgress = true)");
    }


    @Override
    public boolean isIndexingInProgress() {
        return indexingInProgress ||
                indexingStatuses.values().stream()
                        .anyMatch(status -> status == Status.INDEXING);
    }


    @Override
    public void setIndexingStopped() {
        indexingInProgress = false;
        indexingStatuses.clear();
        logger.info("Глобальная индексация остановлена");
    }


    public void updateSiteIndexingStatus(long siteId, Status status) {
        if (status == Status.INDEXING) {
            indexingStatuses.put(siteId, status);
            logger.debug("Добавлен статус INDEXING для сайта ID: {}", siteId);
        } else {
            indexingStatuses.remove(siteId);
            logger.debug("Удален статус INDEXING для сайта ID: {}", siteId);
        }
    }


    public int getActiveIndexingCount() {
        return (int) indexingStatuses.values().stream()
                .filter(status -> status == Status.INDEXING)
                .count();
    }



    public DatabaseService getDatabaseService() {
        return databaseService;
    }

    public SiteRepository getSiteRepository() {
        return siteRepository;
    }

    public IndexRepository getIndexRepository() {
        return indexRepository;
    }

    public PageRepository getPageRepository() {
        return pageRepository;
    }

    public SitesList getSitesList() {
        return sitesList;
    }

    public IndexServiceImpl getIndexServiceImpl() {
        return indexServiceImpl;
    }

    public ConcurrentHashMap<Long, Status> getIndexingStatuses() {
        return indexingStatuses;
    }

    public void setIndexingInProgress(boolean indexingInProgress) {
        this.indexingInProgress = indexingInProgress;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void logInfo(String message, Object... args) {
        logger.info(message, args);
    }
}
