package com.example.searchengine.indexing;

import com.example.searchengine.services.HealthCheckService;
import com.example.searchengine.services.SiteValidationService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.searchengine.config.SiteList;
import com.example.searchengine.exceptions.InvalidSiteException;
import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.services.SiteService;
import com.example.searchengine.utils.DBSaver;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@Transactional
public class SiteManager {

    private final SiteService siteService;
    private final DBSaver dbSaver;
    private final SiteList sitesList;
    private final HealthCheckService healthCheckService;
    private final RobotsTxtChecker robotsTxtChecker;
    private volatile Status currentStatus = Status.INDEXED;
    private static final Pattern URL_PATTERN = Pattern.compile("/sites/([\\w-]+)");

    private static final Logger logger =
            LoggerFactory.getLogger(SiteManager.class);

    @Autowired
    private SiteValidationService validationService;

    @Autowired
    public SiteManager(SiteService siteService, DBSaver dbSaver,
                       RobotsTxtChecker robotsTxtChecker,
                       SiteList sitesList, HealthCheckService healthCheckService) {
        this.siteService = siteService;
        this.dbSaver = dbSaver;
        this.sitesList = sitesList;
        this.robotsTxtChecker = robotsTxtChecker;
        this.healthCheckService = healthCheckService;
    }

    public Site getSiteById(Long id) {
        return siteService.findById(id)
                .orElseThrow(() -> {
                    logger.error("Сайт с ID: {} не найден. Индексация невозможна.", id);
                    currentStatus = Status.FAILED;
                    return new NoSuchElementException("Сайт не найден с ID: " + id);
                });
    }

    public boolean isReadyForIndexing(Site site) {
        try {
            boolean available = validationService.checkSiteAvailability(site.getUrl());
            boolean validContent = validationService.checkContentValidity(site.getContent());
            boolean indexable = robotsTxtChecker.isSiteIndexableAsync(site).get();

            boolean ready = available && validContent && indexable;
            logger.info("Сайт с ID: {} готов к индексации: {}", site.getId(), ready);
            return ready;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> getSitesFromConfig() {
        Map<String, String> siteMap = new HashMap<>();
        Map<Long, SiteList.SiteConfig> sitesMap = sitesList.getSites();

        if (sitesMap != null && !sitesMap.isEmpty()) {
            for (SiteList.SiteConfig site : sitesMap.values()) {
                if (site != null && site.getName() != null && site.getUrl() != null) {
                    siteMap.put(site.getName(), site.getUrl());
                } else {
                    logger.warn("Объект сайта или его поля пусты: {}", site);
                }
            }
        } else {
            logger.warn("Карта сайтов пустая или не инициализирована.");
        }
        return siteMap;
    }

    public boolean checkIfSiteIsIndexedByFullName(String searchURL) {
        String uniqueIdentifier = extractUniqueIdentifierFromUrl(searchURL);
        try {
            long id = Long.parseLong(uniqueIdentifier);
            return siteService.checkIfSiteExistsById(id);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String extractUniqueIdentifierFromUrl(String searchURL) {
        Matcher matcher = URL_PATTERN.matcher(searchURL);
        if (matcher.find() && matcher.groupCount() > 0) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Невозможно извлечь идентификатор из указанного URL");
    }

    public void indexPagesFromMap(Map<String, String> siteList) throws InvalidSiteException {
        for (Map.Entry<String, String> entry : siteList.entrySet()) {
            String name = entry.getKey();
            String url = entry.getValue();
            Site site = new Site(name, url, Status.INDEXING);
            logger.info("Готовимся сохранить сайт с именем '{}' и URL '{}'", name, url);
            siteService.saveSite(site);
            logger.info("Сохранённый сайт: {}", site);
            if (site.getId() == null || site.getId() <= 0) {
                throw new RuntimeException("Не удалось присвоить идентификатор сайту " +
                        "с именем '" + name + "' и URL '" + url + "'.");
            }
            logger.info("Создан сайт с идентификатором: {}, имя: {}, URL: {}", site.getId(), name, url);
        }
        logger.info("Индексация завершена для всех сайтов из списка.");
    }

    public Status getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(Status status) {
        this.currentStatus = status;
    }
}