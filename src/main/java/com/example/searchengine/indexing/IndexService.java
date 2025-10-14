package com.example.searchengine.indexing;

import com.example.searchengine.models.Status;
import com.example.searchengine.repository.LemmaRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.searchengine.dao.IndexDao;
import com.example.searchengine.exceptions.InvalidSiteException;
import com.example.searchengine.models.Index;
import com.example.searchengine.models.Lemma;
import com.example.searchengine.models.Page;
import com.example.searchengine.repository.IndexRepository;
import com.example.searchengine.repository.PageRepository;
import com.example.searchengine.services.IndexingHistoryService;
import com.example.searchengine.services.NotificationService;
import com.example.searchengine.utils.DBSaver;
import com.example.searchengine.utils.UrlRecursiveParser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


@Service
@Transactional
@Slf4j
public class IndexService {

    private final IndexDao indexDao;
    private final IndexRepository indexRepository;
    private final IndexingHistoryService indexingHistoryService;
    private final NotificationService notificationService;
    private final UrlRecursiveParser urlRecursiveParser;
    private final DBSaver dbSaver;
    private final LemmaRepository lemmaRepository;

    private static final Logger logger
            = LoggerFactory.getLogger(IndexService.class);

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    public IndexService(
            IndexDao indexDao,
            DBSaver dbSaver,
            UrlRecursiveParser urlRecursiveParser,
            IndexRepository indexRepository,
            IndexingHistoryService indexingHistoryService,
            NotificationService notificationService, LemmaRepository lemmaRepository
    ) {
        this.indexDao = indexDao;
        this.dbSaver = dbSaver;
        this.urlRecursiveParser = urlRecursiveParser;
        this.indexRepository = indexRepository;
        this.indexingHistoryService = indexingHistoryService;
        this.notificationService = notificationService;
        this.lemmaRepository = lemmaRepository;
    }


    public void indexPage(String url) throws IOException,
            InvalidSiteException, InterruptedException {
        logger.info("Начало индексации страницы: {}", url);

        Set<String> parsedLinks = urlRecursiveParser.startParsing(url);
        for (String link : parsedLinks) {
            try {
                dbSaver.addPagesToDatabase(link);
                logger.info("Страница успешно проиндексирована: {}", link);
            } catch (Exception ex) {
                logger.error("Ошибка при индексе страницы {}: {}", link, ex.getMessage());
            }
        }
    }


    public long saveIndex(Index index, UUID sessionId) {
        if (index == null) {
            logger.error("Передан нулевой индекс для сохранения");
            return -1;
        }

        logger.info("Сохранение индекса: {}", index);
        Index savedIndex = indexRepository.save(index);
        notificationService.addNotification("Индекс сохранен: " + savedIndex);
        String url = getUrlFromIndex(savedIndex);
        indexingHistoryService.addRecord(sessionId, new IndexingHistoryRecord(url,
                LocalDateTime.now(), Status.SAVED));
        return savedIndex.getId();
    }


    public long saveOrUpdateIndex(Index index) {
        logger.info("Сохранение или обновление индекса: {}", index);
        return indexDao.saveOrUpdateIndex(index);
    }


    public void updateIndex(Index index) {
        if (index == null) {
            logger.error("Передан нулевой индекс для обновления");
            return;
        }

        logger.info("Обновление индекса: {}", index);
        indexDao.update(index);
    }


    public boolean checkIfIndexExists(Long pageId, Long lemmaId) {
        logger.info("Проверка существования индекса для страницы ID: " +
                "{} и леммы ID: {}", pageId, lemmaId);
        return indexRepository.existsByPageIdAndLemmaId(pageId, lemmaId);
    }


    public Optional<Index> findIndex(Long id) {
        logger.info("Поиск индекса с ID: {}", id);
        return indexRepository.findById(id);
    }


    public List<Index> findByLemmaId(Long lemmaId) {
        logger.info("Поиск индексов по ID леммы: {}", lemmaId);
        Optional<Lemma> optionalLemma = lemmaRepository.findById(lemmaId);

        if (optionalLemma.isPresent()) {
            Lemma lemma = optionalLemma.get();
            return indexDao.findByLemma(lemma);
        } else {
            throw new EntityNotFoundException("Лемма с указанным ID не найдена");
        }
    }


    public Index findByIdPair(Long pageId, Long lemmaId) {
        Page page = pageRepository.findById(pageId).orElseThrow(() ->
                new EntityNotFoundException("Page not found"));
        Lemma lemma = lemmaRepository.findById(lemmaId).orElseThrow(() ->
                new EntityNotFoundException("Lemma not found"));
        return indexDao.findByIdPair(page, lemma);
    }


    public Index findById(Long indexId) {
        return indexRepository.findById(indexId).orElse(null);
    }


    public Optional<Index> findByPageAndLemma(Long pageId, Long lemmaId) {
        logger.info("Поиск индекса по паре (страница ID: {}, лемма ID: {})", pageId, lemmaId);
        return indexRepository.findByPageIdAndLemmaId(pageId, lemmaId);
    }


    public List<Index> findAllIndexes() {
        logger.info("Получение всех индексов");
        List<Index> indexes = indexRepository.findAll();
        if (indexes.isEmpty()) {
            logger.warn("Нет доступных индексов.");
        }
        return indexes;
    }


    public String getUrlFromIndex(Index index) {
        if (index == null) {
            logger.error("Передан нулевой индекс");
            return null;
        }

        if (index.getPage() == null) {
            logger.error("Страница для указанного индекса не найдена");
            return null;
        }

        String url = index.getPage().getUrl();
        logger.info("Возвращенный URL страницы: {}", url);
        return url;
    }


    public List<IndexingHistoryRecord> getIndexingHistory(UUID sessionId) {
        return indexingHistoryService.getHistory(sessionId);
    }


    public List<String> getNotifications() {
        return notificationService.getNotifications();
    }


    public void clearNotifications() {
        notificationService.clearNotifications();
    }


    public void deleteByPageId(Long pageId, UUID sessionId) {
        logger.info("Удаление индексов по ID страницы: {}", pageId);
        indexDao.deleteByPageId(pageId);
        notificationService.addNotification("Индексы удалены по ID страницы: " + pageId);
        indexingHistoryService.addRecord(sessionId,
                new IndexingHistoryRecord("Page ID: " + pageId,
                        LocalDateTime.now(), Status.DELETED));
    }


    public void deleteIndex(Index index) {
        if (index == null) {
            logger.error("Передан нулевой индекс для удаления");
            return;
        }

        logger.info("Удаление индекса: {}", index);
        indexRepository.delete(index);
    }


    public int deleteAllIndexes() {
        logger.info("Удаление всех индексов");
        int countBeforeDeletion = (int) indexRepository.count();
        indexRepository.deleteAll();
        return countBeforeDeletion;
    }


    public void cleanOldIndexes(LocalDateTime threshold) {
        logger.info("Очистка индексов старше: {}", threshold);
        indexRepository.deleteAllByLastModifiedBefore(threshold);
    }


    public long countIndexes() {
        logger.info("Запрошен подсчёт общего числа индексов");
        return indexRepository.count();
    }
}