package com.example.searchengine.indexing;

import com.example.searchengine.config.Site;
import com.example.searchengine.models.SearchResult;
import com.example.searchengine.dto.statistics.Data;
import com.example.searchengine.models.Index;
import com.example.searchengine.models.Lemma;
import com.example.searchengine.models.Page;
import com.example.searchengine.repository.IndexRepository;
import com.example.searchengine.repository.LemmaRepository;
import com.example.searchengine.repository.PageRepository;
import com.example.searchengine.services.CrawlerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@Component
public class AdvancedIndexOperations {

    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final CrawlerService crawlerService;
    private final IndexServiceImpl indexServiceImpl;

    private static final Logger log = LoggerFactory.getLogger(AdvancedIndexOperations.class);

    @PersistenceContext
    private EntityManager entityManager;

    public AdvancedIndexOperations(IndexRepository indexRepository,
                                   LemmaRepository lemmaRepository,
                                   PageRepository pageRepository,
                                   CrawlerService crawlerService,
                                   IndexServiceImpl indexServiceImpl) {
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.crawlerService = crawlerService;
        this.indexServiceImpl = indexServiceImpl;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLemmaAndIndex(Site site, long pageId, String lemmaText, float rank) {
        Lemma lemma = new Lemma(lemmaText, 1, site);
        lemmaRepository.save(lemma);
        Optional<Page> optionalPage = pageRepository.findById(pageId);
        if (optionalPage.isEmpty()) {
            log.error("Страница с ID {} не найдена.", pageId);
            return;
        }
        Page page = optionalPage.get();
        Index index = new Index();
        index.setLemma(lemma);
        index.setPage(page);
        index.setRank(rank);
        indexRepository.save(index);
        log.debug("Индекс успешно сохранён для страницы с ID: {}", pageId);
    }


    public long saveOrUpdateIndex(Index index) {
        return executeWithLogging(() -> {
            long pageId = index.getPage().getId();
            long lemmaId = index.getLemma().getId();
            if (!indexServiceImpl.checkIfIndexExists(pageId, lemmaId)) {
                entityManager.persist(index);
                log.info("Index saved successfully: {}", index);
                return index.getId();
            } else {
                Index indexFromDB = findByIdPair(pageId, lemmaId);
                float currentRank = index.getRank();
                float dbRank = indexFromDB.getRank();
                float updatedRank = currentRank + dbRank;
                indexFromDB.setRank(updatedRank);
                indexServiceImpl.update(indexFromDB);
                log.info("Index updated successfully: {}", indexFromDB);
                return indexFromDB.getId();
            }
        }, "Failed to save or update index");
    }

    public Index findByIdPair(long pageId, long lemmaId) {
        return executeWithLogging(() -> {
            TypedQuery<Index> query = entityManager.createQuery(
                    "SELECT i FROM Index i WHERE i.page.id = " +
                            ":pageId AND i.lemma.id = :lemmaId", Index.class);
            query.setParameter("pageId", pageId);
            query.setParameter("lemmaId", lemmaId);
            Index index = query.getSingleResult();
            log.info("Index found by pair (pageId: {}, lemmaId: {}): {}",
                    pageId, lemmaId, index);
            return index;
        }, "Failed to find index by pair");
    }



    public List<Index> findByLemmaId(long lemmaId) {
        log.info("Поиск индексов по ID леммы: {}", lemmaId);
        Optional<Lemma> optionalLemma = lemmaRepository.findById(lemmaId);
        if (optionalLemma.isPresent()) {
            Lemma lemma = optionalLemma.get();
            return lemmaRepository.findIndexesByLemma(lemma);
        } else {
            throw new EntityNotFoundException("Лемма с указанным ID не найдена");
        }
    }


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
                dataItem.setSnippet(truncateText(page.getContent()));
                dataItem.setRelevance(computeRelevance(page.getContent(), query));
                result.getData().add(dataItem);
                results.add(result);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            log.error("Ошибка при поиске страниц:", e);
        } finally {
            session.close();
        }
        return results;
    }


    private String truncateText(String text) {
        if (text.length() > 150) {
            return text.substring(0, 150) + "...";
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


    public Set<String> getParsedLinks(String url) throws Exception {
        return crawlerService.startParsing(url);
    }


    public String getUrlFromIndex(Index index) {
        if (index == null || index.getPage() == null) {
            log.error("Некорректный индекс для преобразования в URL");
            return null;
        }
        return index.getPage().getPath();
    }


    protected <T> T executeWithLogging(Supplier<T> action, String errorMessage) {
        try {
            return action.get();
        } catch (Exception e) {
            log.error("{}: {}", errorMessage, e.getMessage());
            throw new RuntimeException(errorMessage, e);
        }
    }
}