package com.example.searchengine.controllers.api;

import com.example.searchengine.indexing.IndexService;
import com.example.searchengine.indexing.IndexingHistoryRecord;
import com.example.searchengine.indexing.IndexingService;
import com.example.searchengine.indexing.SiteManager;
import com.example.searchengine.models.Index;
import com.example.searchengine.models.Status;
import com.example.searchengine.services.IndexingHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/indexing")
public class IndexingApiController {

    private final IndexingService indexingService;
    private final IndexService indexService;
    private final IndexingHistoryService indexingHistoryService;
    private final SiteManager siteManager;

    private static final Logger logger =
            LoggerFactory.getLogger(IndexingApiController.class);


    @Autowired
    public IndexingApiController(IndexingService indexingService,
                                 IndexService indexService,
                                 IndexingHistoryService indexingHistoryService,
                                 SiteManager siteManager) {
        this.indexingService = indexingService;
        this.indexService = indexService;
        this.indexingHistoryService = indexingHistoryService;
        this.siteManager = siteManager;
    }


    @PostMapping("/mass-index")
    public ResponseEntity<String> massIndexing(@RequestBody Map<String, String> siteList) {
        try {
            siteManager.indexPagesFromMap(siteList);
            return ResponseEntity.ok("Массовая индексация началась успешно.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при массовой индексации: " + e.getMessage());
        }
    }


    @PostMapping
    public ResponseEntity<String> saveIndex(@Valid @RequestBody Index newIndex) {
        try {
            UUID sessionId = indexingHistoryService.startIndexingSession();
            long indexId = indexService.saveIndex(newIndex, sessionId);
            String url = indexService.getUrlFromIndex(newIndex);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    "Индекс сохранён: " + url);
        } catch (DataIntegrityViolationException ex) {
            logger.error("Ошибка целостности данных при сохранении индекса: ", ex);
            return ResponseEntity.badRequest().body(
                    "Ошибка целостности данных при сохранении индекса.");
        } catch (Exception ex) {
            logger.error("Общая ошибка при сохранении индекса: ", ex);
            return ResponseEntity.internalServerError().body(
                    "Произошла внутренняя ошибка при сохранении индекса.");
        }
    }


    @GetMapping("/check-site-by-url")
    public ResponseEntity<Boolean> checkIfSiteIsIndexedByFullName(@RequestParam String searchURL) {
        try {
            boolean result = siteManager.checkIfSiteIsIndexedByFullName(searchURL);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Ошибка при проверке сайта по URL: {}", searchURL, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    @DeleteMapping("/page/{pageId}")
    public ResponseEntity<Void> deleteByPageId(@PathVariable Long pageId) {
        try {
            UUID sessionId = indexingHistoryService.startIndexingSession();
            indexService.deleteByPageId(pageId, sessionId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error(
                    "Ошибка при удалении индекса по ID страницы {}: ",
                    pageId, e);
            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/history")
    public ResponseEntity<?> getIndexingHistory(@RequestParam UUID sessionId) {
        try {
            List<IndexingHistoryRecord> history =
                    indexService.getIndexingHistory(sessionId);
            if (history == null || history.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(history);
        } catch (DataAccessException ex) {
            logger.error("Ошибка доступа к базе данных " +
                    "при получении истории индексации: ", ex);
            return ResponseEntity.status(
                    HttpStatus.SERVICE_UNAVAILABLE).body(
                            "Нет доступа к базе данных.");
        } catch (Exception ex) {
            logger.error("Общая ошибка при получении истории индексации: ", ex);
            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR).body(
                            "Произошла общая ошибка.");
        }
    }


    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications() {
        try {
            List<String> notifications = indexService.getNotifications();
            if (notifications == null || notifications.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(notifications);
        } catch (DataAccessException ex) {
            logger.error("Ошибка доступа к данным " +
                    "при получении уведомлений: ", ex);
            return ResponseEntity.status
                    (HttpStatus.SERVICE_UNAVAILABLE).
                    body("Нет доступа к базовым данным.");
        } catch (Exception ex) {
            logger.error("Общая ошибка при получении уведомлений: ", ex);
            return ResponseEntity.status
                    (HttpStatus.INTERNAL_SERVER_ERROR).
                    body("Произошла внутренняя ошибка.");
        }
    }


    @DeleteMapping("/notifications/clear")
    public ResponseEntity<String> clearNotifications() {
        try {
            indexService.clearNotifications();
            return ResponseEntity.ok("Уведомления успешно очищены");
        } catch (DataAccessException ex) {
            logger.error("Ошибка доступа к данным при очистке уведомлений: ", ex);
            return ResponseEntity.status(
                    HttpStatus.SERVICE_UNAVAILABLE).body(
                            "Проблемы с доступом к данным " +
                                    "при очистке уведомлений.");
        } catch (IllegalStateException ex) {
            logger.error("Недопустимые условия при очистке уведомлений: ", ex);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    "Система находится в недопустимом состоянии " +
                            "для данной операции.");
        } catch (Exception ex) {
            logger.error("Общая ошибка при очистке уведомлений: ", ex);
            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR).body(
                            "Произошла внутренняя ошибка " +
                                    "при очистке уведомлений.");
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getIndex(@PathVariable Long id) {
        if (id == null || id <= 0) {
            logger.warn("Неверный идентификатор: {}", id);
            return ResponseEntity.status(
                    HttpStatus.BAD_REQUEST).body(
                            "Неверный идентификатор записи");
        }
        Optional<Index> indexOptional = indexService.findIndex(id);
        if (indexOptional.isPresent()) {
            return ResponseEntity.ok(indexOptional.get());
        } else {
            logger.info("Элемент с идентификатором {} не найден.", id);
            return ResponseEntity.status(
                    HttpStatus.NOT_FOUND).body(
                            "Элемент с указанным идентификатором не найден");
        }
    }


    @GetMapping
    public ResponseEntity<?> getAllIndexes() {
        try {
            Iterable<Index> indexes = indexService.findAllIndexes();
            return ResponseEntity.ok(indexes);
        } catch (DataAccessException ex) {
            logger.error("Ошибка доступа к данным " +
                    "при получении всех индексов: ", ex);
            return ResponseEntity.status(
                    HttpStatus.SERVICE_UNAVAILABLE).body(
                            "Проблемы с доступом к данным.");
        } catch (Exception ex) {
            logger.error("Общая ошибка при получении всех индексов: ", ex);
            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR).body(
                            "Произошла внутренняя ошибка.");
        }
    }


    @DeleteMapping
    public ResponseEntity<String> deleteAllIndexes() {
        try {
            int deletedCount = indexService.deleteAllIndexes();
            if (deletedCount > 0) {
                return ResponseEntity.ok(
                        "Все индексы удалены (" + deletedCount + ")");
            } else {
                return ResponseEntity.noContent().build();
            }
        } catch (DataAccessException ex) {
            logger.error("Ошибка доступа к данным " +
                    "при массовом удалении индексов: ", ex);
            return ResponseEntity.status(
                    HttpStatus.SERVICE_UNAVAILABLE).body(
                            "Нет доступа к данным.");
        } catch (Exception ex) {
            logger.error("Общая ошибка при массовом удалении индексов: ", ex);
            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR).body(
                            "Произошла внутренняя ошибка.");
        }
    }
}
