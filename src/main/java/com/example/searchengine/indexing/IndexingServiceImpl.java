package com.example.searchengine.indexing;

import com.example.searchengine.exceptions.IndexingStatusFetchException;
import com.example.searchengine.repository.PageRepository;
import com.example.searchengine.repository.SiteRepository;
import com.example.searchengine.services.DatabaseService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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


@Getter
@Setter
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingServiceImpl.class);


    private final DatabaseService databaseService;
    private final SiteRepository siteRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesList;

    private final ConcurrentHashMap<Integer, Status> indexingStatuses = new ConcurrentHashMap<>();
    private boolean indexingInProgress = false;


    @Override
    @PostConstruct
    public void init() {
        try {
            List<Integer> availablePageIds = indexRepository.findAvailablePageIds();
            logger.info("Количество доступных pageId: {}", availablePageIds.size());

            if (availablePageIds.isEmpty()) {
                databaseService.ensureInitialData();
                availablePageIds = indexRepository.findAvailablePageIds();
                logger.info("После обновления количество pageId: {}", availablePageIds.size());
            }

            if (availablePageIds.isEmpty()) {
                logger.error("Не удалось создать доступные pageId. Индексация невозможна!");
            }
        } catch (Exception e) {
            logger.error("Ошибка при инициализации индексации", e);
        }
    }


    @Override
    @Transactional
    public boolean canStartIndexing(Integer id) {
        if (id == null || id <= 0) {
            logger.warn("Недопустимый ID: {}", id);
            return false;
        }
        if (indexingStatuses.putIfAbsent(id, Status.INDEXING) != null) {
            logger.warn("Индексация уже активна для ID: {}", id);
            return false;
        }
        return true;
    }


    @Override
    @Transactional
    public String getSiteUrlForId(Integer id) {
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
    public String getStatus(Integer id) throws IndexingStatusFetchException {
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
