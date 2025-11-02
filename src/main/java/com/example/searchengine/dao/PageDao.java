package com.example.searchengine.dao;

import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Repository;
import com.example.searchengine.models.Page;
import com.example.searchengine.config.Site;
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
        return ((Number) entityManager.createQuery(
                "SELECT COUNT(*) FROM Page").getSingleResult()).intValue();
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
}