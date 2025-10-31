package com.example.searchengine.dao;

import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import com.example.searchengine.models.Index;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.example.searchengine.models.Lemma;
import com.example.searchengine.models.Page;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;


@Repository
public class IndexDao {

    private static final Logger logger = LoggerFactory.getLogger(IndexDao.class);

    @PersistenceContext
    private EntityManager entityManager;

    public IndexDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Integer save(Index index) {
        return executeWithLogging(() -> {
            entityManager.persist(index);
            logger.info("Index saved successfully: {}", index);
            return index.getId();
        }, "Failed to save index");
    }

    public long saveOrUpdateIndex(Index index) {
        return executeWithLogging(() -> {
            if (!checkIfIndexExists(index.getPage(), index.getLemma())) {
                entityManager.persist(index);
                logger.info("Index savedOrUpdate successfully: {}", index);
                return index.getId();
            } else {
                logger.info("Index already exists!");
                Index indexFromDB = findByIdPair(index.getPage(), index.getLemma());

                float currentRank = index.getRank();
                float dbRank = indexFromDB.getRank();
                float updatedRank = currentRank + dbRank;

                indexFromDB.setRank(updatedRank);
                update(indexFromDB);
                logger.info("Index saveOrUpdated successfully: {}", indexFromDB);
                return indexFromDB.getId();
            }
        }, "Failed to save or update index");
    }


    public Boolean checkIfIndexExists(Page page, Lemma lemma) {
        return executeWithLogging(() -> {
            TypedQuery<Index> query = entityManager.createQuery(
                    "SELECT i FROM Index i WHERE i.page = :page AND i.lemma = :lemma", Index.class);
            query.setParameter("page", page);
            query.setParameter("lemma", lemma);
            boolean exists = !query.getResultList().isEmpty();
            logger.info("Index exists check for (page: {}, lemma: {}): {}",
                    page.getId(), lemma.getId(), exists);
            return exists;
        }, "Failed to check if index exists");
    }

    public Index findByIdPair(Page page, Lemma lemma) {
        return executeWithLogging(() -> {
            TypedQuery<Index> query = entityManager.createQuery(
                    "SELECT i FROM Index i WHERE i.page = :page AND i.lemma = :lemma", Index.class);
            query.setParameter("page", page);
            query.setParameter("lemma", lemma);
            Index index = query.getSingleResult();
            logger.info("Index found by pair (page: {}, lemma: {}): {}",
                    page.getId(), lemma.getId(), index);
            return index;
        }, "Failed to find index by pair");
    }

    public void update(Index index) {
        executeWithLogging(() -> {
            entityManager.merge(index);
            logger.info("Index updated successfully: {}", index);
            return null;
        }, "Failed to update index");
    }

    public Optional<Index> findById(Integer id) {
        return executeWithLogging(() -> {
            Index index = entityManager.find(Index.class, id);
            logger.info("Index found by id: {}: {}", id, index);
            return Optional.ofNullable(index);
        }, "Failed to find index by id");
    }

    public List<Index> findAll() {
        return findAll(-1);
    }

    public List<Index> findAll(int limit) {
        TypedQuery<Index> query = entityManager.createQuery(
                "SELECT i FROM Index i", Index.class);
        if (limit >= 0) {
            query.setMaxResults(limit);
        }
        List<Index> indexes = query.getResultList();
        logger.info("Получено {} индексов.", indexes.size());
        return indexes;
    }

    public List<Index> findByLemma(Lemma lemma) {
        return executeWithLogging(() -> {
            TypedQuery<Index> query = entityManager.createQuery(
                    "SELECT i FROM Index i WHERE i.lemma = :lemma", Index.class);
            query.setParameter("lemma", lemma);
            List<Index> indexes = query.getResultList();
            logger.info("Indexes found by lemmaId {}: {}", lemma.getId(), indexes);
            return indexes;
        }, "Failed to find indexes by lemma");
    }

    public void delete(Index index) {
        executeWithLogging(() -> {
            entityManager.remove(entityManager.contains(index) ?
                    index : entityManager.merge(index));
            logger.info("Index deleted: {}", index);
            return null;
        }, "Failed to delete index");
    }

    public void deleteAll() {
        executeWithLogging(() -> {
            entityManager.createQuery("DELETE FROM Index").executeUpdate();
            logger.info("All indexes deleted successfully");
            return null;
        }, "Failed to delete all indexes");
    }


    public void deleteByPage(Page page) {
        executeWithLogging(() -> {
            Query query = entityManager.createQuery(
                    "DELETE FROM Index i WHERE i.page = :page");
            query.setParameter("page", page);
            int rowsDeleted = query.executeUpdate();
            logger.info("Indexes deleted by page object (" +
                    "id={}): {} row(s)", page.getId(), rowsDeleted);
            return null;
        }, "Failed to delete indexes by page");
    }


    public void deleteByPageId(Integer pageId) {
        executeWithLogging(() -> {
            Query query = entityManager.createQuery(
                    "DELETE FROM Index i WHERE i.page.id = :pageId");
            query.setParameter("pageId", pageId);
            int rowsDeleted = query.executeUpdate();
            logger.info("Indexes deleted by page ID (pageId={}): {} row(s)", pageId, rowsDeleted);
            return null;
        }, "Failed to delete indexes by pageId");
    }

    public Long count() {
        return ((Number) entityManager.createQuery(
                "SELECT COUNT(i) FROM Index i").getSingleResult()).longValue();
    }

    private <T> T executeWithLogging(Supplier<T> action, String errorMessage) {
        try {
            return action.get();
        } catch (Exception e) {
            logger.error("{}: {}", errorMessage, e.getMessage());
            throw new RuntimeException(errorMessage, e);
        }
    }
}