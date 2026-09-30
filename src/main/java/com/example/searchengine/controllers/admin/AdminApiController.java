package com.example.searchengine.controllers.admin;

import com.example.searchengine.config.CrawlerConfig;
import com.example.searchengine.dto.adminLogs.AdminAnalyticsDto;
import com.example.searchengine.dto.adminLogs.TopQueryDto;
import com.example.searchengine.dto.adminLogs.ZeroResultQueryDto;
import com.example.searchengine.services.SearchLogService;
import com.example.searchengine.services.AuthService;
import com.example.searchengine.services.HealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.searchengine.services.crawler.CrawlerService;
import java.util.Map;

import java.util.List;


@Controller
@RequestMapping("/api/admin")
public class AdminApiController {

    private static final Logger logger = LoggerFactory.getLogger(AdminApiController.class);

    private final CrawlerConfig crawlerConfig;
    private final SearchLogService searchLogService;
    private final SessionRegistry sessionRegistry;
    private final HealthService healthService;
    private final AuthService authService;
    private final CrawlerService crawlerService;


    public AdminApiController(CrawlerConfig crawlerConfig,
                              CrawlerService crawlerService,
                              SearchLogService searchLogService,
                              SessionRegistry sessionRegistry,
                              HealthService healthService,
                              AuthService authService) {
        this.crawlerConfig = crawlerConfig;
        this.crawlerService = crawlerService;
        this.searchLogService = searchLogService;
        this.sessionRegistry = sessionRegistry;
        this.healthService = healthService;
        this.authService = authService;
    }


    @GetMapping("/layout")
    public String getAdminLayout(Model model) {
        model.addAttribute("content", "");
        return "layout";
    }

    @GetMapping("/panel")
    public String getAdminPanelFragment(Model model) {
        model.addAttribute("pageTitle", "Админ панель");
        model.addAttribute("currentPage", "panel");
        model.addAttribute("currentMode", crawlerConfig.getCurrentMode());
        return "panel :: content";
    }

    @GetMapping("/modes")
    public String getModesFragment(Model model) {
        model.addAttribute("pageTitle", "Режимы индексации");
        model.addAttribute("currentPage", "modes");
        model.addAttribute("currentMode", crawlerConfig.getCurrentMode());
        return "modes :: content";
    }

    @GetMapping("/analytics")
    @ResponseBody
    public AdminAnalyticsDto getAdminAnalytics() {
        return AdminAnalyticsDto.builder()
                .topQueries(searchLogService.getTopQueries(10))
                .zeroResultQueries(searchLogService.getZeroResultQueries())
                .activeSessions(sessionRegistry.getAllPrincipals().size())
                .systemHealth(healthService.getStatus())
                .authLogs(authService.getRecentLogs(20))
                .build();
    }

    @GetMapping("/analytics/top-queries")
    @ResponseBody
    public List<TopQueryDto> getTopQueries(@RequestParam(defaultValue = "10") int limit) {
        return searchLogService.getTopQueries(limit);
    }

    @GetMapping("/analytics/zero-result")
    @ResponseBody
    public List<ZeroResultQueryDto> getZeroResultQueries() {
        return searchLogService.getZeroResultQueries();
    }

    @GetMapping("/mode")
    @ResponseBody
    public String getCurrentMode() {
        return crawlerConfig.getCurrentMode();
    }

    @GetMapping("/crawler/progress")
    @ResponseBody
    public Map<Long, Integer> getCrawlerProgress() {
        return crawlerService.getIndexingProgress();
    }

    @GetMapping("/crawler/errors")
    @ResponseBody
    public Map<Long, Integer> getCrawlerErrors() {
        return crawlerService.getIndexingErrors();
    }

    @PostMapping("/crawler/stop/{siteId}")
    @ResponseBody
    public ResponseEntity<?> stopCrawler(@PathVariable Long siteId) {
        logger.info("🛑 Ручная остановка обхода сайта ID={}", siteId);
        crawlerService.stopCrawling(siteId);
        return ResponseEntity.ok(Map.of(
                "result", true,
                "siteId", siteId,
                "message", "Обход сайта остановлен"
        ));
    }

    @PostMapping("/mode/single")
    @ResponseBody
    public String setSingleMode() {
        System.out.println("🔥 setSingleMode() вызван");
        CrawlerConfig.setSingleSiteMode();
        System.out.println("✅ Режим переключен на ОДИН сайт");
        return "Режим переключен на ОДИН сайт";
    }

    @PostMapping("/mode/multi")
    @ResponseBody
    public String setMultiMode() {
        System.out.println("🔥 setMultiMode() вызван");
        CrawlerConfig.setMultiSiteMode();
        System.out.println("✅ Режим переключен на НЕСКОЛЬКО сайтов");
        return "Режим переключен на НЕСКОЛЬКО сайтов";
    }
}