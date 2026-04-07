package com.example.searchengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "crawler")
public class CrawlerConfig {

    // ===========================================
    // ⚡ ПРОФИЛЬ №1: ИНДЕКСАЦИЯ ОДНОГО САЙТА
    // ===========================================
    private static final int SINGLE_SITE_MAX_DEPTH = 10;
    private static final int SINGLE_SITE_TIMEOUT = 20000;
    private static final int SINGLE_SITE_MAX_LINKS_PER_PAGE = 100;
    private static final int SINGLE_SITE_DELAY_MIN = 500;
    private static final int SINGLE_SITE_DELAY_MAX = 1500;
    private static final int SINGLE_SITE_ERROR_LIMIT = 30;
    private static final int SINGLE_SITE_PAGINATION_MAX = 50;
    private static final int SINGLE_SITE_POOL_SIZE = 24;
    private static final int SINGLE_SITE_QUEUE_CAPACITY = 1000;
    private static final int SINGLE_SITE_MAX_CONCURRENT_BROWSERS = 2;
    private static final long SINGLE_SITE_IDLE_TIMEOUT = 30000;
    private static final long SINGLE_SITE_CHECK_INTERVAL = 5000;
    private static final int SINGLE_SITE_MAX_ANALYSIS_ATTEMPTS = 3;

    // ===========================================
    // ⚡ ПРОФИЛЬ №2: ИНДЕКСАЦИЯ НЕСКОЛЬКИХ САЙТОВ
    // ===========================================
    private static final int MULTI_SITE_MAX_DEPTH = 8;
    private static final int MULTI_SITE_TIMEOUT = 120000;
    private static final int MULTI_SITE_MAX_LINKS_PER_PAGE = 100;
    private static final int MULTI_SITE_DELAY_MIN = 1000;
    private static final int MULTI_SITE_DELAY_MAX = 3000;
    private static final int MULTI_SITE_ERROR_LIMIT = 50;
    private static final int MULTI_SITE_PAGINATION_MAX = 30;
    private static final int MULTI_SITE_POOL_SIZE = 8;
    private static final int MULTI_SITE_QUEUE_CAPACITY = 500;
    private static final int MULTI_SITE_MAX_CONCURRENT_BROWSERS = 3;
    private static final long MULTI_SITE_IDLE_TIMEOUT = 120000;
    private static final long MULTI_SITE_CHECK_INTERVAL = 15000;
    private static final int MULTI_SITE_MAX_ANALYSIS_ATTEMPTS = 5;

    // ===========================================
    // 🔧 ЗАГРУЖАЕМЫЕ ПАРАМЕТРЫ ИЗ YAML (ТОЛЬКО JS)
    // ===========================================
    private List<String> jsEnabledSites;
    private int jsTimeout = 30000;
    private int jsWait = 5000;

    // ===========================================
    // 🔧 ТУМБЛЕР ПЕРЕКЛЮЧЕНИЯ ПРОФИЛЕЙ
    // ===========================================
    private static boolean multiSiteMode = false;

    public static void setSingleSiteMode() {
        multiSiteMode = false;
        System.out.println("🔵 РЕЖИМ: Индексация ОДНОГО сайта");
    }

    public static void setMultiSiteMode() {
        multiSiteMode = true;
        System.out.println("🟡 РЕЖИМ: Индексация НЕСКОЛЬКИХ сайтов");
    }


    public List<String> getJsEnabledSites() {
        return jsEnabledSites;
    }

    public int getJsTimeout() {
        return jsTimeout;
    }

    public int getJsWait() {
        return jsWait;
    }


    public int getMaxDepth() {
        return multiSiteMode ? MULTI_SITE_MAX_DEPTH : SINGLE_SITE_MAX_DEPTH;
    }

    public int getTimeout() {
        return multiSiteMode ? MULTI_SITE_TIMEOUT : SINGLE_SITE_TIMEOUT;
    }

    public int getMaxLinksPerPage() {
        return multiSiteMode ? MULTI_SITE_MAX_LINKS_PER_PAGE : SINGLE_SITE_MAX_LINKS_PER_PAGE;
    }

    public int getRandomDelay() {
        int min = multiSiteMode ? MULTI_SITE_DELAY_MIN : SINGLE_SITE_DELAY_MIN;
        int max = multiSiteMode ? MULTI_SITE_DELAY_MAX : SINGLE_SITE_DELAY_MAX;
        return min + (int)(Math.random() * ((max - min) + 1));
    }

