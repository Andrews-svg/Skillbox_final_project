package com.example.searchengine.services;

import com.example.searchengine.config.CrawlerConfig;
import com.example.searchengine.services.indexing.IndexingState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;

@Service
public class HealthService {

    private final JdbcTemplate jdbcTemplate;
    private final SiteService siteService;
    private final PageService pageService;
    private final IndexingState indexingState;
    private final CrawlerConfig crawlerConfig;

    public HealthService(JdbcTemplate jdbcTemplate,
                         SiteService siteService,
                         PageService pageService,
                         IndexingState indexingState,
                         CrawlerConfig crawlerConfig) {
        this.jdbcTemplate = jdbcTemplate;
        this.siteService = siteService;
        this.pageService = pageService;
        this.indexingState = indexingState;
        this.crawlerConfig = crawlerConfig;
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();

        // ========== СИСТЕМНЫЕ МЕТРИКИ ==========
        status.put("system", getSystemMetrics());

        // ========== БАЗА ДАННЫХ ==========
        status.put("database", getDatabaseMetrics());

        // ========== ИНДЕКСАЦИЯ ==========
        status.put("indexing", getIndexingMetrics());

        // ========== ОБЩАЯ СТАТИСТИКА ==========
        status.put("total", getTotalMetrics());

        return status;
    }

    private Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        metrics.put("totalMemory", runtime.totalMemory() / (1024 * 1024) + " MB");
        metrics.put("freeMemory", runtime.freeMemory() / (1024 * 1024) + " MB");
        metrics.put("usedMemory", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) + " MB");
        metrics.put("maxMemory", runtime.maxMemory() / (1024 * 1024) + " MB");
        metrics.put("heapMemoryUsage", memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024) + " MB");


        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        metrics.put("availableProcessors", runtime.availableProcessors());
        metrics.put("systemLoadAverage", osBean.getSystemLoadAverage());
        metrics.put("osName", osBean.getName());
        metrics.put("osVersion", osBean.getVersion());
        metrics.put("osArch", osBean.getArch());

        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptimeSeconds = runtimeBean.getUptime() / 1000;
        long uptimeMinutes = uptimeSeconds / 60;
        long uptimeHours = uptimeMinutes / 60;
        metrics.put("uptime", String.format("%d ч %d мин", uptimeHours, uptimeMinutes % 60));

        return metrics;
    }

    private Map<String, Object> getDatabaseMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            Integer dbStatus = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            metrics.put("connection", dbStatus != null && dbStatus == 1 ? "OK" : "ERROR");

            Integer tableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'search_engine'",
                    Integer.class);
            metrics.put("tablesCount", tableCount);

            Long dbSize = jdbcTemplate.queryForObject(
                    "SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) " +
                            "FROM information_schema.tables WHERE table_schema = 'search_engine'",
                    Long.class);
            metrics.put("databaseSize", dbSize + " MB");

        } catch (Exception e) {
            metrics.put("connection", "ERROR");
            metrics.put("error", e.getMessage());
        }

        return metrics;
    }

    private Map<String, Object> getIndexingMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("isActive", indexingState.isActive());
        metrics.put("currentMode", crawlerConfig.getCurrentMode());
        metrics.put("activeSitesCount", siteService.countByStatus(com.example.searchengine.models.Status.INDEXING));
        metrics.put("indexedSitesCount", siteService.countByStatus(com.example.searchengine.models.Status.INDEXED));
        metrics.put("failedSitesCount", siteService.countByStatus(com.example.searchengine.models.Status.FAILED));

        return metrics;
    }

    private Map<String, Object> getTotalMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("totalSites", siteService.countAll());
        metrics.put("totalPages", pageService.countAll());

        return metrics;
    }
}