package com.example.searchengine.dao;

import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Repository;
import com.example.searchengine.models.Page;
import com.example.searchengine.config.Site;
import com.example.searchengine.models.Status;
import org.springframework.cache.annotation.Cacheable;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class PageDao {

    private static final Logger logger = LoggerFactory.getLogger(PageDao.class);

    @PersistenceContext
    private EntityManager entityManager;


    public Integer save(Page page) {
        validatePage(page);
        entityManager.persist(page);
        return page.getId();
    }


    @CachePut(value = "pages", key = "#page.id")
    public void update(Page entity) {
        entityManager.merge(entity);
        logger.info("Страница успешно обновлена: {}", entity);
    }


    @Cacheable(value = "pages", key = "#id")
    public Optional<Page> findById(Long id) {
        Page page = entityManager.find(Page.class, id);
        if (page != null) {
            logger.info("Страница найдена по id {}: {}", id, page);
        } else {
            logger.warn("Страница не найдена по id: {}", id);
        }
        return Optional.ofNullable(page);
    }


    @Cacheable(value = "pages", key="'all'")
    public List<Page> findAll() {
        return findAll(-1);
    }


    @Cacheable(value = "pages", key="#limit")
    public List<Page> findAll(int limit) {
        TypedQuery<Page> query = entityManager.createQuery("FROM Page", Page.class);
        if (limit >= 0) {
            query.setMaxResults(limit);
        }
        List<Page> pages = query.getResultList();
        logger.info("Получено {} страниц.", pages.size());
        return pages;
    }


    public Optional<Page> findByName(String path) {
        return entityManager.createQuery("FROM Page WHERE path = :path", Page.class)
                .setParameter("path", path)
                .getResultStream()
                .findFirst();
    }

    public List<Page> findAllBySite(Site site) {
        if (site == null || site.getId() <= 0) {
            throw new IllegalArgumentException("Некорректный объект сайта.");
        }
        TypedQuery<Page> query = entityManager.createQuery("FROM Page WHERE site.id = :siteId", Page.class);
        query.setParameter("siteId", site.getId());
        List<Page> pages = query.getResultList();
        if (!pages.isEmpty()) {
            logger.info("Найдено {} страниц для сайта с id {}", pages.size(), site.getId());
        } else {
            logger.warn("Нет страниц для сайта с id {}", site.getId());
        }
        return pages;
    }

    public List<Page> findPagesByStatus(Status status) {
        return entityManager.createNamedQuery("Page.findByStatus", Page.class)
                .setParameter("status", status)
                .getResultList();
    }

    public boolean checkIfPageExists(String path) {
        return !entityManager.createQuery("FROM Page WHERE path = :path", Page.class)
                .setParameter("path", path)
                .getResultList().isEmpty();
    }



    public int getCountBySiteUrl(String url) {
        return ((Number) entityManager.createQuery(
                "SELECT COUNT(*) FROM Page WHERE url = :url", Long.class)
                .setParameter("url", url)
                .getSingleResult()).intValue();
    }

    public List<Page> getAllPages() {
        return findAll(-1);
    }

    public List<Page> getPagesBySiteUrl(String siteUrl) {
        if (siteUrl == null || siteUrl.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return entityManager.createQuery("FROM Page WHERE url = :url", Page.class)
                .setParameter("url", siteUrl)
                .getResultList();
    }


    @CacheEvict(value = "pages", key = "#entity.id")
    public void delete(Page entity) {
        if (entity == null || entity.getId() == null || entity.getId() <= 0) {
            logger.warn("Нельзя удалить страницу с неверным или отсутствующим id: {}", entity);
            return;
        }
        entityManager.remove(entity);
        logger.info("Удалена страница: {}", entity);
    }


    @CacheEvict(value = "pages", allEntries = true)
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM Page").executeUpdate();
        logger.info("Все страницы удалены успешно");
    }


    private void validatePage(Page page) {
        if (page.getUrl() == null || page.getUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Поле URL должно быть заполнено.");
        }
        if (page.getContent() == null || page.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Поле CONTENT должно быть заполнено.");
        }
        if (page.getSite() == null) {
            throw new IllegalArgumentException("Объект сайта (SITE) должен быть установлен.");
        }
    }


    public Integer count() {
        return ((Number) entityManager.createQuery("SELECT COUNT(*) FROM Page").getSingleResult()).intValue();
    }


    public int countPages() {
        return ((Number) entityManager.createQuery(
                "SELECT COUNT(*) FROM Page").getSingleResult()).intValue();
    }

    public int countPagesOnSite(Integer siteId) {
        return ((Number) entityManager.createQuery(
                        "SELECT COUNT(*) FROM Page p WHERE p.site.id = :site_id", Integer.class)
                .setParameter("site_id", siteId)
                .getSingleResult()).intValue();
    }

    public int countByUrl(String url) {
        return ((Number) entityManager.createQuery(
                        "SELECT COUNT(*) FROM Page WHERE url = :url", Long.class)
                .setParameter("url", url)
                .getSingleResult()).intValue();
    }

    public Integer countByContentContainingAndSiteUrl(String query, String siteUrl) {
        return ((Number) entityManager.createQuery(
                        "SELECT COUNT(p) FROM Page p WHERE LOWER(p.content) " +
                                "LIKE LOWER(:query) AND p.site.url = :siteUrl", Integer.class)
                .setParameter("query", "%" + query + "%")
                .setParameter("siteUrl", siteUrl)
                .getSingleResult()).intValue();
    }

    public Integer countByContentContaining(String query) {
        return ((Number) entityManager.createQuery(
                        "SELECT COUNT(p) FROM Page p WHERE LOWER(p.content) " +
                                "LIKE LOWER(:query)", Long.class)
                .setParameter("query", "%" + query + "%")
                .getSingleResult()).intValue();
    }


    public Map<Integer, Integer> countPagesGroupedBySite(List<Site> sites) {
        Set<Integer> siteIds = sites.stream().map(Site::getId).collect(Collectors.toSet());
        TypedQuery<Object[]> query = entityManager.createQuery(
                "SELECT p.site.id, COUNT(p) FROM Page p WHERE p.site.id IN (:ids) " +
                        "GROUP BY p.site.id", Object[].class);
        query.setParameter("ids", siteIds);
        Map<Integer, Integer> result = new HashMap<>();
        for (Object[] row : query.getResultList()) {
            Integer siteId = ((BigDecimal) row[0]).intValue();
            Integer count = (Integer) row[1];
            result.put(siteId, count);
        }
        return result;
    }


    public boolean existsByUri(String uri) {
        return entityManager.createQuery("SELECT COUNT(p) FROM Page p WHERE p.uri = :uri", Integer.class)
                .setParameter("uri", uri)
                .getSingleResult() > 0;
    }


    private <T> T executeWithLogging(java.util.function.Supplier<T> action, String errorMessage) {
        try {
            return action.get();
        } catch (Exception e) {
            logger.error("Ошибка: {}, Причина: {}", errorMessage, e.getMessage(), e);
            throw new RuntimeException(errorMessage, e);
        }
    }
}