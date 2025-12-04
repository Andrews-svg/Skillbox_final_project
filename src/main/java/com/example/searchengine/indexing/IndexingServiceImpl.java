package com.example.searchengine.indexing;

import com.example.searchengine.exceptions.IndexingStatusFetchException;
import com.example.searchengine.models.Page;
import com.example.searchengine.repository.PageRepository;
import com.example.searchengine.repository.SiteRepository;
import com.example.searchengine.services.DatabaseService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.example.searchengine.config.SitesList;
import com.example.searchengine.config.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.repository.IndexRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Getter
@Setter
@Service
public class IndexingServiceImpl implements IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingServiceImpl.class);

    private final DatabaseService databaseService;
    private final SiteRepository siteRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesList;
    private final IndexServiceImpl indexServiceImpl;

    private final ConcurrentHashMap<Long, Status> indexingStatuses = new ConcurrentHashMap<>();
    private boolean indexingInProgress = false;

    @PersistenceContext
    private EntityManager entityManager;


    public IndexingServiceImpl(DatabaseService databaseService, SiteRepository siteRepository,
                               IndexRepository indexRepository, PageRepository pageRepository,
                               SitesList sitesList, IndexServiceImpl indexServiceImpl) {
        this.databaseService = databaseService;
        this.siteRepository = siteRepository;
        this.indexRepository = indexRepository;
        this.pageRepository = pageRepository;
        this.sitesList = sitesList;
        this.indexServiceImpl = indexServiceImpl;
    }


    @Override
    @Transactional
    public void init() {
        try {
            List<Long> availablePageIds = indexServiceImpl.findAllAvailablePageIds();
            if (availablePageIds.isEmpty()) {
                Map<Long, SitesList.SiteConfig> registeredSitesMap = sitesList.getSites();

                if (registeredSitesMap.isEmpty()) {
                    logger.error("Нет зарегистрированных сайтов. Индексирование невозможно.");
                    return;
                }
                List<Site> registeredSites = registeredSitesMap.values().stream()
                        .filter(config -> !config.getName().isBlank() && !config.getUrl().isBlank())
                        .map(config -> {
                            Site site = new Site(config.getName(), config.getUrl());
                            siteRepository.save(site);
                            return site;
                        })
                        .toList();
                if (!registeredSites.isEmpty()) {
                    int totalPagesPerSite = 1000 / registeredSites.size();
                    for (Site site : registeredSites) {
                        for (int i = 1; i <= totalPagesPerSite; i++) {
                            Page page = new Page("/default-page-" + i, 200,
                                    "Default Content", site);
                            pageRepository.save(page);
                        }
                    }
                    availablePageIds = indexServiceImpl.findAllAvailablePageIds();
                    logger.info("Создано {} начальных идентификаторов.", availablePageIds.size());
                } else {
                    logger.warn("Ни один зарегистрированный сайт не прошел проверку на наличие имени и URL.");
                }
            }
            logger.info("Доступные pageId: {}", availablePageIds.size());
        } catch (Exception e) {
            logger.error("Ошибка при инициализации индексации", e);
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
    public boolean isIndexingInProgress() {
        return indexingInProgress;
    }


    @Override
    @Transactional
    public void setIndexingStopped() {
        indexingInProgress = false;
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
    @Transactional
    public String getStatus(long id) throws IndexingStatusFetchException {
        Optional<Site> siteOptional = siteRepository.findById(id);
        if (siteOptional.isEmpty()) {
            throw new IndexingStatusFetchException("Страница с таким ID не найдена.");
        }
        Site site = siteOptional.get();
        return site.getStatus().toString();
    }


    @Override
    public void logInfo(String message, Object... args) {
        logger.info(message, args);
    }
}
