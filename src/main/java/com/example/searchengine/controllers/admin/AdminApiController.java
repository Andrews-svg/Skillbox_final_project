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
import com.example.searchengine.models.IndexingSession;
import com.example.searchengine.models.Site;
import com.example.searchengine.dto.adminLogs.AdminSiteDto;
import com.example.searchengine.dto.adminLogs.UserActivityDto;
import com.example.searchengine.models.statistics.UserActivityLog;
import com.example.searchengine.repositories.statistics.UserActivityRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.example.searchengine.repositories.PageRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.example.searchengine.services.VisitCounterService;
import com.example.searchengine.repositories.UserRepository;
import com.example.searchengine.services.ApplicationStartupTime;
import com.example.searchengine.services.indexing.SessionHistoryService;
import com.example.searchengine.services.SiteService;
import com.example.searchengine.services.PageService;
import com.example.searchengine.services.LemmaService;


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
    private final SessionHistoryService sessionHistoryService;
    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final PageRepository pageRepository;
    private final UserActivityRepository userActivityRepository;



    public AdminApiController(CrawlerConfig crawlerConfig,
                              CrawlerService crawlerService,
                              SearchLogService searchLogService,
                              SessionRegistry sessionRegistry,
                              HealthService healthService,
                              AuthService authService,
                              VisitCounterService visitCounterService,
                              UserRepository userRepository, ApplicationStartupTime applicationStartupTime,
                              SessionHistoryService sessionHistoryService, SiteService siteService,
                              PageService pageService, LemmaService lemmaService, PageRepository pageRepository,
                              UserActivityRepository userActivityRepository) {
        this.crawlerConfig = crawlerConfig;
        this.crawlerService = crawlerService;
        this.searchLogService = searchLogService;
        this.sessionRegistry = sessionRegistry;
        this.healthService = healthService;
        this.authService = authService;
        this.visitCounterService = visitCounterService;
        this.userRepository = userRepository;
        this.applicationStartupTime = applicationStartupTime;
        this.sessionHistoryService = sessionHistoryService;
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.pageRepository = pageRepository;
        this.userActivityRepository = userActivityRepository;
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

    @GetMapping("/sessions")
    @ResponseBody
    public List<IndexingSession> getSessions(@RequestParam(defaultValue = "20") int limit) {
        return sessionHistoryService.getRecentSessions(limit);
    }

    @GetMapping("/overview")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOverview() {
        Map<String, Object> sites = new HashMap<>();
        sites.put("total", siteService.countAll());
        sites.put("indexed", siteService.countByStatus(com.example.searchengine.models.Status.INDEXED));
        sites.put("indexing", siteService.countByStatus(com.example.searchengine.models.Status.INDEXING));
        sites.put("failed", siteService.countByStatus(com.example.searchengine.models.Status.FAILED));

        Map<String, Object> pages = new HashMap<>();
        pages.put("total", pageService.countAll());

        Map<String, Object> lemmas = new HashMap<>();
        lemmas.put("total", lemmaService.getTotalLemmas());

        Map<String, Object> mode = new HashMap<>();
        mode.put("current", crawlerConfig.getCurrentMode());
        mode.put("isMulti", crawlerConfig.isMultiSiteMode());

        Map<String, Object> indexing = new HashMap<>();
        indexing.put("active", crawlerService.isIndexing());
        indexing.put("progress", crawlerService.getIndexingProgress());
        indexing.put("errors", crawlerService.getIndexingErrors());

        Map<String, Object> users = new HashMap<>();
        users.put("activeSessions", sessionRegistry.getAllPrincipals().size());
        users.put("totalUsers", userRepository.count());

        Map<String, Object> visits = new HashMap<>();
        visits.put("total", visitCounterService.getTotalVisits());

        Map<String, Object> overview = new HashMap<>();
        overview.put("sites", sites);
        overview.put("pages", pages);
        overview.put("lemmas", lemmas);
        overview.put("mode", mode);
        overview.put("indexing", indexing);
        overview.put("users", users);
        overview.put("visits", visits);
        overview.put("uptimeSeconds", applicationStartupTime.getUptimeSeconds());

        return ResponseEntity.ok(overview);
    }

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(healthService.getStatus());
    }

    @GetMapping("/sites")
    @ResponseBody
    public List<AdminSiteDto> getSites() {
        List<Site> sites = siteService.findAll();
        List<AdminSiteDto> result = new ArrayList<>(sites.size());
        for (Site site : sites) {
            result.add(new AdminSiteDto(
                    site.getId(),
                    site.getUrl(),
                    site.getName(),
                    site.getStatus() != null ? site.getStatus().name() : "UNKNOWN",
                    site.getStatusTime(),
                    pageRepository.countBySite(site),
                    site.getLastError()
            ));
        }
        return result;
    }

    @GetMapping("/auth-log")
    @ResponseBody
    public List<UserActivityDto> getAuthLog(@RequestParam(defaultValue = "50") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        PageRequest page = PageRequest.of(0, safeLimit,
                Sort.by(Sort.Direction.DESC, "timestamp"));
        return userActivityRepository.findAll(page).stream()
                .map(a -> new UserActivityDto(
                        a.getId(),
                        a.getUserId(),
                        a.getUsername(),
                        a.getAction(),
                        a.getActionDetail(),
                        a.getIpAddress(),
                        a.getUserAgent(),
                        a.getTimestamp(),
                        a.getSuccess()
                ))
                .toList();
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
                "mode", "SINGLE",
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
                "mode", "MULTI",
                "message", "Режим переключён на MULTI"
        ));
    }
}