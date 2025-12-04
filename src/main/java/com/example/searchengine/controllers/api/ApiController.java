package com.example.searchengine.controllers.api;

import com.example.searchengine.config.SitesList;
import com.example.searchengine.dto.statistics.response.ErrorResponseDTO;
import com.example.searchengine.models.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.searchengine.exceptions.*;
import com.example.searchengine.indexing.*;
import com.example.searchengine.dto.statistics.StatisticsData;
import com.example.searchengine.services.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;


@RestController
@RequestMapping("/api")
public class ApiController {


    private final SitesList sitesList;
    private final StatisticsService statisticsService;
    private final AsyncJobService asyncJobService;
    private final AdvancedIndexOperations advancedIndexOperations;

    private static final Logger logger =
            LoggerFactory.getLogger(ApiController.class);


    public ApiController(SitesList sitesList,
                         StatisticsService statisticsService,
                         AsyncJobService asyncJobService,
                         AdvancedIndexOperations advancedIndexOperations) {
        this.sitesList = sitesList;
        this.statisticsService = statisticsService;
        this.asyncJobService = asyncJobService;
        this.advancedIndexOperations = advancedIndexOperations;
    }


    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing() {
        try {
            asyncJobService.startFullIndexing();
            return ResponseEntity.ok().body(Map.of("result", true));
        } catch (Exception e) {
            logger.error("Ошибка при запуске индексации: {}", e.getMessage(), e);
            ErrorResponseDTO errorDto = new ErrorResponseDTO(
                    LocalDateTime.now(),
                    HttpStatus.BAD_REQUEST.value(),
                    "Индексация уже запущена"
            );
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("result", false);
            errorResponse.put("error", errorDto.getMessage());
            errorResponse.put("timestamp", errorDto.getTimestamp());
            errorResponse.put("code", errorDto.getCode());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }


    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        try {
            asyncJobService.stopIndexing();
            return ResponseEntity.ok().body(Map.of("result", true));
        } catch (IllegalStateException e) {
            logger.error("Ошибка остановки индексации: {}", e.getMessage(), e);
            ErrorResponseDTO errorDto = new ErrorResponseDTO(
                    LocalDateTime.now(),
                    HttpStatus.BAD_REQUEST.value(),
                    "Индексация не запущена"
            );
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("result", false);
            errorResponse.put("error", errorDto.getMessage());
            errorResponse.put("timestamp", errorDto.getTimestamp());
            errorResponse.put("code", errorDto.getCode());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }


    @PostMapping("/indexPage")
    public ResponseEntity<Map<String, Object>> indexPage(@RequestParam String url) {
        try {
            if (!isValidUrl(url)) {
                logger.error("Ошибка: неверный формат URL '{}'", url);
                ErrorResponseDTO errorDto = new ErrorResponseDTO(
                        LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "Неверный формат адреса страницы."
                );
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("result", false);
                errorResponse.put("error", errorDto.getMessage());
                errorResponse.put("timestamp", errorDto.getTimestamp());
                errorResponse.put("code", errorDto.getCode());
                return ResponseEntity.badRequest().body(errorResponse);
            }
            if (!sitesList.isAllowedDomain(url)) {
                logger.error("Ошибка: домен страницы '{}' не входит в разрешенные", url);
                ErrorResponseDTO errorDto = new ErrorResponseDTO(
                        LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "Данная страница находится за пределами сайтов, " +
                                "указанных в конфигурационном файле."
                );
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("result", false);
                errorResponse.put("error", errorDto.getMessage());
                errorResponse.put("timestamp", errorDto.getTimestamp());
                errorResponse.put("code", errorDto.getCode());
                return ResponseEntity.badRequest().body(errorResponse);
            }
            asyncJobService.indexPage(url);
            return ResponseEntity.ok().body(Map.of("result", true));
        } catch (Exception ex) {
            logger.error("Ошибка при индексе страницы: {}", ex.getMessage(), ex);
            ErrorResponseDTO errorDto = new ErrorResponseDTO(
                    LocalDateTime.now(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Возникла непредвиденная ошибка при обработке страницы."
            );
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("result", false);
            errorResponse.put("error", errorDto.getMessage());
            errorResponse.put("timestamp", errorDto.getTimestamp());
            errorResponse.put("code", errorDto.getCode());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
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
    public ResponseEntity<Map<String, Object>> getStatistics() {
        logger.info("Получение статистики...");
        try {
            StatisticsData statisticsData = statisticsService.getStatistics();
            logger.info("Получение статистики: {}", statisticsData);
            return ResponseEntity.ok().body(Map.of("result", true, "statistics", statisticsData));
        } catch (StatisticsFetchingException ex) {
            logger.error("Ошибка при получении статистики: {}", ex.getMessage(), ex);
            ErrorResponseDTO errorDto = new ErrorResponseDTO(
                    LocalDateTime.now(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    ex.getMessage()
            );
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("result", false);
            errorResponse.put("error", errorDto.getMessage());
            errorResponse.put("timestamp", errorDto.getTimestamp());
            errorResponse.put("code", errorDto.getCode());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "20") Integer limit) {

        if (query.isEmpty()) {
            logger.error("Ошибка: задан пустой поисковый запрос");
            ErrorResponseDTO errorDto = new ErrorResponseDTO(
                    LocalDateTime.now(),
                    HttpStatus.BAD_REQUEST.value(),
                    "Задан пустой поисковый запрос"
            );
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("result", false);
            errorResponse.put("error", errorDto.getMessage());
            errorResponse.put("timestamp", errorDto.getTimestamp());
            errorResponse.put("code", errorDto.getCode());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        try {
            Map<String, Object> responseData = performSearch(query, site, offset, limit);
            return ResponseEntity.ok(responseData);
        } catch (Exception ex) {
            logger.error("Ошибка при выполнении поиска: {}", ex.getMessage(), ex);
            ErrorResponseDTO errorDto = new ErrorResponseDTO(
                    LocalDateTime.now(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Возникла непредвиденная ошибка при поиске."
            );
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("result", false);
            errorResponse.put("error", errorDto.getMessage());
            errorResponse.put("timestamp", errorDto.getTimestamp());
            errorResponse.put("code", errorDto.getCode());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    private Map<String, Object> performSearch(String query, String site, Integer offset, Integer limit) {
        List<SearchResult> allResults = advancedIndexOperations.findPagesForQuery(query);
        if (site != null && !site.isEmpty()) {
            allResults.removeIf(result -> !result.getSite().equals(site));
        }
        int totalCount = allResults.size();
        int fromIndex = Math.max(0, offset);
        int toIndex = Math.min(fromIndex + limit, allResults.size());
        List<SearchResult> resultPage = new ArrayList<>(allResults.subList(fromIndex, toIndex));
        return Map.of("count", totalCount, "data", resultPage);
    }
}






