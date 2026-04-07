package com.example.searchengine.services.crawler;

import com.example.searchengine.config.CrawlerConfig;
import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.services.PageService;
import com.example.searchengine.services.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class WatchdogService {

    private static final Logger logger = LoggerFactory.getLogger(WatchdogService.class);

    private final CrawlerConfig crawlerConfig;

    private final long IDLE_TIMEOUT_MS;
    private final long CHECK_INTERVAL_MS;
    private final int MAX_ANALYSIS_ATTEMPTS;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicLong lastActivityTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean isActive = new AtomicBoolean(true);
    private final AtomicInteger analysisAttempts = new AtomicInteger(0);

    private Site currentSite;
    private SiteService siteService;
    private PageService pageService;
    private Runnable onTimeoutAction;
    private Long siteId;


    public WatchdogService(CrawlerConfig crawlerConfig) {
        this.crawlerConfig = crawlerConfig;
        this.IDLE_TIMEOUT_MS = crawlerConfig.getIdleTimeout();
        this.CHECK_INTERVAL_MS = crawlerConfig.getCheckInterval();
        this.MAX_ANALYSIS_ATTEMPTS = crawlerConfig.getMaxAnalysisAttempts();
        logger.info("🕒 Watchdog инициализирован: таймаут={}мс, интервал={}мс, попыток={}",
                IDLE_TIMEOUT_MS, CHECK_INTERVAL_MS, MAX_ANALYSIS_ATTEMPTS);
    }


    public void startWatching(Site site, SiteService siteService,
                              PageService pageService, Runnable onTimeout) {
        this.currentSite = site;
        this.siteId = site.getId();
        this.siteService = siteService;
        this.pageService = pageService;
        this.onTimeoutAction = onTimeout;
        this.lastActivityTime.set(System.currentTimeMillis());
        this.isActive.set(true);
        this.analysisAttempts.set(0);
        logger.info("🔍 Watchdog запущен для сайта: {} (ID: {}), режим: {}",
                site.getUrl(), siteId, crawlerConfig.getCurrentMode());
        scheduler.scheduleAtFixedRate(this::checkActivity,
                CHECK_INTERVAL_MS, CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }


    private void checkActivity() {
        if (!isActive.get()) {
            return;
        }
        Site currentSiteState = siteService.findById(siteId).orElse(null);
        if (currentSiteState != null && currentSiteState.getStatus() != Status.INDEXING) {
            logger.info("🔍 Watchdog: сайт {} уже имеет статус {}, останавливаем слежение",
                    currentSite.getUrl(), currentSiteState.getStatus());
            stopWatching();
            return;
        }
        long idleTime = System.currentTimeMillis() - lastActivityTime.get();
        if (idleTime > IDLE_TIMEOUT_MS) {
            logger.info("⏰ Watchdog: сайт {} бездействует {} мс (попытка #{})",
                    currentSite.getUrl(), idleTime, analysisAttempts.incrementAndGet());
            performAnalysis();
        }
    }


    private void performAnalysis() {
        boolean hasActiveThreads = hasActiveCrawlerThreads();
        long pageCount = pageService.countBySite(currentSite);
        boolean hasErrors = hasSiteErrors();
        logger.info("📊 Watchdog анализ для {}: страниц={}, активные потоки={}, ошибки={}",
                currentSite.getUrl(), pageCount, hasActiveThreads, hasErrors);
        Site refreshedSite = siteService.findById(siteId).orElse(currentSite);
        String currentMode = crawlerConfig.getCurrentMode();
        boolean isMultiMode = currentMode.contains("МУЛЬТИ") || currentMode.contains("MULTI");
        if (pageCount > 0 && !hasActiveThreads) {
            int minPagesForSuccess = isMultiMode ? 500 : 200;
            if (pageCount < minPagesForSuccess) {
                logger.warn("⚠️ Watchdog: сайт {} имеет только {} страниц (нужно минимум {}), ждём дальше",
                        refreshedSite.getUrl(), pageCount, minPagesForSuccess);
                analysisAttempts.set(0);
                lastActivityTime.set(System.currentTimeMillis() - IDLE_TIMEOUT_MS + 10000);
                return;
            }
            logger.info("✅ Watchdog: сайт {} проиндексирован ({} страниц), ставим INDEXED",
                    refreshedSite.getUrl(), pageCount);
            siteService.updateStatus(refreshedSite, Status.INDEXED);
            if (onTimeoutAction != null) {
                onTimeoutAction.run();
            }
            stopWatching();
        } else if (pageCount == 0 && !hasActiveThreads) {
            logger.error("❌ Watchdog: сайт {} не проиндексирован, ставим FAILED",
                    refreshedSite.getUrl());
            siteService.updateStatusWithError(refreshedSite, "Индексация не дала результатов");
            stopWatching();
        } else if (analysisAttempts.get() >= MAX_ANALYSIS_ATTEMPTS) {
            logger.warn("⚠️ Watchdog: превышено количество попыток анализа ({}), принудительное завершение",
                    MAX_ANALYSIS_ATTEMPTS);
            if (pageCount > 0) {
                siteService.updateStatus(refreshedSite, Status.INDEXED);
            } else {
                siteService.updateStatusWithError(refreshedSite, "Таймаут индексации");
            }
            if (onTimeoutAction != null) {
                onTimeoutAction.run();
            }
            stopWatching();
        } else {
            logger.debug("⏳ Watchdog: ждем завершения потоков для сайта {}", refreshedSite.getUrl());
            lastActivityTime.set(System.currentTimeMillis() - IDLE_TIMEOUT_MS + 5000);
        }
    }


    private boolean hasActiveCrawlerThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .anyMatch(t -> (t.getName().contains("ForkJoinPool") ||
                        t.getName().contains("CrawlTask") ||
                        t.getName().startsWith("pool-")) &&
                        t.getState() == Thread.State.RUNNABLE);
    }


    private boolean hasSiteErrors() {
        return false;
    }


    public void notifyActivity() {
        lastActivityTime.set(System.currentTimeMillis());
        analysisAttempts.set(0);
        logger.trace("Watchdog: активность зарегистрирована для сайта {}",
                currentSite != null ? currentSite.getUrl() : "unknown");
    }


    public void stopWatching() {
        if (!isActive.getAndSet(false)) {
            return;
        }
        logger.info("🛑 Watchdog остановлен для сайта {}",
                currentSite != null ? currentSite.getUrl() : "unknown");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
