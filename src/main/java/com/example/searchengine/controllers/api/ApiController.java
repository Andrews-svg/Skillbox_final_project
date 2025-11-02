package com.example.searchengine.controllers.api;

import com.example.searchengine.config.SitesList;
import com.example.searchengine.models.SearchResult;
import com.example.searchengine.utils.DBSaver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.searchengine.request.IndexPageRequest;
import com.example.searchengine.exceptions.*;
import com.example.searchengine.indexing.*;
import com.example.searchengine.dto.statistics.StatisticsData;
import com.example.searchengine.services.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    @Autowired
    private SitesList sitesList;

    private final IndexingService indexingService;
    private final IndexService indexService;
    private final StatisticsService statisticsService;
    private final DBSaver dbSaver;


    private static final Logger logger =
            LoggerFactory.getLogger(ApiController.class);


    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing() {
        try {
            indexingService.startFullIndexing();
            return ResponseEntity.ok(Map.of("result", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("result",
                    false, "error", "Индексация уже запущена"));
        }
    }


    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        try {
            indexingService.stopIndexing();
            return ResponseEntity.ok(Map.of("result", true));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("result",
                    false, "error", "Индексация не запущена"));
        }
    }



    @PostMapping("/indexPage")
    public ResponseEntity<Map<String, Object>> indexPage(@RequestBody IndexPageRequest request) {
        Map<String, Object> responseMap = new HashMap<>();

        try {
            if (!isValidUrl(request.getUrl())) {
                responseMap.put("result", false);
                responseMap.put("error", "Неверный формат адреса страницы.");
                return ResponseEntity.badRequest().body(responseMap);
            }

            if (!sitesList.isAllowedDomain(request.getUrl())) {
                responseMap.put("result", false);
                responseMap.put("error",
                        "Данная страница находится за пределами сайтов, " +
                                "указанных в конфигурационном файле");
                return ResponseEntity.badRequest().body(responseMap);
            }

            String content = dbSaver.fetchUrlContent(request.getUrl());
            if (content != null && !content.trim().isEmpty()) {
                indexService.indexPage(request.getUrl());
                responseMap.put("result", true);
                return ResponseEntity.ok(responseMap);
            } else {
                responseMap.put("result", false);
                responseMap.put("error", "Пустой контент");
                return ResponseEntity.badRequest().body(responseMap);
            }
        } catch (Exception ex) {
            responseMap.put("result", false);
            responseMap.put("error", "Возникла непредвиденная ошибка при обработке страницы.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseMap);
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


    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "20") Integer limit) {

        if (query.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false,
                            "error", "Задан пустой поисковый запрос"));
        }

        Map<String, Object> responseData = performSearch(query, site, offset, limit);

        return ResponseEntity.ok(responseData);
    }


    private Map<String, Object> performSearch(String query, String site,
                                              Integer offset, Integer limit) {

        List<SearchResult> allResults = indexService.findPagesForQuery(query);

        int totalCount = allResults.size();
        if (site != null && !site.isEmpty()) {
            allResults.removeIf(result -> !result.getSite().equals(site));
        }
        int fromIndex = Math.max(0, offset);
        int toIndex = Math.min(fromIndex + limit, allResults.size());
        List<SearchResult> resultPage = new ArrayList<>(allResults.subList(fromIndex, toIndex));

        return Map.of(
                "result", true,
                "count", totalCount,
                "data", resultPage
        );
    }
}






