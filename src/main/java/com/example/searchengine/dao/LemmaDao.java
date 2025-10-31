package com.example.searchengine.dao;

import com.example.searchengine.config.Site;
import com.example.searchengine.models.*;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.*;


@Repository
public class LemmaDao {

    private static final Logger logger = LoggerFactory.getLogger(LemmaDao.class);

    private final EntityManager entityManager;

    private static final double FREQUENCY_THRESHOLD = 0.8;

    public LemmaDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Integer save(Lemma lemma) {
        entityManager.persist(lemma);
        logger.info("Lemma saved successfully: {}", lemma);
        return lemma.getId();
    }


    public void saveOrUpdateLemma(Lemma lemma) {
        TypedQuery<Lemma> query = entityManager.createQuery(
                "SELECT l FROM Lemma l WHERE l.lemma = :lemma AND l.site.id = :siteId", Lemma.class);
        query.setParameter("lemma", lemma.getLemma());
        query.setParameter("siteId", lemma.getSite().getId());
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);

        List<Lemma> resultList = query.getResultList();
        if (!resultList.isEmpty()) {
            Lemma existingLemma = resultList.get(0);
            existingLemma.setFrequency(existingLemma.getFrequency() + 1);
            entityManager.merge(existingLemma);
            logger.info("saveOrUpdateLemma updated successfully: {}", existingLemma);
        } else {
            save(lemma);
        }
    }

    @CachePut(value = "lemmas", key = "#lemma.id")
    public void update(Lemma lemma) {
        entityManager.merge(lemma);
        logger.info("Lemma updated successfully: {}", lemma);
    }

    public boolean checkIfLemmaExists(String name) {
        TypedQuery<Lemma> query = entityManager.createQuery(
                "SELECT l FROM Lemma l WHERE l.lemma = :lemma", Lemma.class);
        query.setParameter("lemma", name);
        try {
            query.getSingleResult();
            logger.info("Lemma exists check for '{}': true", name);
            return true;
        } catch (NoResultException e) {
            logger.info("Lemma exists check for '{}': false", name);
            return false;
        }
    }

    @Cacheable(value = "lemmas", key = "#id")
    public Optional<Lemma> findById(Integer id) {
        return Optional.ofNullable(entityManager.find(Lemma.class, id));
    }


    public List<Lemma> findLemmaByName(String name) {
        TypedQuery<Lemma> query = entityManager.createQuery(
                "SELECT l FROM Lemma l WHERE l.lemma = :lemma", Lemma.class);
        query.setParameter("lemma", name);
        return query.getResultList();
    }


    @Cacheable(value = "lemmas", key="'all'")
    public List<Lemma> findAll() {
        return findAll(-1);
    }


    @Cacheable(value = "lemmas", key="#limit")
    public List<Lemma> findAll(int limit) {
        TypedQuery<Lemma> query = entityManager.createQuery("SELECT l FROM Lemma l", Lemma.class);
        if (limit >= 0) {
            query.setMaxResults(limit);
        }
        List<Lemma> lemmas = query.getResultList();
        logger.info("Получено {} лемм.", lemmas.size());
        return lemmas;
    }

    public Optional<Lemma> findLemma(Integer lemmaId) {
        return Optional.ofNullable(entityManager.find(Lemma.class, lemmaId));
    }

    public List<Lemma> findAllBySite(Site site) {
        TypedQuery<Lemma> query = entityManager.createQuery(
                "SELECT l FROM Lemma l WHERE l.site.id = :siteId", Lemma.class);
        query.setParameter("siteId", site.getId());
        return query.getResultList();
    }

    public Lemma findByNameAndSiteID(String name, Integer siteId) {
        TypedQuery<Lemma> query = entityManager.createQuery(
                "SELECT l FROM Lemma l WHERE l.lemma =" +
                        ":lemma AND l.site.id = :siteId", Lemma.class);
        query.setParameter("lemma", name);
        query.setParameter("siteId", siteId);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<Lemma> findByName(String name) {
        TypedQuery<Lemma> query = entityManager.createQuery(
                "SELECT l FROM Lemma l WHERE l.lemma = :lemma", Lemma.class);
        query.setParameter("lemma", name);
        return query.getResultList();
    }

    public boolean isLemmaTooFrequent(Lemma lemma) {
        if (lemma == null) {
            throw new IllegalArgumentException("Лемма или частота не должны быть null");
        }
        return lemma.getFrequency() > FREQUENCY_THRESHOLD;
    }

    public Integer countLemmas() {
        TypedQuery<Integer> query = entityManager.createQuery(
                "SELECT COUNT(l) FROM Lemma l", Integer.class);
        return query.getSingleResult();
    }

    public Integer countLemmasOnSite(Integer siteId) {
        TypedQuery<Integer> query = entityManager.createQuery(
                "SELECT COUNT(l) FROM Lemma l WHERE l.site.id = :siteId", Integer.class);
        query.setParameter("siteId", siteId);
        return query.getSingleResult();
    }


    public Map<Integer, Integer> countLemmasGroupedBySite(Set<Integer> siteIds) {
        if (siteIds == null || siteIds.isEmpty()) {
            return new HashMap<>();
        }

        TypedQuery<Object[]> query = entityManager.createQuery(
                "SELECT l.site.id, COUNT(l) FROM Lemma l " +
                        "WHERE l.site.id IN (:ids) GROUP BY l.site.id",
                Object[].class
        );
        query.setParameter("ids", siteIds);

        Map<Integer, Integer> result = new HashMap<>();
        for (Object[] row : query.getResultList()) {
            Integer siteId = (Integer) row[0];
            Integer count = (Integer) row[1];
            result.put(siteId, count);
        }

        return result;
    }

    @CacheEvict(value = "lemmas", key = "#entity.id")
    public void delete(Lemma lemma) {
        entityManager.remove(entityManager.contains(lemma) ? lemma : entityManager.merge(lemma));
        logger.info("Lemma deleted successfully: {}", lemma);
    }


    @CacheEvict(value = "lemmas", allEntries = true)
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM Lemma").executeUpdate();
        logger.info("All lemmas deleted successfully");
    }
}
