package com.example.searchengine.controllers.api;

import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.utils.DBSaver;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.example.searchengine.dao.LemmaDao;
import com.example.searchengine.dao.PageDao;
import com.example.searchengine.request.IndexPageRequest;
import com.example.searchengine.exceptions.*;
import com.example.searchengine.indexing.*;
import com.example.searchengine.dto.statistics.StatisticsData;
import com.example.searchengine.repository.IndexRepository;
import com.example.searchengine.services.*;
import com.example.searchengine.utils.Searcher;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final IndexingService indexingService;
    private final IndexService indexService;
    private final StatisticsService statisticsService;
    private final SearcherService searchService;
    private final Searcher searcher;
    private final LemmaDao lemmaDao;
    private final PageManager pageManager;
    private final IndexRepository indexRepository;
    private final PageService pageService;
    private final SiteManager siteManager;
    private final IndexingHistoryService indexingHistoryService;
    private final SiteService siteService;
    private final PageDao pageDao;
    private final DBSaver dbSaver;


    @Autowired
    public ApiController(
            IndexingService indexingService,
            IndexService indexService,
            StatisticsService statisticsService,
            SearcherService searchService,
            Searcher searcher,
            LemmaDao lemmaDao,
            PageManager pageManager,
            IndexRepository indexRepository,
            PageService pageService,
            SiteManager siteManager,
            IndexingHistoryService indexingHistoryService,
            SiteService siteService,
            PageDao pageDao,
            DBSaver dbSaver
    ) {
        this.indexingService = indexingService;
        this.indexService = indexService;
        this.statisticsService = statisticsService;
        this.searchService = searchService;
        this.searcher = searcher;
        this.lemmaDao = lemmaDao;
        this.pageManager = pageManager;
        this.indexRepository = indexRepository;
        this.pageService = pageService;
        this.siteManager = siteManager;
        this.indexingHistoryService = indexingHistoryService;
        this.siteService = siteService;
        this.pageDao = pageDao;
        this.dbSaver = dbSaver;
    }

    private static final Logger logger =
            LoggerFactory.getLogger(ApiController.class);


    @PostMapping("/startIndexing")
    public ResponseEntity<Map<String, String>> startIndexing(
            @RequestParam Long id,
            @RequestParam boolean isLemma) {
        if (id <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Недопустимый идентификатор сайта."));
        }
        indexingService.startIndexing(id, isLemma);
        return ResponseEntity.ok(Map.of("status", "Индексация начата."));
    }


    @PostMapping("/indexAllSites")
    public ResponseEntity<Map<String, String>> indexAllSites(
            @RequestParam Long id) {
        if (id <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Недопустимый идентификатор сайта."));
        }
        indexingService.indexAllSites(id);
        return ResponseEntity.ok(Map.of("status",
                "Индексация всех сайтов начата."));
    }


    @PostMapping("/stopIndexing")
    public ResponseEntity<Map<String, String>> stopIndexing(
            @RequestParam Long id) {
        if (id <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Недопустимый идентификатор сайта."));
        }
        indexingService.stopIndexing(id);
        return ResponseEntity.ok(Map.of("status",
                "Индексация остановлена."));
    }


    @RequestMapping(value="/indexPage", method= RequestMethod.POST)
    public ResponseEntity<Map<String,Object>> indexPage(
            @RequestBody @Validated IndexPageRequest request) throws IOException {
        logger.info("Начало индексации страницы: {}", request.getUrl());

        try {
            String content = dbSaver.fetchUrlContent(request.getUrl());

            if (StringUtils.isBlank(content)) {
                logger.warn("Пустой контент для страницы: {}", request.getUrl());
                return ResponseEntity.badRequest().body(Map.of("error", "Пустой контент"));
            }

            indexService.indexPage(request.getUrl());
            return ResponseEntity.ok(Map.of("status", "Индексация выполнена."));
        } catch (IOException e) {
            logger.error("Ошибка при индексации страницы: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при индексации страницы."));
        } catch (InvalidSiteException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    @GetMapping("/statistics")
    public ResponseEntity<Object> getStatistics() {
        logger.info("Получение статистики...");
        try {
            StatisticsData statisticsData = statisticsService.getStatistics();
            logger.info("Получение статистики: {}", statisticsData);
            return ResponseEntity.ok(Map.of("result", true,
                    "statistics", statisticsData));
        } catch (StatisticsFetchingException ex) {
            logger.error("Ошибка при получении статистики: " +
                    "{}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getIndexingStatus(
            @RequestParam Long id, @RequestParam boolean isLemma) {
        if (id <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Недопустимый идентификатор сайта."));
        }
        try {
            String status = pageManager.getIndexingStatusById(id, isLemma);
            return ResponseEntity.ok(Map.of("status", status));
        } catch (IndexingStatusFetchException ex) {
            logger.error("Ошибка при получении статуса индексации: " +
                    "{}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }
}






