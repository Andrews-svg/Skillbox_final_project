package com.example.searchengine.controllers.api;

import com.example.searchengine.dto.statistics.Data;
import com.example.searchengine.exceptions.LeastFrequentLemmasProcessingException;
import com.example.searchengine.exceptions.LeastFrequentLemmasRetrievalException;
import com.example.searchengine.exceptions.SearchExecutionException;
import com.example.searchengine.exceptions.SearchIndexesException;
import com.example.searchengine.models.Index;
import com.example.searchengine.models.Lemma;
import com.example.searchengine.models.SearchResult;
import com.example.searchengine.services.SearcherService;
import com.example.searchengine.utils.Searcher;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final Logger logger =
            LoggerFactory.getLogger(SearchController.class);

    private final SearcherService searcherService;
    private final Searcher searcher;

    public SearchController(SearcherService searcherService, Searcher searcher) {

        this.searcherService = searcherService;
        this.searcher = searcher;
    }


    @GetMapping("/")
    public ResponseEntity<?> search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {

        if (query.isEmpty()) {
            logger.error("Задан пустой поисковый запрос");
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Задан пустой поисковый запрос"));
        }

        logger.info("Поисковый общий запрос: {}, сайт: {}", query, site);

        try {
            CompletableFuture<SearchResult> future = CompletableFuture.supplyAsync(() ->
                    searcher.search(query, site, offset, limit));

            return future.handle((result, throwable) -> {
                if (throwable != null) {
                    logger.error("Ошибка при асинхронном поиске: {}", throwable.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", throwable.getMessage()));
                }
                return ResponseEntity.ok(Map.of(
                        "result", true,
                        "count", result.getTotalCount(),
                        "data", result.getData()
                ));
            }).join();
        } catch (SearchExecutionException ex) {
            logger.error("Ошибка при синхронном запуске поиска: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/data")
    public ResponseEntity<?> searchData(
            @RequestParam String query,
            @RequestParam(required = false) String site) {
        if (query.isEmpty()) {
            logger.error("Задан пустой поисковый запрос на поиск информации");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Задан пустой поисковый запрос"));
        }
        logger.info("Поисковый запрос: {}, сайт: {}", query, site);
        try {

            ArrayList<Data> searchResults =
                    searcherService.getDataFromSearchInput(
                            query, site, 0, 20);
            logger.info("Результаты поиска информации: {} найдено.",
                    searchResults.size());
            return ResponseEntity.ok(Map.of(
                    "result", true,
                    "count", searchResults.size(),
                    "data", searchResults
            ));
        } catch (Exception ex) {
            logger.error("Ошибка при обработке поискового запроса: {}",
                    ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/least-frequent-lemmas-indexes")
    public ResponseEntity<?> getLeastFrequentLemmaIndexesEndpoint(
            @NotEmpty(message = "Задан пустой поисковый запрос") @RequestParam String query,
            @Pattern(regexp = "^(http://|https://).+", message = "Некорректный URL")
            @RequestParam(required = false) String site) {

        logger.info("Запрос на получение индексов наименее частых лемм для запроса: '{}', на сайте: '{}'",
                query, site);

        try {
            ArrayList<Lemma> sortedArray =
                    searcher.
                            inputToLemmasSortedArrayWithoutTooFrequentLemmas(query, site);
            List<Index> leastFrequentLemmaIndexes =
                    searcher.
                            getIndexesForLeastFrequentLemmasGroupedBySite(sortedArray);
            logger.info("Обработка запроса '{}' " +
                            "на сайте '{}' завершена: " +
                            "обнаружено {} индексов наименее частых лемм.",
                    query, site, leastFrequentLemmaIndexes.size());
            return ResponseEntity.ok(Map.of(
                    "result", true,
                    "count", leastFrequentLemmaIndexes.size(),
                    "data", leastFrequentLemmaIndexes
            ));
        } catch (LeastFrequentLemmasProcessingException ex) {
            logger.error("Ошибка при обработке запроса наименее частых лемм: {}",
                    ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }


    @GetMapping("/least-frequent-lemmas")
    public ResponseEntity<?> getLeastFrequentLemmas(
            @NotEmpty(message = "Задан пустой поисковый запрос")
            @RequestParam String query,
            @Pattern(regexp = "^(http://|https://).+", message =
                    "Некорректный URL")
            @RequestParam(required = false) String site) {
        logger.info("Запрос на получение наименее частых лемм для запроса: " +
                        "'{}', на сайте: '{}'",
                query, site);
        try {
            ArrayList<Lemma> sortedArray =
                    searcher.
                            inputToLemmasSortedArrayWithoutTooFrequentLemmas(
                            query, site);
            List<Index> leastFrequentLemmaIndexes =
                    searcher.
                            getIndexesForLeastFrequentLemmasGroupedBySite(
                            sortedArray);
            logger.info("Успешно найдено {} " +
                            "наименее частых лемм для запроса: '{}'.",
                    leastFrequentLemmaIndexes.size(), query);
            return ResponseEntity.ok(Map.of(
                    "result", true,
                    "count", leastFrequentLemmaIndexes.size(),
                    "data", leastFrequentLemmaIndexes
            ));
        } catch (LeastFrequentLemmasRetrievalException ex) {
            logger.error("Ошибка при выборе наименее частых лемм: {}",
                    ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }


    @PostMapping("/searchIndexes")
    public ResponseEntity<?> searchIndexes(@RequestBody List<Lemma> lemmaList) {
        try {
            List<Index> indexList =
                    searcher.findIndexesForSearchOutput(lemmaList);
            return ResponseEntity.ok(indexList);
        } catch (SearchIndexesException ex) {
            logger.error("Ошибка при поиске индексов: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }
}
