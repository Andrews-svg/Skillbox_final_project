package com.example.searchengine.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "crawler")
public class CrawlerConfig {

    // ===========================================
    // ⚡ КОНЦЕПЦИЯ РЕЖИМОВ ИНДЕКСАЦИИ
    // ===========================================
    // SINGLE — индексация ОДНОГО сайта. Настройки ПРОЩЕ:
    //          меньше глубина, меньше лимиты, короче таймауты.
    //          Обход идёт быстрее, ресурсы не распыляются.
    //
    // MULTI — индексация НЕСКОЛЬКИХ сайтов (обычно маркетплейсы
    //          и новостные порталы с разветвлённой структурой).
    //          Настройки ГЛУБЖЕ и ТЩАТЕЛЬНЕЕ:
    //          больше глубина, больше допустимо ошибок,
    //          больше попыток, дольше таймауты.
    //
    // ⚠️ SINGLE обрабатывает ТОЛЬКО ПЕРВЫЙ сайт из indexing-settings.sites.
    //    MULTI обрабатывает ВСЕ.
    // ===========================================

    // ===========================================
    // ⚡ ПРОФИЛЬ №1: ИНДЕКСАЦИЯ ОДНОГО САЙТА
    // ===========================================
    private static final int SINGLE_NETWORK_TIMEOUT_MS = 15000;
    private static final int SINGLE_PAGE_TIMEOUT_MS = 180000;
    private static final int SINGLE_JS_TIMEOUT_MS = 30000;
    private static final int SINGLE_SITE_MAX_DEPTH = 10;
    private static final int SINGLE_SITE_MAX_LINKS_PER_PAGE = 100;
    private static final int SINGLE_SITE_DELAY_MIN = 1000;
    private static final int SINGLE_SITE_DELAY_MAX = 3000;
    private static final int SINGLE_SITE_ERROR_LIMIT = 100;
    private static final int SINGLE_SITE_PAGINATION_MAX = 50;
    private static final int SINGLE_SITE_POOL_SIZE = 8;
    private static final int SINGLE_SITE_QUEUE_CAPACITY = 1000;
    private static final int SINGLE_SITE_MAX_CONCURRENT_BROWSERS = 2;
    private static final long SINGLE_SITE_IDLE_TIMEOUT = 60000;
    private static final long SINGLE_SITE_CHECK_INTERVAL = 10000;
    private static final int SINGLE_SITE_MAX_ANALYSIS_ATTEMPTS = 3;
    private static final int SINGLE_SITE_RETRY_COUNT = 3;

    // ===========================================
    // ⚡ ПРОФИЛЬ №2: ИНДЕКСАЦИЯ НЕСКОЛЬКИХ САЙТОВ
    // ===========================================
    private static final int MULTI_NETWORK_TIMEOUT_MS = 20000;
    private static final int MULTI_PAGE_TIMEOUT_MS = 240000;
    private static final int MULTI_JS_TIMEOUT_MS = 45000;
    private static final int MULTI_SITE_MAX_DEPTH = 15;
    private static final int MULTI_SITE_MAX_LINKS_PER_PAGE = 100;
    private static final int MULTI_SITE_DELAY_MIN = 2000;
    private static final int MULTI_SITE_DELAY_MAX = 5000;
    private static final int MULTI_SITE_ERROR_LIMIT = 300;
    private static final int MULTI_SITE_PAGINATION_MAX = 100;
    private static final int MULTI_SITE_POOL_SIZE = 8;
    private static final int MULTI_SITE_QUEUE_CAPACITY = 500;
    private static final int MULTI_SITE_MAX_CONCURRENT_BROWSERS = 3;
    private static final long MULTI_SITE_IDLE_TIMEOUT = 120000;
    private static final long MULTI_SITE_CHECK_INTERVAL = 15000;
    private static final int MULTI_SITE_MAX_ANALYSIS_ATTEMPTS = 5;
    private static final int MULTI_SITE_RETRY_COUNT = 5;

    // ===========================================
    // 🔧 ОБЩИЕ ПАРАМЕТРЫ ДЛЯ RETRY
    // ===========================================
    private static final int RETRY_DELAY_MS = 2000;

    // ===========================================
    // 🔧 ЗАГРУЖАЕМЫЕ ПАРАМЕТРЫ ИЗ YAML
    // ===========================================
    private List<String> jsEnabledSites;
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

    // ===========================================
    // ГЕТТЕРЫ
    // ===========================================

    public int getNetworkTimeout() {
        return multiSiteMode ? MULTI_NETWORK_TIMEOUT_MS : SINGLE_NETWORK_TIMEOUT_MS;
    }

    public List<String> getJsEnabledSites() {
        return jsEnabledSites;
    }

    public int getJsWait() {
        return jsWait;
    }

    public int getPageTimeout() {
        return multiSiteMode ? MULTI_PAGE_TIMEOUT_MS : SINGLE_PAGE_TIMEOUT_MS;
    }

    public int getJsTimeout() {
        return multiSiteMode ? MULTI_JS_TIMEOUT_MS : SINGLE_JS_TIMEOUT_MS;
    }

    public int getMaxDepth() {
        return multiSiteMode ? MULTI_SITE_MAX_DEPTH : SINGLE_SITE_MAX_DEPTH;
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

    public int getRetryCount() {
        return multiSiteMode ? MULTI_SITE_RETRY_COUNT : SINGLE_SITE_RETRY_COUNT;
    }

    public int getRetryDelay() {
        return RETRY_DELAY_MS;
    }

    // ===========================================
    // СЕТТЕРЫ (для YAML-биндинга)
    // ===========================================

    public void setJsEnabledSites(List<String> jsEnabledSites) {
        this.jsEnabledSites = jsEnabledSites;
    }

    public void setJsWait(int jsWait) {
        this.jsWait = jsWait;
    }


    // ===========================================
// 🔧 ИНИЦИАЛИЗАЦИЯ ПРИ СТАРТЕ
// ===========================================
    @PostConstruct
    public void init() {
        printCurrentConfig();
    }

    // ===========================================
    // ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ
    // ===========================================

    public String getCurrentMode() {
        return multiSiteMode ? "🟡 МУЛЬТИ-САЙТ" : "🔵 ОДИН САЙТ";
    }

    public boolean isMultiSiteMode() {
        return multiSiteMode;
    }

    public void printCurrentConfig() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(getCurrentMode() + " - ТЕКУЩАЯ КОНФИГУРАЦИЯ");
        System.out.println("=".repeat(60));
        System.out.println("Глубина обхода: " + getMaxDepth());
        System.out.println("Сетевой таймаут: " + getNetworkTimeout() + " мс");
        System.out.println("Таймаут страницы: " + getPageTimeout() + " мс");
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
        System.out.println("Количество попыток (retry): " + getRetryCount());
        System.out.println("Задержка между попытками: " + getRetryDelay() + " мс");
        System.out.println("JS таймаут: " + getJsTimeout() + " мс");
        System.out.println("JS ожидание: " + jsWait + " мс");
        System.out.println("JS сайты: " + (jsEnabledSites != null ? jsEnabledSites : "[]"));
        System.out.println("=".repeat(60));

    }
}
