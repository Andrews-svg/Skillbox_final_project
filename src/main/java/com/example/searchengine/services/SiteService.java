package com.example.searchengine.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class SiteService {

    private static final Logger logger = LoggerFactory.getLogger(SiteService.class);

    private final SitesList sitesList;
    private final SiteRepository siteRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public SiteService(SitesList sitesList, SiteRepository siteRepository) {
        this.sitesList = requireNonNull(sitesList, "SitesList cannot be null");
        this.siteRepository = requireNonNull(siteRepository, "SiteRepository cannot be null");
    }


    private void validateSite(Site site) throws InvalidSiteException {
        if (!isValidUrl(site.getUrl())) {
            throw new InvalidSiteException("Некорректный адрес сайта");
        }
    }


    private boolean isValidUrl(String inputUrl) {
        try {
            new URL(inputUrl).toURI();
            Pattern pattern =
                    Pattern.compile(
                            "^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$");
            return pattern.matcher(inputUrl).matches();
        } catch (Exception e) {
            return false;
        }
    }


    @Transactional
    public void saveSite(Site site) throws InvalidSiteException {
        validateSite(site);
        logger.debug("Before saving site: {}", site);
        siteRepository.saveAndFlush(site);
        entityManager.refresh(site);

        if (site.getId() == null) {
            logger.error("Error: site ID is null after saving.");
            throw new IllegalStateException(
                    "Ошибка при сохранении сайта: идентификатор не присвоен.");
        }
        logger.info("Website saved with ID: {}", site.getId());
    }


    @Transactional
    public void saveAll(List<Site> sites) throws InvalidSiteException {
        for (Site site : sites) {
            saveSite(site);
        }
    }


    public boolean isAnySiteIndexing() {
        List<Site> indexingSites = siteRepository.findByStatus(Status.INDEXING);
        return !indexingSites.isEmpty();
    }


    public Site fetchFullyLoadedSite(Integer siteId) {
        TypedQuery<Site> query = entityManager.createQuery(
                "SELECT s FROM Site s LEFT JOIN FETCH s.pages WHERE s.id = :siteId",
                Site.class
        ).setParameter("siteId", siteId);

        Optional<Site> result = Optional.ofNullable(query.getSingleResult());
        if (result.isEmpty()) {
            throw new EntityNotFoundException("Сайт с идентификатором " + siteId + " не найден.");
        }
        return result.get();
    }


    @Cacheable(value = "sites", key = "#id")
    public Optional<Site> findById(Integer id) {
        return siteRepository.findById(id);
    }


    @Transactional(readOnly = true)
    public Optional<Site> findByUrl(String url) {
        if (isBlank(url)) {
            throw new IllegalArgumentException("URL not allowed to be blank");
        }
        return siteRepository.findByUrl(url);
    }


    public List<Site> findAllSites() {
        return sitesList.getSites().values().stream()
                .map(config -> new Site(Status.INDEXING, LocalDateTime.now(),
                        config.getUrl(), config.getName()))
                .collect(Collectors.toList());
    }


    public Optional<Site> determineSiteForPage(String pageUri) {
        if (pageUri == null || pageUri.trim().isEmpty()) {
            throw new IllegalArgumentException("Full URI of a page must not be empty");
        }
        String domain = extractDomainFromUri(pageUri);
        return findByDomain(domain);
    }


    private String extractDomainFromUri(String uri) {
        try {
            URI parsedUri = new URI(uri);
            return parsedUri.getHost();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI format provided.", e);
        }
    }


    public Optional<Site> findByDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            throw new IllegalArgumentException("Domain name cannot be empty");
        }
        return siteRepository.findByDomain(domain);
    }


    public Optional<Integer> getSiteId(String siteUrl) {
        if (siteUrl == null || siteUrl.isBlank()) {
            return Optional.empty();
        }
        return siteRepository.findByUrl(siteUrl).map(Site::getId);
    }


    public int getTotalSites() {
        return Math.toIntExact(siteRepository.count());
    }


    @Transactional
    @CacheEvict(value = "sites", key = "#site.id")
    public void delete(Site site) {
        siteRepository.delete(site);
        logger.info("Deleted site: {}", site);
    }


    private <T> T executeWithLogging(Supplier<T> action, String errorMessage) {
        try {
            return action.get();
        } catch (Exception e) {
            logger.error("{}: {}", errorMessage, e.getMessage());
            throw new RuntimeException(errorMessage, e);
        }
    }


    public Integer count() {
        return executeWithLogging(() ->
                        ((Number)entityManager.createQuery(
                                "SELECT COUNT(s) FROM Site s").getSingleResult()).intValue(),
                "Failed to count sites");
    }

    public Integer countSites() {
        return Math.toIntExact(siteRepository.count());
    }
}