package com.example.searchengine.services;

import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.repositories.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
public class SiteService {

    private static final Logger logger = LoggerFactory.getLogger(SiteService.class);
    private final SiteRepository siteRepository;


    public SiteService(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }


    @Transactional
    public void save(Site site) {
        validateSite(site);
        siteRepository.save(site);
        logger.info("Сайт сохранен: {} ({})", site.getName(), site.getUrl());
    }


    @Transactional
    public void delete(Site site) {
        siteRepository.delete(site);
        logger.info("Сайт удален: {} ({})", site.getName(), site.getUrl());
    }


    @Transactional
    public void deleteById(long siteId) {
        siteRepository.deleteById(siteId);
        logger.info("Сайт с id {} удален", siteId);
    }


    public Optional<Site> findById(Long siteId) {
        logger.debug("Поиск сайта по ID: {}", siteId);
        return siteRepository.findById(siteId);
    }


    @Transactional(readOnly = true)
    public Optional<Site> findByUrl(String url) {
        return siteRepository.findByUrl(url);
    }


    @Transactional(readOnly = true)
    public List<Site> findAll() {
        return siteRepository.findAll();
    }


    @Transactional(readOnly = true)
    public boolean isAnySiteIndexing() {
        return siteRepository.existsByStatus(Status.INDEXING);
    }


    @Transactional
    public void updateStatus(Site site, Status status) {
        LocalDateTime now = LocalDateTime.now();
        int updated = siteRepository.updateStatusById(site.getId(), status, now);
        if (updated == 0) {
            logger.warn("Не удалось обновить статус сайта id={} — сайт не найден", site.getId());
            return;
        }
        site.setStatus(status);
        site.setStatusTime(now);
        logger.info("Статус сайта {} обновлен на {}", site.getUrl(), status);
    }


    @Transactional
    public void updateStatusWithError(Site site, String error) {
        LocalDateTime now = LocalDateTime.now();
        int updated = siteRepository.updateStatusWithErrorById(
                site.getId(), Status.FAILED, now, error);
        if (updated == 0) {
            logger.warn("Не удалось обновить статус с ошибкой для сайта id={}", site.getId());
            return;
        }
        site.setStatus(Status.FAILED);
        site.setStatusTime(now);
        site.setLastError(error);
        logger.error("Ошибка индексации сайта {}: {}", site.getUrl(), error);
    }


    @Transactional
    public Site createNewSite(String url, String name) {
        Site site = new Site(name, url);
        return siteRepository.save(site);
    }


    @Transactional
    public void updateStatusTime(Site site) {
        LocalDateTime now = LocalDateTime.now();
        int updated = siteRepository.updateStatusTimeById(site.getId(), now);
        if (updated > 0) {
            site.setStatusTime(now);
        }
        logger.debug("Время статуса обновлено для сайта {}", site.getUrl());
    }


    @Transactional
    public void updateStatusTimeForSites(List<Site> sites) {
        for (Site site : sites) {
            site.setStatusTime(LocalDateTime.now());
        }
        logger.debug("Время статуса обновлено для {} сайтов", sites.size());
    }


    @Transactional(readOnly = true)
    public long getTotalSites() {
        return siteRepository.count();
    }


    private void validateSite(Site site) {
        if (site.getUrl() == null || site.getUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("URL сайта не может быть пустым");
        }
        if (site.getName() == null || site.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Название сайта не может быть пустым");
        }
    }

    public long countByStatus(Status status) {
        return siteRepository.countByStatus(status);
    }

    public long countAll() {
        return siteRepository.count();
    }
}