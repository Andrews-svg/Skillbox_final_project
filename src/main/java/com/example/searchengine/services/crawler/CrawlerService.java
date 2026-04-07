package com.example.searchengine.services;

import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;
import com.example.searchengine.services.indexing.IndexingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;


@Service
public class CrawlerService {

    private static final Logger logger = LoggerFactory.getLogger(CrawlerService.class);

    private final SiteService siteService;
    private final SiteCrawler siteCrawler;
    private final PageProcessor pageProcessor;
    private final IndexingState indexingState;


    private final ConcurrentHashMap<Long, ForkJoinPool> sitePools = new ConcurrentHashMap<>();


    public CrawlerService(SiteService siteService,
                          SiteCrawler siteCrawler,
                          PageProcessor pageProcessor, IndexingState indexingState) {
        this.siteService = siteService;
        this.siteCrawler = siteCrawler;
        this.pageProcessor = pageProcessor;
        this.indexingState = indexingState;
    }


    public void crawlSite(Site site) {
        if (!indexingState.isActive()) {
            logger.warn("Индексация не активна, но crawlSite вызван для {}", site.getUrl());
            return;
        }
        logger.info("🚀 Запуск обхода сайта: {}", site.getUrl());
        int processors = Runtime.getRuntime().availableProcessors();
        int poolSize = Math.min(24, Math.max(4, processors * 2));
        ForkJoinPool pool = new ForkJoinPool(
                poolSize,
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                (t, e) -> logger.error("Ошибка в потоке {} сайта {}: {}",
                        t.getName(), site.getUrl(), e.getMessage()),
                false
        );
        sitePools.put(site.getId(), pool);
        try {
            siteCrawler.crawl(site, pool);
        } catch (Exception e) {
            logger.error("❌ Ошибка при обходе сайта {}: {}", site.getUrl(), e.getMessage(), e);
            siteService.updateStatusWithError(site, "Ошибка обхода: " + e.getMessage());
        } finally {
            shutdownPool(site.getId(), pool);
            sitePools.remove(site.getId());
        }
    }


    public boolean indexPage(Site site, String pageUrl) {
        logger.info("📥 Ручная индексация страницы: {}", pageUrl);
        try {
            Optional<Page> page = pageProcessor.processPage(site, pageUrl);

            if (page.isPresent()) {
                logger.info("✅ Страница успешно проиндексирована: {}", pageUrl);
                return true;
            } else {
                logger.warn("❌ Не удалось проиндексировать страницу: {}", pageUrl);
                return false;
            }
        } catch (Exception e) {
            logger.error("🔥 Ошибка при индексации страницы {}: {}", pageUrl, e.getMessage(), e);
            return false;
        }
    }


    public void stopAllCrawling() {
        logger.info("⛔ Остановка всех обходов...");
        indexingState.setActive(false);
        siteCrawler.stopAllCrawling();
        sitePools.forEach(this::shutdownPool);
        sitePools.clear();
    }



    public void stopCrawling(Long siteId) {
        logger.info("⛔ Остановка обхода сайта ID: {}", siteId);
        siteCrawler.stopCrawling(siteId);
        ForkJoinPool pool = sitePools.get(siteId);
        if (pool != null) {
            shutdownPool(siteId, pool);
            sitePools.remove(siteId);
        }
    }


    public Map<Long, Integer> getIndexingProgress() {
        return siteCrawler.getProgress();
    }


    private void shutdownPool(Long siteId, ForkJoinPool pool) {
        if (pool == null || pool.isShutdown()) return;
        pool.shutdown();
        try {
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Пул для сайта {} не завершился за 30 сек, принудительное завершение", siteId);
                pool.shutdownNow();
                if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.error("НЕ УДАЛОСЬ ОСТАНОВИТЬ ПУЛ для сайта {}", siteId);
                }
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}