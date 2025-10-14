package com.example.searchengine.controllers.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.example.searchengine.indexing.IndexingService;
import com.example.searchengine.models.Site;
import com.example.searchengine.services.*;
import com.example.searchengine.dto.statistics.Data;

import java.net.URI;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
public class DefaultController {

    private static final Logger logger =
            LoggerFactory.getLogger(DefaultController.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SiteService siteService;

    @Autowired
    private PageService pageService;

    @Autowired
    private LemmaService lemmaService;

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private SearcherService searcherService;

    @Autowired
    private String siteUrlRegex;

    @Autowired
    private String exampleDomainRegex;

    public DefaultController(SiteService siteService,
                             PageService pageService, LemmaService lemmaService,
                             IndexingService indexingService,
                             SearcherService searcherService) {
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexingService = indexingService;
        this.searcherService = searcherService;
    }



    @GetMapping("/")
    public String index(Model model, Principal principal) {
        if (principal != null) {
            logger.info("Accessed index page by user: {}", principal.getName());
            model.addAttribute("welcomeMessage",
                    "Добро пожаловать на главную страницу, " + principal.getName() + "!");
        } else {
            logger.warn("Accessed index page without authentication");
            return "redirect:/login";
        }
        return "index";
    }



    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        logger.info("Accessed dashboard");
        model.addAttribute("activeTab", "dashboard");
        model.addAttribute("totalSites", siteService.getTotalSites());
        model.addAttribute("totalPages", pageService.getTotalPages());
        model.addAttribute("totalLemmas", lemmaService.getTotalLemmas());

        Pattern pattern = Pattern.compile(siteUrlRegex);
        List<Site> allSites = siteService.findAllSites();
        List<Site> filteredSites = allSites.stream()
                .filter(site -> pattern.matcher(site.getUrl()).matches())
                .collect(Collectors.toList());

        model.addAttribute("sites", filteredSites);
        return "index";
    }



    @GetMapping("/management")
    public String management(
            Model model,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Boolean isLemma,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String url
    ) {
        logger.info("Accessed management page with id: {}, isLemma: {}, action: {}",
                Optional.ofNullable(id).map(Object::toString).
                        orElse("null"), isLemma, action);

        if (!isValidId(id)) {
            logger.error("Invalid or missing ID: {}", id);
            model.addAttribute("errorMessage",
                    "Некорректный или отсутствующий ID. Пожалуйста, проверьте ваш запрос.");
            model.addAttribute("activeTab", "management");
            return "index";
        }

        if (isLemma == null) {
            logger.warn("isLemma parameter is not provided. Defaulting to false.");
            isLemma = false;
        }
        model.addAttribute("activeTab", "management");

        switch (action) {
            case "startIndexing":
                manageStartIndexing(model, id, isLemma);
                break;
            case "stopIndexing":
                manageStopIndexing(model, id);
                break;
            case "indexPage":
                manageIndexPage(model, url);
                break;
            case "checkStatus":
                manageCheckStatus(model, id);
                break;
            default:
                logger.warn("Unknown action received in management: {}", action);
                model.addAttribute("errorMessage",
                        "Неизвестное действие. Пожалуйста, попробуйте снова.");
        }
        return "index";
    }


    private boolean isValidId(Long id) {
        return id != null && id > 0 && id <= Integer.MAX_VALUE;
    }

    private void manageStartIndexing(Model model, Long id, Boolean isLemma) {
        logger.info("Initiating indexing for ID: {} with isLemma: {}", id, isLemma);
        try {
            URI uri = UriComponentsBuilder.fromUriString(
                            "http://localhost:8080/api/startIndexing")
                    .queryParam("id", id)
                    .queryParam("isLemma", isLemma)
                    .build().toUri();
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST,
                    null, String.class);
            model.addAttribute("indexingStatus", response.getBody());
            logger.info("Индексация для ID: {} успешно инициирована.", id);
        } catch (Exception e) {
            logger.error("Ошибка при запуске индексации для ID: {}: {}",
                    id, e.getMessage(), e);
            model.addAttribute("errorMessage",
                    "Ошибка при запуске индексации.");
        }
    }

    private void manageStopIndexing(Model model, Long id) {
        logger.info("Stopping indexing for ID: {}", id);
        try {
            URI uri = UriComponentsBuilder.
                    fromUriString("http://localhost:8080/api/stopIndexing")
                    .queryParam("id", id)
                    .build().toUri();

            ResponseEntity<String> response = restTemplate.exchange(uri,
                    HttpMethod.POST, null, String.class);
            model.addAttribute("indexingStatus", response.getBody());
        } catch (Exception e) {
            logger.error("Ошибка при остановке индексации для ID: " +
                    "{}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage",
                    "Ошибка при остановке индексации.");
        }
    }


    private void manageIndexPage(Model model, String url) {
        logger.info("Indexing page with URL: {}", url);
        try {
            URI uri = UriComponentsBuilder.
                    fromUriString("http://localhost:8080/api/indexPage")
                    .queryParam("url", url)
                    .build().toUri();

            ResponseEntity<String> response = restTemplate.exchange(uri,
                    HttpMethod.POST, null, String.class);
            model.addAttribute("indexingStatus", response.getBody());
            logger.info("Страница с URL: {} успешно обработана.", url);
        } catch (Exception e) {
            logger.error("Ошибка при обработке страницы с URL: " +
                    "{}: {}", url, e.getMessage(), e);
            model.addAttribute("errorMessage",
                    "Ошибка при добавлении/обновлении страницы.");
        }
    }

    private void manageCheckStatus(Model model, Long id) {
        logger.info("Checking indexing status for ID: {}", id);
        try {
            URI uri = UriComponentsBuilder.
                    fromUriString("http://localhost:8080/api/isIndexing")
                    .queryParam("id", id)
                    .build().toUri();

            ResponseEntity<String> response = restTemplate.exchange(uri,
                    HttpMethod.GET, null, String.class);
            model.addAttribute("indexingStatus", response.getBody());
        } catch (Exception e) {
            logger.error("Ошибка при проверке состояния индексации для ID: " +
                    "{}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage",
                    "Ошибка при получении статуса индексации.");
        }
    }

    @GetMapping("/search")
    public String searchPage(Model model) {
        logger.info("Accessed search page");
        model.addAttribute("activeTab", "search");
        Pattern pattern = Pattern.compile(exampleDomainRegex);
        List<Site> allSites = siteService.findAllSites();
        List<Site> filteredSites = allSites.stream()
                .filter(site -> pattern.matcher(site.getUrl()).matches())
                .collect(Collectors.toList());
        model.addAttribute("sites", filteredSites);
        return "index";
    }


    @PostMapping("/auth/search")
    public String performSearch(
            @RequestParam String query,
            @RequestParam String site,
            Model model
    ) {
        logger.info("Performing search with query: '{}' on site: '{}'", query, site);
        if (!query.trim().isEmpty()) {
            ArrayList<Data> results =
                    searcherService.getDataFromSearchInput(query, site, 0, 10);
            Long totalCount = searcherService.getTotalCount(query, site);

            model.addAttribute("results", results);
            model.addAttribute("query", query);
            model.addAttribute("selectedSite", site);
            model.addAttribute("totalCount", totalCount);
        } else {
            model.addAttribute("errorMessage",
                    "Запрос не может быть пустым");
            return "index";
        }
        logger.info("Search completed. Total results: {}",
                model.asMap().get("totalCount"));
        return "index";
    }
}