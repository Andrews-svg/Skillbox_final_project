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
import com.example.searchengine.services.VisitCounterService;
import com.example.searchengine.repositories.UserRepository;
import com.example.searchengine.services.ApplicationStartupTime;

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
    private final VisitCounterService visitCounterService;
    private final UserRepository userRepository;
    private final ApplicationStartupTime applicationStartupTime;



    public AdminApiController(CrawlerConfig crawlerConfig,
                              CrawlerService crawlerService,
                              SearchLogService searchLogService,
                              SessionRegistry sessionRegistry,
                              HealthService healthService,
                              AuthService authService,
                              VisitCounterService visitCounterService,
                              UserRepository userRepository, ApplicationStartupTime applicationStartupTime) {
        this.crawlerConfig = crawlerConfig;
        this.crawlerService = crawlerService;
        this.searchLogService = searchLogService;
        this.sessionRegistry = sessionRegistry;
        this.healthService = healthService;
        this.authService = authService;
        this.visitCounterService = visitCounterService;
        this.userRepository = userRepository;
        this.applicationStartupTime = applicationStartupTime;
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
                .totalVisits(visitCounterService.getTotalVisits())
                .totalUsers(userRepository.count())
                .uptimeSeconds(applicationStartupTime.getUptimeSeconds())
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
    public ResponseEntity<Map<String, Object>> getCurrentMode() {
        return ResponseEntity.ok(Map.of(
                "mode", crawlerConfig.getCurrentMode(),
                "isMulti", crawlerConfig.isMultiSiteMode()
        ));
    }

    @GetMapping("/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "mode", crawlerConfig.getCurrentMode(),
                "isMulti", crawlerConfig.isMultiSiteMode(),
                "indexing", crawlerService.isIndexing(),
                "progress", crawlerService.getIndexingProgress(),
                "errors", crawlerService.getIndexingErrors()
        ));
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
    public ResponseEntity<Map<String, Object>> setSingleMode() {
        if (crawlerService.isIndexing()) {
            logger.warn("Попытка переключить режим /mode/single во время индексации");
            return ResponseEntity.badRequest().body(Map.of(
                    "result", false,
                    "error", "Нельзя переключить режим во время индексации"
            ));
        }
        crawlerConfig.setSingleSiteMode();
        logger.info("✅ Режим переключён на SINGLE");
        return ResponseEntity.ok(Map.of(
                "result", true,
                "mode", "single",
                "message", "Режим переключён на SINGLE"
        ));
    }

    @PostMapping("/mode/multi")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> setMultiMode() {
        if (crawlerService.isIndexing()) {
            logger.warn("Попытка переключить режим /mode/multi во время индексации");
            return ResponseEntity.badRequest().body(Map.of(
                    "result", false,
                    "error", "Нельзя переключить режим во время индексации"
            ));
        }
        crawlerConfig.setMultiSiteMode();
        logger.info("✅ Режим переключён на MULTI");
        return ResponseEntity.ok(Map.of(
                "result", true,
                "mode", "multi",
                "message", "Режим переключён на MULTI"
        ));
    }
}