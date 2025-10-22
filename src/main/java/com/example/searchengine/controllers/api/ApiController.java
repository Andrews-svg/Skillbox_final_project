package com.example.searchengine.controllers.api;

import com.example.searchengine.models.SearchResult;
import com.example.searchengine.utils.DBSaver;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.example.searchengine.request.IndexPageRequest;
import com.example.searchengine.exceptions.*;
import com.example.searchengine.indexing.*;
import com.example.searchengine.dto.statistics.StatisticsData;
import com.example.searchengine.services.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final IndexingService indexingService;
    private final IndexService indexService;
    private final StatisticsService statisticsService;
    private final PageManager pageManager;
    private final DBSaver dbSaver;


    private static final Logger logger =
            LoggerFactory.getLogger(ApiController.class);


    @GetMapping("/startIndexing")
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


    @GetMapping("/stopIndexing")
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
    public ResponseEntity<Map<String, Object>> indexPage(@RequestBody @Validated IndexPageRequest request)
            throws IOException {
        logger.info("Начало индексации страницы: {}", request.getUrl());
        Map<String, Object> responseMap = new HashMap<>();

        try {
            if (!isValidUrl(request.getUrl())) {
                responseMap.put("result", false);
                responseMap.put("error", "Неверный формат адреса страницы.");
                return ResponseEntity.badRequest().body(responseMap);
            }

            String content = dbSaver.fetchUrlContent(request.getUrl());

            if (StringUtils.isBlank(content)) {
                logger.warn("Пустой контент для страницы: {}", request.getUrl());
                responseMap.put("result", false);
                responseMap.put("error", "Пустой контент");
                return ResponseEntity.badRequest().body(responseMap);
            }

            indexService.indexPage(request.getUrl());

            responseMap.put("result", true);
            return ResponseEntity.ok(responseMap);
        } catch (IOException e) {
            logger.error("Ошибка при индексации страницы: {}", e.getMessage());
            responseMap.put("result", false);
            responseMap.put("error", "Ошибка при индексации страницы.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseMap);
        } catch (InvalidSiteException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private boolean isValidUrl(String url) {
        try {
            URI uri = new URI(url);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }


    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "20") Integer limit) {


        if (query.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("result",
                    false, "error", "Задан пустой поисковый запрос"));
        }


        List<SearchResult> results = performSearch(query, site, offset, limit);

        return ResponseEntity.ok(Map.of(
                "result", true,
                "count", results.size(),
                "data", results));
    }

    private List<SearchResult> performSearch(String query, String site,
                                             Integer offset, Integer limit) {

        List<SearchResult> resultList = new ArrayList<>();

        List<SearchResult> allResults = indexService.findPagesForQuery(query);

        if (site != null && !site.isEmpty()) {
            allResults.removeIf(result -> !result.getSite().equals(site));
        }
        
        int fromIndex = Math.max(0, offset);
        int toIndex = Math.min(fromIndex + limit, allResults.size());

        for (int i = fromIndex; i < toIndex; i++) {
            resultList.add(allResults.get(i));
        }

        return resultList;
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






