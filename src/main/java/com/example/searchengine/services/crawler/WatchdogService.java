package com.example.searchengine.services;

import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class WatchdogService {

    private static final Logger logger = LoggerFactory.getLogger(WatchdogService.class);
    private static final long IDLE_TIMEOUT_MS = 30000;
    private static final long CHECK_INTERVAL_MS = 5000;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicLong lastActivityTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean isActive = new AtomicBoolean(true);
    private final AtomicBoolean analysisPerformed = new AtomicBoolean(false);

    private Site currentSite;
    private SiteService siteService;
    private PageService pageService;
    private Runnable onTimeoutAction;

    public void startWatching(Site site, SiteService siteService,
                              PageService pageService, Runnable onTimeout) {
        this.currentSite = site;
        this.siteService = siteService;
        this.pageService = pageService;
        this.onTimeoutAction = onTimeout;
        this.lastActivityTime.set(System.currentTimeMillis());
        this.isActive.set(true);
        this.analysisPerformed.set(false);
        logger.info("🔍 Watchdog запущен для сайта: {}", site.getUrl());
        scheduler.scheduleAtFixedRate(this::checkActivity,
                CHECK_INTERVAL_MS, CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void checkActivity() {
        if (!isActive.get() || analysisPerformed.get()) {
            return;
        }
        long idleTime = System.currentTimeMillis() - lastActivityTime.get();
        if (idleTime > IDLE_TIMEOUT_MS) {
            logger.info("⏰ Watchdog: сайт {} бездействует {} мс, запускаем анализ",
                    currentSite.getUrl(), idleTime);
            performAnalysis();
        }
    }


    private void performAnalysis() {
        analysisPerformed.set(true);
        boolean hasActiveThreads = hasActiveCrawlerThreads();
        long pageCount = pageService.countBySite(currentSite);
        logger.info("📊 Watchdog анализ для {}: страниц={}, активные потоки={}",
                currentSite.getUrl(), pageCount, hasActiveThreads);
        if (pageCount > 0 && !hasActiveThreads) {
            logger.info("✅ Watchdog: сайт {} проиндексирован ({} страниц), ставим INDEXED",
                    currentSite.getUrl(), pageCount);
            siteService.updateStatus(currentSite, Status.INDEXED);
            if (onTimeoutAction != null) {
                onTimeoutAction.run();
            }
        } else if (pageCount == 0) {
            logger.error("❌ Watchdog: сайт {} не проиндексирован, ставим FAILED", currentSite.getUrl());
            siteService.updateStatusWithError(currentSite, "Индексация не дала результатов");
        } else {
            logger.warn("⚠️ Watchdog: сайт {} еще активен ({} потоков), ждем",
                    currentSite.getUrl(), hasActiveThreads);
            analysisPerformed.set(false);
        }
    }


    private boolean hasActiveCrawlerThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .anyMatch(t -> t.getName().contains("ForkJoinPool") ||
                        t.getName().contains("CrawlTask") ||
                        t.getName().startsWith("pool-"));
    }


    public void notifyActivity() {
        lastActivityTime.set(System.currentTimeMillis());
        logger.trace("Watchdog: активность зарегистрирована");
    }


    public void stopWatching() {
        isActive.set(false);
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.debug("Watchdog остановлен для сайта {}",
                currentSite != null ? currentSite.getUrl() : "unknown");
    }
}