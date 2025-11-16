package com.example.searchengine.indexing;

import com.example.searchengine.config.Site;
import com.example.searchengine.dto.statistics.Data;
import com.example.searchengine.models.*;
import com.example.searchengine.repository.IndexRepository;
import com.example.searchengine.repository.LemmaRepository;
import com.example.searchengine.repository.PageRepository;
import com.example.searchengine.services.CrawlerService;
import com.example.searchengine.services.DatabaseService;
import com.example.searchengine.utils.UrlParser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

@Service
@EnableAsync
@RequiredArgsConstructor
@Transactional
@Slf4j
public class IndexServiceImpl implements IndexService {

    private final IndexRepository indexRepository;
    private final UrlParser urlRecursiveParser;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final DatabaseService databaseService;
    private final CrawlerService crawlerService;

    private static final Logger logger = LoggerFactory.getLogger(IndexServiceImpl.class);

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    @Async
    public void indexPage(String url) throws Exception {
        logger.info("Начало индексации страницы: {}", url);
        Set<String> links = crawlerService.startParsing(url);
        List<CompletableFuture<Void>> tasks = new ArrayList<>();

        for (String link : links) {
            CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                try {
                    databaseService.addPagesToDatabase(link);
                    logger.info("Страница успешно проиндексирована: {}", link);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            });
            tasks.add(task);
        }
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    return null;
                })
                .exceptionally(ex -> {
                    if (ex instanceof CompletionException && ex.getCause() != null) {
                        Throwable cause = ex.getCause();
                        logger.error("Ошибка при индексации страниц: {}", cause.getMessage(), cause);
                        throw new RuntimeException(cause);
                    } else {
                        logger.error("Непредвиденная ошибка: {}", ex.getMessage(), ex);
                        throw new RuntimeException(ex);
                    }
                }).join();
    }


    @Override
    public int saveIndex(Index index) {
        if (index == null) {
            logger.error("Передан нулевой индекс для сохранения");
            return -1;
        }

        logger.info("Сохранение индекса: {}", index);
        Index savedIndex = indexRepository.save(index);
        return savedIndex.getId();
    }

    @Override
    public boolean checkIfIndexExists(Integer pageId, Integer lemmaId) {
        logger.info("Проверка существования индекса для страницы ID: {}, леммы ID: {}", pageId, lemmaId);
        return indexRepository.existsByPageIdAndLemmaId(pageId, lemmaId);
    }


    @Override
    public Optional<Index> findIndex(Integer id) {
        logger.info("Поиск индекса с ID: {}", id);
        return indexRepository.findById(id);
    }


    @Override
    public List<Index> findByLemmaId(Integer lemmaId) {
        logger.info("Поиск индексов по ID леммы: {}", lemmaId);
        Optional<Lemma> optionalLemma = lemmaRepository.findById(lemmaId);

        if (optionalLemma.isPresent()) {
            Lemma lemma = optionalLemma.get();
            return lemmaRepository.findIndexesByLemma(lemma);
        } else {
            throw new EntityNotFoundException("Лемма с указанным ID не найдена");
        }
    }



    @Override
    public Index findById(Integer indexId) {
        return indexRepository.findById(indexId).orElse(null);
    }


    @Override
    public List<Index> findAllIndexes() {
        logger.info("Получение всех индексов");
        List<Index> indexes = indexRepository.findAll();
        if (indexes.isEmpty()) {
            logger.warn("Нет доступных индексов.");
        }
        return indexes;
    }


    @Override
    public String getUrlFromIndex(Index index) {
        if (index == null || index.getPage() == null) {
            logger.error("Передан некорректный индекс");
            return null;
        }
        return index.getPage().getPath();
    }

    @Override
    public void deleteByPageId(Integer pageId, UUID sessionId) {
        logger.info("Удаление индексов по ID страницы: {}", pageId);
        pageRepository.deleteByPageId(pageId);
    }

    @Override
    public int deleteAllIndexes() {
        logger.info("Удаление всех индексов");
        int countBeforeDeletion = (int) indexRepository.count();
        indexRepository.deleteAll();
        return countBeforeDeletion;
    }


    @Override
    public Set<String> getParsedLinks(String url) throws Exception {
        return crawlerService.startParsing(url);
    }


    @Override
    public List<SearchResult> findPagesForQuery(String query) {
        Session session = entityManager.unwrap(Session.class);
        Transaction transaction = null;
        List<SearchResult> results = new ArrayList<>();
        try {
            transaction = session.beginTransaction();
            Query<Page> hqlQuery = session.createNamedQuery("Page.findByContentLike", Page.class);
            hqlQuery.setParameter("content", "%" + query + "%");
            List<Page> foundPages = hqlQuery.list();
            for (Page page : foundPages) {
                SearchResult result = new SearchResult();
                result.setSuccess(true);
                result.setTotalCount(foundPages.size());
                Data dataItem = new Data();
                dataItem.setSite(page.getSite());
                dataItem.setPath(page.getPath());
                dataItem.setTitle(page.getPath());
                dataItem.setSnippet(truncateText(page.getContent(), 150));
                dataItem.setRelevance(computeRelevance(page.getContent(), query));
                result.getData().add(dataItem);
                results.add(result);
            }
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Ошибка при поиске страниц:", e);
        } finally {
            session.close();
        }
        return results;
    }


    private String truncateText(String text, int maxLength) {
        if (text.length() > maxLength) {
            return text.substring(0, maxLength) + "...";
        }
        return text;
    }


    private double computeRelevance(String content, String query) {
        int occurrences = countOccurrences(content, query);
        return (double) occurrences / content.length();
    }


    private int countOccurrences(String text, String substring) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(substring, idx)) >= 0) {
            count++;
            idx += substring.length();
        }
        return count;
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
            jakarta.persistence.Query query = entityManager.createQuery(
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
            jakarta.persistence.Query query = entityManager.createQuery(
                    "DELETE FROM Index i WHERE i.page.id = :pageId");
            query.setParameter("pageId", pageId);
            int rowsDeleted = query.executeUpdate();
            logger.info("Indexes deleted by page ID (pageId={}): {} row(s)", pageId, rowsDeleted);
            return null;
        }, "Failed to delete indexes by pageId");
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLemmaAndIndex(Site site, Integer pageId, String lemmaText, float rank) {
        Lemma lemma = new Lemma(lemmaText, 1, site, site.getStatus());
        lemmaRepository.save(lemma);

        Optional<Page> optionalPage = pageRepository.findById(pageId);
        if (optionalPage.isEmpty()) {
            logger.error("Страница с ID {} не найдена.", pageId);
            return;
        }

        Page page = optionalPage.get();
        Index index = new Index();
        index.setLemma(lemma);
        index.setPage(page);
        index.setSite(site);
        index.setRank(rank);

        indexRepository.save(index);
        logger.debug("Индекс успешно сохранён для страницы с ID: {}", pageId);
    }


    public Integer count() {
        return ((Number) entityManager.createQuery(
                "SELECT COUNT(i) FROM Index i").getSingleResult()).intValue();
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