    public int getErrorLimit() {
        return multiSiteMode ? MULTI_SITE_ERROR_LIMIT : SINGLE_SITE_ERROR_LIMIT;
    }

    public int getPaginationMaxPages() {
        return multiSiteMode ? MULTI_SITE_PAGINATION_MAX : SINGLE_SITE_PAGINATION_MAX;
    }

    public int getPoolSize() {
        return multiSiteMode ? MULTI_SITE_POOL_SIZE : SINGLE_SITE_POOL_SIZE;
    }

    public int getQueueCapacity() {
        return multiSiteMode ? MULTI_SITE_QUEUE_CAPACITY : SINGLE_SITE_QUEUE_CAPACITY;
    }

    public int getMaxConcurrentBrowsers() {
        return multiSiteMode ? MULTI_SITE_MAX_CONCURRENT_BROWSERS : SINGLE_SITE_MAX_CONCURRENT_BROWSERS;
    }

    public long getIdleTimeout() {
        return multiSiteMode ? MULTI_SITE_IDLE_TIMEOUT : SINGLE_SITE_IDLE_TIMEOUT;
    }

    public long getCheckInterval() {
        return multiSiteMode ? MULTI_SITE_CHECK_INTERVAL : SINGLE_SITE_CHECK_INTERVAL;
    }

    public int getMaxAnalysisAttempts() {
        return multiSiteMode ? MULTI_SITE_MAX_ANALYSIS_ATTEMPTS : SINGLE_SITE_MAX_ANALYSIS_ATTEMPTS;
    }


    public void setJsEnabledSites(List<String> jsEnabledSites) {
        this.jsEnabledSites = jsEnabledSites;
    }

    public void setJsTimeout(int jsTimeout) {
        this.jsTimeout = jsTimeout;
    }

    public void setJsWait(int jsWait) {
        this.jsWait = jsWait;
    }


    public void setMaxDepth(int maxDepth) {}
    public void setTimeout(int timeout) {}
    public void setMaxLinksPerPage(int maxLinksPerPage) {}
    public void setErrorLimit(int errorLimit) {}
    public void setPaginationMaxPages(int paginationMaxPages) {}
    public void setPoolSize(int poolSize) {}
    public void setQueueCapacity(int queueCapacity) {}
    public void setMaxConcurrentBrowsers(int maxConcurrentBrowsers) {}
    public void setIdleTimeout(long idleTimeout) {}
    public void setCheckInterval(long checkInterval) {}
    public void setMaxAnalysisAttempts(int maxAnalysisAttempts) {}
    public void setDelay(Map<String, Integer> delay) {}


    public String getCurrentMode() {
        return multiSiteMode ? "🟡 МУЛЬТИ-САЙТ" : "🔵 ОДИН САЙТ";
    }

    public void printCurrentConfig() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(getCurrentMode() + " - ТЕКУЩАЯ КОНФИГУРАЦИЯ");
        System.out.println("=".repeat(60));
        System.out.println("Глубина обхода: " + getMaxDepth());
        System.out.println("Таймаут: " + getTimeout() + " мс");
        System.out.println("Макс. ссылок на странице: " + getMaxLinksPerPage());
        System.out.println("Задержка: " + getRandomDelay() + " мс (динамическая)");
        System.out.println("Лимит ошибок: " + getErrorLimit());
        System.out.println("Макс. страниц пагинации: " + getPaginationMaxPages());
        System.out.println("Размер пула потоков: " + getPoolSize());
        System.out.println("Емкость очереди: " + getQueueCapacity());
        System.out.println("Параллельных браузеров: " + getMaxConcurrentBrowsers());
        System.out.println("Watchdog бездействие: " + getIdleTimeout() + " мс");
        System.out.println("Watchdog интервал: " + getCheckInterval() + " мс");
        System.out.println("Watchdog попыток: " + getMaxAnalysisAttempts());
        System.out.println("JS таймаут: " + jsTimeout + " мс");
        System.out.println("JS ожидание: " + jsWait + " мс");
        System.out.println("JS сайты: " + (jsEnabledSites != null ? jsEnabledSites : "[]"));
        System.out.println("=".repeat(60));
    }
}
