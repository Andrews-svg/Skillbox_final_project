package com.example.searchengine.controllers.web;

import com.example.searchengine.indexing.AsyncJobService;
import com.example.searchengine.indexing.IndexServiceImpl;
import com.example.searchengine.indexing.IndexingServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.searchengine.config.Site;
import com.example.searchengine.services.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Controller
public class DefaultController {

    private static final Logger logger =
            LoggerFactory.getLogger(DefaultController.class);

    @Autowired
    private IndexingServiceImpl indexingService;

    @Autowired
    private IndexServiceImpl indexServiceImpl;

    @Autowired
    private SiteService siteService;

    @Autowired
    private PageService pageService;

    @Autowired
    private LemmaService lemmaService;

    @Autowired
    private AsyncJobService asyncJobService;

    @Autowired
    private String siteUrlRegex;


    public DefaultController(SiteService siteService,
                             PageService pageService, LemmaService lemmaService) {
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
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
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) Boolean isLemma,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String url
    ) {
        logger.info("Accessed management page with id: {}, isLemma: {}, action: {}",
                Optional.ofNullable(id).map(Object::toString).orElse("null"),
                isLemma, action);

        if ("stopIndexing".equals(action)) {
            manageStopIndexing(model);
        } else if ("startIndexing".equals(action)) {
            if (!isValidId(id)) {
                logger.error("Invalid or missing ID: {}", id);
                model.addAttribute("errorMessage",
                        "Некорректный или отсутствующий ID. Пожалуйста, проверьте ваш запрос.");
                model.addAttribute("activeTab", "management");
                return "index";
            }
            manageStartIndexing(model, id, isLemma);
        } else if ("indexPage".equals(action)) {
            manageIndexPage(model, url);
        } else if ("checkStatus".equals(action)) {
            manageCheckStatus(model, id);
        } else {
            logger.warn("Unknown action received in management: {}", action);
            model.addAttribute("errorMessage",
                    "Неизвестное действие. Пожалуйста, попробуйте снова.");
        }

        model.addAttribute("activeTab", "management");
        return "index";
    }


    private void manageCheckStatus(Model model, Integer id) {
        Object resource = siteService.findById(id);
        if (resource != null) {
            model.addAttribute("statusInfo",
                    "Статус ресурса успешно проверен.");
        } else {
            model.addAttribute("errorMessage",
                    "Ошибка при проверке статуса ресурса.");
        }
    }


    private void manageIndexPage(Model model, String url) {
        try {
            asyncJobService.indexPage(url);
            model.addAttribute("infoMessage", "Запрос на индексацию принят.");
        } catch (Exception e) {
            logger.error("Error indexing page {}: {}", url, e.getMessage(), e);
            model.addAttribute("errorMessage",
                    "Ошибка при добавлении страницы для индексации.");
        }
    }


    private void manageStopIndexing(Model model) {
        try {
            asyncJobService.stopIndexing();
            model.addAttribute("infoMessage",
                    "Процесс индексации остановлен.");
        } catch (Exception e) {
            logger.error("Ошибка при остановке индексации: {}", e.getMessage(), e);
            model.addAttribute("errorMessage",
                    "Ошибка при попытке остановить процесс индексации.");
        }
    }

    private void manageStartIndexing(Model model, Integer id, Boolean isLemma) {
        try {
            if (isLemma) {
                asyncJobService.startIndexing(id, true);
            } else {
                asyncJobService.startIndexing(id, false);
            }

            model.addAttribute("infoMessage", "Процесс индексации начат.");
        } catch (Exception e) {
            logger.error("Error starting indexing for site {}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage",
                    "Ошибка при запуске процесса индексации.");
        }
    }

    private boolean isValidId(Integer id) {
        return id != null && id > 0;
    }
}