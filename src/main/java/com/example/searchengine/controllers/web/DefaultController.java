package com.example.searchengine.controllers.web;

import com.example.searchengine.config.CrawlerConfig;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DefaultController {

    private final CrawlerConfig crawlerConfig;

    public DefaultController(CrawlerConfig crawlerConfig) {
        this.crawlerConfig = crawlerConfig;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("currentMode", crawlerConfig.getCurrentMode());
        return "index";
    }
}