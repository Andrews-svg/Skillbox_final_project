package com.example.searchengine.controllers.admin;

import com.example.searchengine.config.CrawlerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class IndexController {

    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);

    private final CrawlerConfig crawlerConfig;

    public IndexController(CrawlerConfig crawlerConfig) {
        this.crawlerConfig = crawlerConfig;
    }

    @GetMapping
    public String adminDashboard(Model model) {
        model.addAttribute("pageTitle", "Дашборд");
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("currentMode", crawlerConfig.getCurrentMode());
        model.addAttribute("content", "dashboard :: content");
        return "layout";
    }

    @GetMapping("/modes")
    public String adminModes(Model model) {
        model.addAttribute("pageTitle", "Режимы индексации");
        model.addAttribute("currentPage", "modes");
        model.addAttribute("currentMode", crawlerConfig.getCurrentMode());
        model.addAttribute("content", "modes :: content");
        return "layout";
    }

    @GetMapping("/mode")
    @ResponseBody
    public String getCurrentMode() {
        return crawlerConfig.getCurrentMode();
    }

    @PostMapping("/mode/single")
    @ResponseBody
    public String setSingleMode() {
        CrawlerConfig.setSingleSiteMode();
        logger.info("🔵 Переключен режим: ОДИН сайт");
        return "Режим переключен на ОДИН сайт";
    }

    @PostMapping("/mode/multi")
    @ResponseBody
    public String setMultiMode() {
        CrawlerConfig.setMultiSiteMode();
        logger.info("🟡 Переключен режим: НЕСКОЛЬКО сайтов");
        return "Режим переключен на НЕСКОЛЬКО сайтов";
    }
}