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
}