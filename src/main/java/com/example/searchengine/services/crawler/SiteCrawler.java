package com.example.searchengine.services;

import com.example.searchengine.config.CrawlerConfig;
import com.example.searchengine.models.Site;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Status;
import com.example.searchengine.services.indexing.IndexingState;
import com.example.searchengine.utils.UrlFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SiteCrawler {

    private static final Logger logger = LoggerFactory.getLogger(SiteCrawler.class);

    private final PageProcessor pageProcessor;
    private final LinkExtractor linkExtractor;
    private final PageService pageService;
    private final SiteService siteService;
    private final CrawlerConfig crawlerConfig;
    private final UrlFilter urlFilter;
    private final IndexingState indexingState;

    private final Map<Long, Set<String>> visitedUrls = new ConcurrentHashMap<>();
    private final Map<Long, AtomicBoolean> stopFlags = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> pageCounters = new ConcurrentHashMap<>();

    @Autowired
    public SiteCrawler(PageProcessor pageProcessor,
                       LinkExtractor linkExtractor,
                       PageService pageService,
                       SiteService siteService,
                       CrawlerConfig crawlerConfig,
                       UrlFilter urlFilter,
                       IndexingState indexingState) {
        this.pageProcessor = pageProcessor;
        this.linkExtractor = linkExtractor;
        this.pageService = pageService;
        this.siteService = siteService;
        this.crawlerConfig = crawlerConfig;
        this.urlFilter = urlFilter;
        this.indexingState = indexingState;
    }


    private boolean shouldStop(AtomicBoolean stopFlag) {
        return stopFlag.get() || !indexingState.isActive();
    }


    public void crawl(Site site, ForkJoinPool pool) {
        Long siteId = site.getId();
        urlFilter.clearVisitedBaseUrls();
        visitedUrls.putIfAbsent(siteId, ConcurrentHashMap.newKeySet());
        stopFlags.putIfAbsent(siteId, new AtomicBoolean(false));
        pageCounters.putIfAbsent(siteId, new AtomicInteger(0));
        AtomicBoolean stopFlag = stopFlags.get(siteId);
        Set<String> visited = visitedUrls.get(siteId);
        AtomicInteger counter = pageCounters.get(siteId);
        if (shouldStop(stopFlag)) {
            logger.info("Индексация остановлена для сайта {}", site.getUrl());
            stopFlag.set(true);
            return;
        }
        logger.info("🔥 Начало обхода сайта: {} (ID: {})", site.getUrl(), siteId);
        try {
            crawlPage(site, site.getUrl(), 0, visited, stopFlag, counter);
            logger.info("✅ Обход сайта завершен: {} (обработано {} страниц)",
                    site.getUrl(), counter.get());
            if (!shouldStop(stopFlag)) {
                siteService.updateStatus(site, Status.INDEXED);
            }
        } catch (Exception e) {
            logger.error("❌ Ошибка при обходе сайта {}: {}", site.getUrl(), e.getMessage(), e);
            siteService.updateStatusWithError(site, "Ошибка обхода: " + e.getMessage());
        } finally {
            visitedUrls.remove(siteId);
            stopFlags.remove(siteId);
            pageCounters.remove(siteId);
            urlFilter.clearVisitedBaseUrls();
        }
    }


    private void crawlPage(Site site, String pageUrl, int depth,
                           Set<String> visited, AtomicBoolean stopFlag,
                           AtomicInteger counter) {
        if (shouldStop(stopFlag)) {
            stopFlag.set(true);
            return;
        }
        if (depth > crawlerConfig.getMaxDepth()) {
            return;
        }
        if (!urlFilter.isValidForCrawling(pageUrl)) {
            logger.debug("URL невалиден для краулинга: {}", pageUrl);
            return;
        }
        if (!urlFilter.shouldIndex(pageUrl)) {
            logger.debug("URL отклонен фильтром: {}", pageUrl);
            return;
        }
        String baseUrl = site.getUrl();
        if (!urlFilter.isSameDomain(pageUrl, baseUrl)) {
            return;
        }
        String path = urlFilter.normalizePath(pageUrl, baseUrl);
        if (path.equals("/error-invalid-url")) {
            return;
        }
        if (!visited.add(path)) {
            return;
        }
        if (pageService.existsByPathAndSite(path, site)) {
            return;
        }
        logger.info("📄 Обработка страницы [{}] {} (глубина {})",
                counter.incrementAndGet(), pageUrl, depth);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Optional<Page>> future = null;
        try {
            future = executor.submit(() -> pageProcessor.processPage(site, pageUrl));
            Optional<Page> page = future.get(20, TimeUnit.SECONDS);
            if (page.isEmpty()) {
                return;
            }
            urlFilter.addVisitedBaseUrl(pageUrl);
            siteService.updateStatusTime(site);
            List<String> links = linkExtractor.extractLinks(pageUrl, site.getUrl());
            int maxLinksPerPage = crawlerConfig.getMaxLinksPerPage();
            if (links.size() > maxLinksPerPage) {
                logger.debug("Страница {} содержит {} ссылок, обрабатываем только первые {}",
                        pageUrl, links.size(), maxLinksPerPage);
                links = links.subList(0, maxLinksPerPage);
            }
            if (links.isEmpty() || shouldStop(stopFlag)) {
                return;
            }
            List<CrawlTask> subtasks = new ArrayList<>();
            for (String link : links) {
                if (shouldStop(stopFlag)) {
                    stopFlag.set(true);
                    break;
                }
                if (isValidLinkForCrawling(link, site, visited)) {
                    subtasks.add(new CrawlTask(site, link, depth + 1, visited, stopFlag, counter));
                }
            }
            if (!subtasks.isEmpty()) {
                ForkJoinTask.invokeAll(subtasks);
            }
        } catch (TimeoutException e) {
            logger.error("⏱ ТАЙМАУТ {} - страница не обработана за 20 секунд", pageUrl);
            future.cancel(true);
        } catch (Exception e) {
            logger.error("❌ Ошибка обработки {}: {}", pageUrl, e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }


    private boolean isValidLinkForCrawling(String link, Site site, Set<String> visited) {
        String baseUrl = site.getUrl();
        if (!urlFilter.isSameDomain(link, baseUrl)) {
            return false;
        }
        if (!urlFilter.isValidForCrawling(link)) {
            return false;
        }
        if (!urlFilter.shouldIndex(link)) {
            return false;
        }
        String path = urlFilter.normalizePath(link, baseUrl);
        if (visited.contains(path)) {
            return false;
        }
        return true;
    }


    private class CrawlTask extends RecursiveAction {
        private final Site site;
        private final String url;
        private final int depth;
        private final Set<String> visited;
        private final AtomicBoolean stopFlag;
        private final AtomicInteger counter;

        public CrawlTask(Site site, String url, int depth,
                         Set<String> visited, AtomicBoolean stopFlag,
                         AtomicInteger counter) {
            this.site = site;
            this.url = url;
            this.depth = depth;
            this.visited = visited;
            this.stopFlag = stopFlag;
            this.counter = counter;
        }

        @Override
        protected void compute() {
            crawlPage(site, url, depth, visited, stopFlag, counter);
        }
    }

    public void stopCrawling(Long siteId) {
        AtomicBoolean stopFlag = stopFlags.get(siteId);
        if (stopFlag != null) {
            stopFlag.set(true);
            logger.info("⛔ Остановлен обход сайта ID: {}", siteId);
        }
    }

    public void stopAllCrawling() {
        logger.info("⛔ Остановка всех обходов...");
        stopFlags.values().forEach(flag -> flag.set(true));
        stopFlags.clear();
        visitedUrls.clear();
        pageCounters.clear();
        urlFilter.clearVisitedBaseUrls();
    }

    public Map<Long, Integer> getProgress() {
        Map<Long, Integer> progress = new HashMap<>();
        pageCounters.forEach((id, counter) -> progress.put(id, counter.get()));
        return progress;
    }
}