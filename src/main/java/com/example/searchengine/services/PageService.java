package com.example.searchengine.services;

import com.example.searchengine.config.Site;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import com.example.searchengine.models.Page;
import com.example.searchengine.repository.PageRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashMap;
import java.util.stream.Collectors;


@Service
public class PageService {

    private final PageRepository pageRepository;
    private final SiteService siteService;
    private final EntityManager entityManager;

    private static final Logger logger = LoggerFactory.getLogger(PageService.class);

    @Autowired
    public PageService(PageRepository pageRepository,
                       SiteService siteService,
                       EntityManager entityManager) {
        this.pageRepository = pageRepository;
        this.siteService = siteService;
        this.entityManager = entityManager;
    }


    @Transactional
    public void savePage(Page page) {
        checkMandatoryFields(page);
        determineSiteIfNecessary(page);
        if (!pageExists(page)) {
            createNewPage(page);
        } else {
            updateExistingPage(page);
        }
    }


    @Transactional
    public void saveAll(List<Page> pages) {
        int batchSize = 100;
        for (int i = 0; i < pages.size(); i++) {
            savePage(pages.get(i));

            if (i % batchSize == 0 && i > 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }


    public Integer validateAndSavePage(Page page) {
        if (!isValid(page)) {
            throw new IllegalArgumentException(
                    "Недостаточно данных для сохранения страницы.");
        }
        return pageRepository.save(page).getId();
    }


    @Transactional(readOnly = true)
    public Optional<Page> findByPath(String path) {
        return pageRepository.findByPath(path);
    }


    public Optional<Page> findById(Integer pageId) {
        return Optional.ofNullable(entityManager.find(Page.class, pageId));
    }


    @Cacheable(value="pageCount")
    public Integer countPages() {
        return (int) pageRepository.count();
    }


    @Transactional
    public void delete(Page entity) {
        if (entity == null || entity.getId() == null || entity.getId() <= 0) {
            logger.warn("Невозможно удалить страницу с неправильным ID: {}", entity);
            return;
        }
        pageRepository.delete(entity);
        logger.info("Удалена страница: {}", entity);
    }


    @Transactional
    public void deleteAll() {
        pageRepository.deleteAllInBatch();
        logger.info("Все страницы удалены успешно");
    }


    @Cacheable(value="sitePageCounts", key="#sites.hashCode()")
    public Map<Integer, Integer> countPagesGroupedBySite(List<Site> sites) {
        Set<Integer> siteIds = sites.stream().map(Site::getId).collect(Collectors.toSet());

        TypedQuery<Object[]> query = entityManager.createQuery(
                "SELECT p.site.id, COUNT(p) FROM Page p WHERE p.site.id IN (:ids) GROUP BY p.site.id",
                Object[].class);
        query.setParameter("ids", siteIds);

        Map<Integer, Integer> result = new HashMap<>();
        for (Object[] row : query.getResultList()) {
            Integer siteId = ((Number) row[0]).intValue();
            Integer count = ((Number) row[1]).intValue();
            result.put(siteId, count);
        }
        return result;
    }


    private void checkMandatoryFields(Page page) {
        if (page.getCode() == null) {
            throw new IllegalArgumentException("Код страницы не указан!");
        }
        if (page.getContent() == null || page.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Содержание страницы не указано!");
        }
    }


    private void determineSiteIfNecessary(Page page) {
        if (page.getSite() == null) {
            Site determinedSite = siteService.determineSiteForPage(page.getPath())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Не удалось определить сайт для страницы."));
            page.setSite(determinedSite);
        }
    }


    private boolean pageExists(Page page) {
        return pageRepository.existsByPath(page.getPath());
    }


    private void createNewPage(Page page) {
        try {
            pageRepository.save(page);
        } catch (PersistenceException | DataIntegrityViolationException e) {
            throw new RuntimeException("Ошибка сохранения страницы.", e);
        }
    }


    private void updateExistingPage(Page page) {
        Optional<Page> existingPageOpt = pageRepository.findByPath(page.getPath());
        if (existingPageOpt.isPresent()) {
            Page existingPage = existingPageOpt.get();
            existingPage.setContent(page.getContent());
            existingPage.setCode(page.getCode());
            pageRepository.save(existingPage);
        }
    }


    private boolean isValid(Page page) {
        return page.getPath() != null && !page.getPath().isEmpty()
                && page.getContent() != null && !page.getContent().isEmpty()
                && page.getSite() != null;
    }


    public Object getTotalPages() {
        return pageRepository.count();
    }
}
