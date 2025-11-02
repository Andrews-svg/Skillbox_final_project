package com.example.searchengine.indexing;

import com.example.searchengine.dao.IndexDao;
import com.example.searchengine.dto.statistics.Data;
import com.example.searchengine.exceptions.InvalidSiteException;
import com.example.searchengine.models.*;
import com.example.searchengine.repository.IndexRepository;
import com.example.searchengine.repository.LemmaRepository;
import com.example.searchengine.utils.DBSaver;
import com.example.searchengine.utils.UrlRecursiveParser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@EnableAsync
@RequiredArgsConstructor
@Transactional
@Slf4j
public class IndexServiceImpl implements IndexService {

    private final IndexDao indexDao;
    private final IndexRepository indexRepository;
    private final UrlRecursiveParser urlRecursiveParser;
    private final DBSaver dbSaver;
    private final LemmaRepository lemmaRepository;

    private static final Logger logger = LoggerFactory.getLogger(IndexServiceImpl.class);

    @PersistenceContext
    private EntityManager entityManager;



    @Override
    @Async
    public void indexPage(String url) throws IOException, InterruptedException {
        logger.info("Начало индексации страницы: {}", url);

        Set<String> links = urlRecursiveParser.startParsing(url);
        List<CompletableFuture<Void>> tasks = new ArrayList<>();

        links.forEach(link -> {
            CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                try {
                    dbSaver.addPagesToDatabase(link);
                    logger.info("Страница успешно проиндексирована: {}", link);
                } catch (Exception ex) {
                    logger.error("Ошибка при индексе страницы {}: {}", link, ex.getMessage());
                }
            });
            tasks.add(task);
        });

        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
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
            return indexDao.findByLemma(lemma);
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
        return index.getPage().getUrl();
    }


    @Override
    public void deleteByPageId(Integer pageId, UUID sessionId) {
        logger.info("Удаление индексов по ID страницы: {}", pageId);
        indexDao.deleteByPageId(pageId);
    }

    @Override
    public int deleteAllIndexes() {
        logger.info("Удаление всех индексов");
        int countBeforeDeletion = (int) indexRepository.count();
        indexRepository.deleteAll();
        return countBeforeDeletion;
    }


    @Override
    public Set<String> getParsedLinks(String url) throws IOException,
            InterruptedException {
        return urlRecursiveParser.startParsing(url);
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
                dataItem.setTitle(page.getTitle());
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
}
