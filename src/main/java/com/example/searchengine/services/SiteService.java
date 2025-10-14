package com.example.searchengine.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.searchengine.config.SiteSettings;
import com.example.searchengine.dao.SiteDao;
import com.example.searchengine.exceptions.InvalidSiteException;
import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static java.util.stream.Collectors.toList;

@Service
public class SiteService {

    private static final Logger logger =
            LoggerFactory.getLogger(SiteService.class);


    private final SiteSettings sitesList;
    private final SiteDao siteDao;
    private final SiteRepository siteRepository;
    private final NotificationService notificationService;
    private final SiteValidationService siteValidationService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public SiteService(SiteDao siteDao, SiteSettings sitesList,
                       SiteRepository siteRepository,
                       NotificationService notificationService,
                       SiteValidationService siteValidationService) {
        this.siteDao = requireNonNull(siteDao,
                "SiteDao cannot be null");
        this.sitesList = requireNonNull(sitesList,
                "SitesList cannot be null");
        this.siteRepository = requireNonNull(siteRepository,
                "SiteRepository cannot be null");
        this.notificationService = requireNonNull(notificationService,
                "NotificationService cannot be null");
        this.siteValidationService = siteValidationService;
    }

    @Transactional
    public void saveSite(Site site) throws InvalidSiteException {
        siteValidationService.validateSite(site);
        logger.debug("Перед сохранением сайта: {}", site);
        siteRepository.saveAndFlush(site);
        entityManager.refresh(site);
        if (site.getId() == null) {
            logger.error("Ошибка: идентификатор сайта равен null после сохранения.");
            throw new IllegalStateException("Ошибка при сохранении сайта: идентификатор не присвоен.");
        }
        logger.info("Сайт успешно сохранён с идентификатором: {}", site.getId());
    }


    public boolean isAnySiteIndexing() {
        List<Site> activeSites = siteRepository.findByStatus(Status.INDEXING);
        return !activeSites.isEmpty();
    }


    public Site fetchFullyLoadedSite(Long siteId) {
        TypedQuery<Site> query = entityManager.createQuery(
                "SELECT s FROM Site s LEFT JOIN FETCH s.pages WHERE s.id = :siteId",
                Site.class
        );
        query.setParameter("siteId", siteId);

        Optional<Site> result = Optional.ofNullable(query.getSingleResult());

        if (result.isEmpty()) {
            throw new EntityNotFoundException("Сайт с идентификатором " + siteId + " не найден.");
        }

        return result.get();
    }


    public Optional<Site> findById(Long id) {
        return siteRepository.findById(id);
    }


    @Transactional
    public void markAllSitesAsFailedImmediately() {
        executeWithLogging(() -> {
            List<Site> allSites = siteDao.findAll();
            allSites.forEach(s -> {
                s.setStatus(Status.FAILED);
                s.setLastError("Индексация прервана вручную!");
                siteDao.update(s);
            });
            logger.info("Все сайты отмечены как сбойные немедленно");
            notificationService.sendAdminNotification("Все сайты были отмечены " +
                    "как сбойные немедленно.");
            return null;
        });
    }


    @Async
    @PreAuthorize("hasRole('ADMIN')")
    public void scheduleMarkAllSitesAsFailed(long delayInSeconds) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delayInSeconds * 1000);
                markAllSitesAsFailedImmediately();
                logger.warn("Все сайты отмечены как сбойные после планирования задания.");
                notificationService.sendAdminNotification("Массовая пометка сайтов " +
                        "как сбойных выполнена успешно.");
            } catch (Exception ex) {
                logger.error("Ошибка при пометке всех сайтов как сбойных:", ex);
                notificationService.sendAdminNotification("Ошибка произошла " +
                        "при массовой пометке сайтов как сбойных.");
            }
        });
    }


    @Transactional
    public Optional<Site> findByUrl(String url) {
        if (isBlank(url)) {
            throw new IllegalArgumentException("URL не может быть пустым или null");
        }

        Optional<Site> foundSite = siteRepository.findByUrl(url);

        if (foundSite.isPresent()) {
            logger.info("Найден сайт с URL: {}", url);
        } else {
            logger.info("Нет сайта с указанным URL: {}", url);
        }
        return foundSite;
    }


    public List<Site> findAllSites() {
        Map<Long, SiteSettings.SiteConfig> siteConfigs = sitesList.getSites();
        if (siteConfigs == null) {
            logger.error("SitesList or its sites is null.");
            return Collections.emptyList();
        }
        return siteConfigs.values().stream()
                .map(config -> new Site(config.getName(),
                        config.getUrl(), Status.INDEXING))
                .collect(toList());
    }


    public Site determineSiteForPage(String uri) {
        if (uri == null || uri.trim().isEmpty()) {
            throw new IllegalArgumentException("Полный URI страницы не может быть пустым или null");
        }
        String domain = extractDomainFromUri(uri);
        return findByDomain(domain);
    }


    private String extractDomainFromUri(String uri) {
        try {
            URI parsedUri = new URI(uri);
            return parsedUri.getHost();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Некорректный формат URI страницы.", e);
        }
    }


    public Site findByDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            throw new IllegalArgumentException("Домен не может быть пустым или null");
        }
        return siteRepository.findByDomain(domain);
    }




    public boolean checkIfSiteExistsById(Long id) {
        Optional<Site> entity = siteRepository.findById(id);
        return entity.isPresent();
    }

    public boolean existsByUrl(String url) {
        if (url == null) {
            logger.warn("Attempted to find site with null URL.");
        }
        boolean exists = siteRepository.existsByUrl(url);
        logger.info(exists ? "Site exists for URL: {}" :
                "Site does not exist for URL: {}", url);
        return exists;
    }


    public Optional<Long> getSiteId(String siteUrl) {
        if (siteUrl == null || siteUrl.isBlank()) {
            return Optional.empty();
        }
        Optional<Site> siteOptional = siteRepository.findByUrl(siteUrl);
        return siteOptional.map(Site::getId);
    }

    public List<Long> getSiteIdsByGroupId(Long groupId) {
        return siteRepository.findByGroupId(groupId);
    }

    public Long getTotalSites() {
        return siteRepository.count();
    }


    @Transactional
    public void delete(Site site) {
        siteRepository.delete(site);
        logger.info("Site deleted: {}", site);
    }

    private <T> void executeWithLogging(Supplier<T> action) {
        try {
            action.get();
        } catch (Exception e) {
            logger.error("{}: {}", "Ошибка при пометке всех сайтов как сбойных немедленно", e.getMessage());
            throw new RuntimeException("Ошибка при пометке всех сайтов как сбойных немедленно", e);
        }
    }

    @Transactional(readOnly = true)
    public Long countSites() {
        return siteRepository.count();
    }
}

