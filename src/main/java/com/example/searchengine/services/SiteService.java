package com.example.searchengine.services;

import com.example.searchengine.config.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.repository.SiteRepository;
import jakarta.persistence.EntityManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SiteService {

    private static final Logger logger = LoggerFactory.getLogger(SiteService.class);

    private final SiteRepository siteRepository;
    private final EntityManager entityManager;

    @Autowired
    public SiteService(SiteRepository siteRepository,
                       EntityManager entityManager) {
        this.siteRepository = siteRepository;
        this.entityManager = entityManager;
    }


    @Transactional
    public void createSite(Site site) {
        validateSite(site);
        siteRepository.save(site);
    }


    @Transactional
    public void updateSite(Site updatedSite) {
        validateSite(updatedSite);
        siteRepository.save(updatedSite);
    }


    @Transactional
    public void saveSite(Site site) throws InvalidSiteException {
        validateSite(site);
        siteRepository.save(site);
        logger.info("Сайт успешно сохранён с идентификатором: {}", site.getId());
    }


    @Transactional
    public void saveAll(List<Site> sites) throws InvalidSiteException {
        int batchSize = 100;
        for (int i = 0; i < sites.size(); i++) {
            Site site = sites.get(i);
            validateSite(site);
            siteRepository.save(site);
            if (i % batchSize == 0 && i > 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }


    @Transactional
    public void deleteSite(long siteId) {
        siteRepository.deleteById(siteId);
    }


    @Transactional(readOnly = true)
    public Optional<Site> findById(long id) {
        return siteRepository.findById(id);
    }


    @Transactional(readOnly = true)
    public Optional<Site> findSiteByUrl(String url) {
        return siteRepository.findByUrl(url);
    }


    @Transactional(readOnly = true)
    public Optional<Site> findByUrl(String url) {
        return siteRepository.findByUrl(url);
    }


    @Transactional(readOnly = true)
    public List<Site> findAllSites() {
        return siteRepository.findAll();
    }


    @Transactional(readOnly = true)
    public Optional<Site> findByExactUrl(String siteURL) {
        return siteRepository.findByUrl(siteURL);
    }


    public Site prepareSiteForSave(String url, String name, Status status) throws InvalidSiteException {
        if (!isValidUrl(url)) {
            throw new InvalidSiteException("Некорректный URL сайта");
        }
        Site site = new Site();
        site.setUrl(url);
        site.setName(name);
        site.setStatus(status);
        site.setStatusTime(java.time.LocalDateTime.now());
        return site;
    }


    private boolean isValidUrl(String inputUrl) {
        if (inputUrl == null || inputUrl.trim().isEmpty()) {
            return false;
        }
        Pattern pattern = Pattern.compile("^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
        return pattern.matcher(inputUrl).matches();
    }
    
    
    @Cacheable(value="siteCount")
    public long getTotalSites() {
        return siteRepository.count();
    }


    @Transactional(readOnly = true)
    public List<Site> listAllSites() {
        return siteRepository.findAll();
    }


    private String generateUUID() {
        return UUID.randomUUID().toString();
    }


    private void validateSite(Site site) {
        if (site.getUrl() == null || site.getUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Адрес сайта обязателен!");
        }
        if (site.getName() == null || site.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Название сайта обязано быть указано!");
        }
    }


    @Transactional(readOnly = true)
    public boolean isAnySiteIndexing() {
        List<Site> indexingSites = siteRepository.findByStatus(Status.INDEXING);
        return !indexingSites.isEmpty();
    }


    public static class InvalidSiteException extends Exception {
        public InvalidSiteException(String message) {
            super(message);
        }
    }

}