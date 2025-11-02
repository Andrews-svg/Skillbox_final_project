package com.example.searchengine.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import com.example.searchengine.config.Site;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Repository
public class SiteDao {

    private static final Logger logger = LoggerFactory.getLogger(SiteDao.class);

    @PersistenceContext
    private EntityManager entityManager;

    public SiteDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Integer save(Site site) {
        return executeWithLogging(() -> {
            entityManager.persist(site);
            if (site.getId() == null) {
                logger.error("Ошибка: идентификатор сайта равен null после сохранения.");
                throw new IllegalStateException("Ошибка при сохранении сайта: идентификатор не присвоен.");
            }
            logger.info("Сайт успешно сохранён с идентификатором: {}", site.getId());
            return site.getId();
        }, "Ошибка при сохранении сайта");
    }

    @CachePut(value = "sites", key = "#site.id")
    public void update(Site site) {
        executeWithLogging(() -> {
            entityManager.merge(site);
            logger.info("Site updated successfully: {}", site);
            return null;
        }, "Failed to update site");
    }


    @Cacheable(value = "sites", key = "#id")
    public Optional<Site> findById(Integer id) {
        return executeWithLogging(() -> {
            Site site = entityManager.find(Site.class, id);
            logger.info("Site found by id {}: {}", id, site);
            return Optional.ofNullable(site);
        }, "Failed to find site by id");
    }

    @Cacheable
    public List<Site> findAll() {
        return findAll(-1);
    }


    @Cacheable(value = "sites")
    public List<Site> findAll(int limit) {
        TypedQuery<Site> query = entityManager.createQuery("SELECT s FROM Site s", Site.class);
        if (limit >= 0) {
            query.setMaxResults(limit);
        }
        List<Site> sites = query.getResultList();
        logger.info("Получено {} сайтов.", sites.size());
        return sites;
    }


    @CacheEvict(value = "sites", key = "#site.id")
    public void delete(Site site) {
        executeWithLogging(() -> {
            entityManager.remove(entityManager.contains(site) ? site : entityManager.merge(site));
            logger.info("Site deleted successfully: {}", site);
            return null;
        }, "Failed to delete site");
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
        return executeWithLogging(() -> {
            Integer count = (Integer) entityManager.createQuery(
                    "SELECT COUNT(s) FROM Site s").getSingleResult();
            logger.info("Counted total sites: {}", count);
            return count;
        }, "Failed to count sites");
    }


}
