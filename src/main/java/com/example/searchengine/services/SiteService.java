package com.example.searchengine.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.searchengine.config.SitesList;
import com.example.searchengine.exceptions.InvalidSiteException;
import com.example.searchengine.config.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static java.util.stream.Collectors.toList;


@Service
public class SiteService {

    private static final Logger logger =
            LoggerFactory.getLogger(SiteService.class);


    private final SitesList sitesList;
    private final SiteRepository siteRepository;


    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public SiteService(SitesList sitesList,
                       SiteRepository siteRepository) {
        this.sitesList = requireNonNull(sitesList,
                "SitesList cannot be null");
        this.siteRepository = requireNonNull(siteRepository,
                "SiteRepository cannot be null");
    }


    @Transactional
    public void saveSite(Site site) throws InvalidSiteException {
        validateSite(site);
        logger.debug("Перед сохранением сайта: {}", site);
        siteRepository.saveAndFlush(site);
        entityManager.refresh(site);

        if (site.getId() == null) {
            logger.error("Ошибка: идентификатор сайта равен null после сохранения.");
            throw new IllegalStateException("Ошибка при сохранении сайта: идентификатор не присвоен.");
        }
        logger.info("Сайт успешно сохранён с идентификатором: {}", site.getId());
    }
    
    
    private void validateSite(Site site) throws InvalidSiteException {
        if (!isValidUrl(site.getUrl())) { 
            throw new InvalidSiteException("Некорректный адрес сайта");
        }
    }

    private boolean isValidUrl(String inputUrl) {
        try {
            new URL(inputUrl).toURI();
            Pattern pattern = Pattern.compile(
                    "^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
            return pattern.matcher(inputUrl).matches();
        } catch (Exception e) {
            return false;
        }
    }


    @Transactional
    public void saveAll(List<Site> sites) throws InvalidSiteException {
        for (Site site : sites) {
            saveSite(site);
        }
    }


    public boolean isAnySiteIndexing() {
        List<Site> activeSites = siteRepository.findByStatus(Status.INDEXING);
        return !activeSites.isEmpty();
    }


    public Site fetchFullyLoadedSite(Integer siteId) {
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


    public Optional<Site> findById(Integer id) {
        return siteRepository.findById(id);
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
        Map<Integer, SitesList.SiteConfig> siteConfigs = sitesList.getSites();
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


    public Optional<Integer> getSiteId(String siteUrl) {
        if (siteUrl == null || siteUrl.isBlank()) {
            return Optional.empty();
        }
        Optional<Site> siteOptional = siteRepository.findByUrl(siteUrl);
        return siteOptional.map(Site::getId);
    }


    public Integer getTotalSites() {
        return (int) siteRepository.count();
    }


    @Transactional
    public void delete(Site site) {
        siteRepository.delete(site);
        logger.info("Site deleted: {}", site);
    }


    @Transactional(readOnly = true)
    public Integer countSites() {
        return (int) siteRepository.count();
    }
}

