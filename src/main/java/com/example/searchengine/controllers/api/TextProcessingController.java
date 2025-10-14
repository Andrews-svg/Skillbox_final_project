package com.example.searchengine.controllers.api;

import com.example.searchengine.dto.statistics.Data;
import com.example.searchengine.request.FilterRequest;
import com.example.searchengine.request.LemmaRequest;
import com.example.searchengine.statisticsResponse.ErrorResponse;
import com.example.searchengine.exceptions.*;
import com.example.searchengine.indexing.IndexingService;
import com.example.searchengine.models.Index;
import com.example.searchengine.models.Lemma;
import com.example.searchengine.models.Page;
import com.example.searchengine.services.PageService;
import com.example.searchengine.services.RelevanceService;
import com.example.searchengine.utils.Searcher;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController
@RequestMapping("/api/text")
public class TextProcessingController {

    @Autowired
    private Searcher searcher;

    @Autowired
    private PageService pageService;

    @Autowired
    private RelevanceService relevanceService;


    @Autowired
    private IndexingService indexingService;

    private static final Logger logger =
            LoggerFactory.getLogger(TextProcessingController.class);

    @PostMapping(value = "/lemmas")
    public ResponseEntity<?> getLemmas(
            @Valid @RequestBody LemmaRequest request) {
        try {
            String input = request.getInput();
            String siteURL = request.getSiteURL();
            System.out.println("Получены данные для обработки: input='" +
                    input + "', siteURL='" + siteURL + "'");
            List<Lemma> lemmas =
                    searcher.
                            inputToLemmasSortedArrayWithoutTooFrequentLemmas(
                                    input, siteURL);
            return ResponseEntity.ok(lemmas);
        } catch (LemmasProcessingException ex) {
            System.err.println("Ошибка при получении лемм: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(ex.getMessage()));
        }
    }


    @PostMapping("/filter")
    public ResponseEntity<?> filter(
            @Valid @RequestBody FilterRequest filterRequest) {
        List<Lemma> lemmaList = filterRequest.getLemmaList();
        List<Index> indexes = filterRequest.getIndexes();
        logger.info("Получен запрос на фильтрацию: Lemma list size={}, " +
                "Index list size={}", lemmaList.size(), indexes.size());
        if (lemmaList.isEmpty()) {
            return ResponseEntity.badRequest().
                    body(Collections.singletonMap("message",
                            "Список лемм пустой"));
        }
        try {
            List<Index> filteredIndexes = searcher.filterIndexes(
                    lemmaList, indexes);
            return ResponseEntity.ok(filteredIndexes);
        } catch (FilteringException ex) {
            logger.error("Ошибка при фильтрации: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap(
                            "message", "Ошибка при фильтрации"));
        }
    }


    @PostMapping("/page-title")
    public ResponseEntity<String> getPageTitle(@RequestBody String htmlPage) {
        try {
            String title = searcher.findPageTitle(htmlPage);
            return ResponseEntity.ok(title);
        } catch (PageTitleExtractionException ex) {
            logger.error("Ошибка при извлечении заголовка страницы: " +
                    "{}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ex.getMessage());
        }
    }

    @PostMapping("/snippet")
    public ResponseEntity<String> getSnippet(
            @RequestBody Map<String, String> params) {
        String pageContent = params.get("pageContent");
        String leastFrequentLemma = params.get("leastFrequentLemma");
        try {
            return ResponseEntity.ok(searcher.findPageSnippet(
                    pageContent, leastFrequentLemma));
        } catch (SnippetGenerationException ex) {
            logger.error("Ошибка при генерации фрагмента страницы: " +
                    "{}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ex.getMessage());
        }
    }

    public ResponseEntity<Map<String, Boolean>> isIndexing() {
        logger.info("Проверка статуса индексации...");
        try {
            boolean isIndexing = indexingService.isIndexing();
            logger.info("Проверка статуса индексации: {}", isIndexing);
            return ResponseEntity.ok(Map.of("isIndexing", isIndexing));
        } catch (IndexingStatusCheckException ex) {
            logger.error("Ошибка при проверке статуса индексации: " +
                    "{}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("isIndexing", false));
        }
    }

    @GetMapping
    public ResponseEntity<?> getData(
            @RequestParam List<Long> leastFrequentLemmaIndexes,
            @RequestParam List<Long> sortedLemmaIds
    ) {
        try {
            List<Data> processedData = processData(
                    leastFrequentLemmaIndexes, sortedLemmaIds);
            return ResponseEntity.ok(processedData);
        } catch (DataProcessingException ex) {
            logger.error("Ошибка при обработке данных: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при обработке данных",
                            "details", ex.getMessage()));
        }
    }


    @PostMapping("/convert-page-to-data")
    public ResponseEntity<Data> convertPageToData(
            @Valid @RequestBody Page page) {
        logger.info("Начало преобразования страницы с урлом: {}", page.getUrl());
        Data data = convertPageToDataInternal(page);
        logger.info("Преобразование страницы завершилось успешно." +
                " Заголовок: {}, url: {}", data.getTitle(), data.getUri());
        return ResponseEntity.ok(data);
    }

    private Data convertPageToDataInternal(Page page) {
        logger.debug("Конвертируем страницу с заголовком: {}", page.getTitle());
        Data data = new Data.Builder()
                .title(page.getTitle())
                .uri(page.getUrl())
                .snippet(getPageSnippet(page.getContent()))
                .relevance(relevanceService.calculateRelevance(page))
                .build();
        logger.debug("Результат преобразования страницы: {}", data);
        return data;
    }

    private String getPageSnippet(String content) {
        String snippet = content.substring(0, Math.min(content.length(), 100)) + "...";
        logger.trace("Фрагмент страницы сформирован: {}", snippet);
        return snippet;
    }


    private List<Data> processData(
            List<Long> indexes, List<Long> lemmas) {
        return new ArrayList<>();
    }
}
