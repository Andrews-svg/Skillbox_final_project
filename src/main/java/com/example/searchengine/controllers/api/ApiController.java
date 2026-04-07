package com.example.searchengine.controllers.api;

import com.example.searchengine.config.SitesList;
import com.example.searchengine.dto.search.SearchResponse;
import com.example.searchengine.dto.statistics.responses.StatisticsResponse;
import com.example.searchengine.dto.statistics.responses.StatisticsData;
import com.example.searchengine.services.*;
import com.example.searchengine.services.indexing.IndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;


@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private final SitesList sitesList;
    private final StatisticsService statisticsService;
    private final SiteValidator siteValidator;
    private final IndexingService indexingService;
    private final SearchService searchService;

    public ApiController(SitesList sitesList,
                         StatisticsService statisticsService,
                         SiteValidator siteValidator,
                         IndexingService indexingService,
                         SearchService searchService) {
        this.sitesList = sitesList;
        this.statisticsService = statisticsService;
        this.siteValidator = siteValidator;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }


    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        try {
            indexingService.startFullIndexing();
            return ResponseEntity.ok(Map.of("result", true));
        } catch (IllegalStateException e) {
            logger.error("Ошибка запуска индексации: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    Map.of("result", false, "error", "Индексация уже запущена")
            );
        }
    }


    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        try {
            indexingService.stopIndexing();
            return ResponseEntity.ok(Map.of("result", true));
        } catch (IllegalStateException e) {
            logger.error("Ошибка остановки индексации: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    Map.of("result", false, "error", "Индексация не запущена")
            );
        }
    }


    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam String url) {
        if (!isValidUrl(url)) {
            return ResponseEntity.badRequest().body(
                    Map.of("result", false, "error", "Неверный формат адреса страницы")
            );
        }
        if (!siteValidator.isAllowedDomain(url, sitesList)) {
            return ResponseEntity.badRequest().body(
                    Map.of("result", false,
                            "error", "Данная страница находится за пределами сайтов, " +
                                    "указанных в конфигурационном файле")
            );
        }
        try {
            boolean success = indexingService.indexPage(url);
            if (success) {
                return ResponseEntity.ok(Map.of("result", true));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        Map.of("result", false, "error", "Не удалось проиндексировать страницу")
                );
            }
        } catch (Exception e) {
            logger.error("Ошибка индексации страницы {}: {}", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("result", false, "error", "Внутренняя ошибка сервера")
            );
        }
    }


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> getStatistics() {
        try {
            StatisticsData statisticsData = statisticsService.getStatistics();
            return ResponseEntity.ok(new StatisticsResponse(true, statisticsData));
        } catch (Exception e) {
            logger.error("Ошибка получения статистики: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("result", false, "error", "Задан пустой поисковый запрос")
            );
        }
        try {
            SearchResponse response = searchService.search(query, site, offset, limit);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Ошибка поиска: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    Map.of("result", false, "error", e.getMessage())
            );
        } catch (Exception e) {
            logger.error("Внутренняя ошибка поиска: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("result", false, "error", "Внутренняя ошибка сервера")
            );
        }
    }


    private boolean isValidUrl(String url) {
        try {
            URI uri = new URI(url);
            return uri.getScheme() != null &&
                    uri.getHost() != null &&
                    (uri.getScheme().equals("http") || uri.getScheme().equals("https"));
        } catch (URISyntaxException e) {
            return false;
        }
    }
}