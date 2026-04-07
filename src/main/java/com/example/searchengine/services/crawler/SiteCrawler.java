package com.example.searchengine.services.crawler;

import com.example.searchengine.config.CrawlerConfig;
import com.example.searchengine.models.Site;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Status;
import com.example.searchengine.services.PageService;
import com.example.searchengine.services.SiteService;
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
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SiteCrawler {
    private static final Logger logger = LoggerFactory.getLogger(SiteCrawler.class);
    private static final long MAX_IDLE_TIME = 300000;

    private final PageProcessor pageProcessor;
    private final LinkExtractor linkExtractor;
    private final PageService pageService;
    private final SiteService siteService;
    private final CrawlerConfig crawlerConfig;
    private final UrlFilter urlFilter;
    private final IndexingState indexingState;
    private final WatchdogService watchdogService;

    private final Map<Long, Set<String>> visitedUrls = new ConcurrentHashMap<>();
    private final Map<Long, AtomicBoolean> stopFlags = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> pageCounters = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> errorCounters = new ConcurrentHashMap<>();
    private final Map<Long, AtomicLong> lastActivityTime = new ConcurrentHashMap<>();


    @Autowired
    public SiteCrawler(PageProcessor pageProcessor,
                       LinkExtractor linkExtractor,
                       PageService pageService,
                       SiteService siteService,
                       CrawlerConfig crawlerConfig,
                       UrlFilter urlFilter,
                       IndexingState indexingState,
                       WatchdogService watchdogService) {
        this.pageProcessor = pageProcessor;
        this.linkExtractor = linkExtractor;
        this.pageService = pageService;
        this.siteService = siteService;
        this.crawlerConfig = crawlerConfig;
        this.urlFilter = urlFilter;
        this.indexingState = indexingState;
        this.watchdogService = watchdogService;
    }


    public void crawl(Site site, ForkJoinPool pool) {
        Long siteId = site.getId();
        String siteUrl = site.getUrl();
        urlFilter.clearVisitedBaseUrls();
        visitedUrls.putIfAbsent(siteId, ConcurrentHashMap.newKeySet());
        stopFlags.putIfAbsent(siteId, new AtomicBoolean(false));
        pageCounters.putIfAbsent(siteId, new AtomicInteger(0));
        errorCounters.putIfAbsent(siteId, new AtomicInteger(0));
        lastActivityTime.putIfAbsent(siteId, new AtomicLong(System.currentTimeMillis()));
        AtomicBoolean stopFlag = stopFlags.get(siteId);
        Set<String> visited = visitedUrls.get(siteId);
        AtomicInteger counter = pageCounters.get(siteId);
        AtomicInteger errorCounter = errorCounters.get(siteId);
        AtomicLong lastActivity = lastActivityTime.get(siteId);
        if (shouldStop(stopFlag, siteId)) {
            logger.info("Индексация остановлена для сайта {}", siteUrl);
            stopFlag.set(true);
            return;
        }
        long expectedPages = estimateExpectedPages(siteUrl);
        logger.info("🔥 Начало обхода сайта: {} (ID: {}), ожидаемое кол-во страниц: ~{}",
                siteUrl, siteId, expectedPages);
        watchdogService.startWatching(site, siteService, pageService, () -> {
            logger.info("🛑 Watchdog инициировал остановку для сайта {}", siteUrl);
            stopFlag.set(true);
        });
        try {
            lastActivity.set(System.currentTimeMillis());
            logProgress(site, counter, errorCounter);
            crawlPage(site, site.getUrl(), 0, visited, stopFlag, counter,
                    errorCounter, lastActivity);
            boolean quiescent = pool.awaitQuiescence(30, TimeUnit.SECONDS);
            if (!quiescent) {
                logger.warn("⚠️ Пул не успокоился за 30 секунд для сайта {}, принудительное завершение",
                        siteUrl);
                pool.shutdownNow();
            }
            checkAndFinalizeCrawling(site, siteId, stopFlag, quiescent, counter);
        } catch (Exception e) {
            logger.error("❌ Ошибка при обходе сайта {}: {}", siteUrl, e.getMessage(), e);
            siteService.updateStatusWithError(site, "Ошибка обхода: " + e.getMessage());
        } finally {
            cleanup(site, siteId);
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
        errorCounters.clear();
        lastActivityTime.clear();
        urlFilter.clearVisitedBaseUrls();
        logger.info("🧹 Ресурсы очищены для всех сайтов");
    }


    public Map<Long, Integer> getProgress() {
        Map<Long, Integer> progress = new HashMap<>();
        pageCounters.forEach((id, counter) -> progress.put(id, counter.get()));
        return progress;
    }


    public Map<Long, Integer> getErrorCounts() {
        Map<Long, Integer> errors = new HashMap<>();
        errorCounters.forEach((id, counter) -> errors.put(id, counter.get()));
        return errors;
    }


    private long estimateExpectedPages(String siteUrl) {
        if (siteUrl.contains("playback.ru")) {
            return 500;
        } else if (siteUrl.contains("skillbox.ru")) {
            return 1000;
        } else if (siteUrl.contains("lenta.ru")) {
            return 5000;
        }
        return 1000;
    }


    private void logProgress(Site site, AtomicInteger counter, AtomicInteger errorCounter) {
        long currentPages = pageService.countBySite(site);
        logger.info("📊 ТЕКУЩИЙ ПРОГРЕСС: {} - обработано {} страниц, всего в БД: {}, ошибок: {}",
                site.getUrl(), counter.get(), currentPages, errorCounter.get());
    }


    private boolean shouldStop(AtomicBoolean stopFlag, Long siteId) {
        if (stopFlag.get() || !indexingState.isActive()) {
            return true;
        }
        AtomicLong lastActivity = lastActivityTime.get(siteId);
        if (lastActivity != null && System.currentTimeMillis() - lastActivity.get() > MAX_IDLE_TIME) {
            logger.warn("⚠️ Сайт {} не проявлял активности {} минут, останавливаем",
                    siteId, MAX_IDLE_TIME / 60000);
            return true;
        }
        return false;
    }

    private void checkAndFinalizeCrawling(Site site, Long siteId, AtomicBoolean stopFlag,
                                          boolean quiescent, AtomicInteger counter) {
        if (!shouldStop(stopFlag, siteId) && quiescent) {
            long pageCount = pageService.countBySite(site);
            if (pageCount > 0 && !hasActiveTasks(siteId)) {
                logger.info("✅ Обход сайта завершен штатно: {} (обработано {} страниц, всего в БД: {})",
                        site.getUrl(), counter.get(), pageCount);
                siteService.updateStatus(site, Status.INDEXED);
            } else if (pageCount == 0) {
                logger.warn("⚠️ Обход сайта {} завершен, но не найдено ни одной страницы",
                        site.getUrl());
                siteService.updateStatusWithError(site, "Не найдено ни одной страницы");
            }
        }
    }


    private void cleanup(Site site, Long siteId) {
        logger.info("🧹 Очистка ресурсов для сайта {}", site.getUrl());
        watchdogService.stopWatching();
        visitedUrls.remove(siteId);
        stopFlags.remove(siteId);
        pageCounters.remove(siteId);
        errorCounters.remove(siteId);
        lastActivityTime.remove(siteId);
        urlFilter.clearVisitedBaseUrls();
        logger.info("🧹 Ресурсы очищены для сайта {}", site.getUrl());
    }


    private boolean hasActiveTasks(Long siteId) {
        return visitedUrls.containsKey(siteId) &&
                !ForkJoinPool.commonPool().isQuiescent();
    }


    private void crawlPage(Site site, String pageUrl, int depth,
                           Set<String> visited, AtomicBoolean stopFlag,
                           AtomicInteger counter, AtomicInteger errorCounter,
                           AtomicLong lastActivity) {
        Long siteId = site.getId();
        String siteUrl = site.getUrl();
        if (shouldStop(stopFlag, siteId)) {
            stopFlag.set(true);
            return;
        }
        lastActivity.set(System.currentTimeMillis());
        watchdogService.notifyActivity();
        if (counter.get() % 10 == 0 && counter.get() > 0) {
            logProgress(site, counter, errorCounter);
        }
        if (depth > crawlerConfig.getMaxDepth()) {
            return;
        }
        if (!isValidUrlForCrawling(pageUrl, site)) {
            return;
        }
        String baseUrl = site.getUrl();
        String path = urlFilter.normalizePath(pageUrl, baseUrl);
        if (path.equals("/error-invalid-url") || !visited.add(path)) {
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
            Optional<Page> page = future.get(crawlerConfig.getTimeout(), TimeUnit.MILLISECONDS);
            if (page.isEmpty()) {
                return;
            }
            urlFilter.addVisitedBaseUrl(pageUrl);
            siteService.updateStatusTime(site);
            processPageLinks(site, pageUrl, depth, visited, stopFlag, counter,
                    errorCounter, lastActivity, baseUrl, path);
        } catch (TimeoutException e) {
            handleTimeout(site, pageUrl, future, errorCounter, stopFlag);
        } catch (Exception e) {
            logger.error("❌ Ошибка обработки {}: {}", pageUrl, e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }


    private boolean isValidUrlForCrawling(String pageUrl, Site site) {
        if (!urlFilter.isValidForCrawling(pageUrl)) {
            logger.debug("URL невалиден для краулинга: {}", pageUrl);
            return false;
        }
        if (!urlFilter.shouldIndex(pageUrl)) {
            logger.debug("URL отклонен фильтром: {}", pageUrl);
            return false;
        }
        if (!urlFilter.isSameDomain(pageUrl, site.getUrl())) {
            return false;
        }
        return true;
    }


    private void processPageLinks(Site site, String pageUrl, int depth,
                                  Set<String> visited, AtomicBoolean stopFlag,
                                  AtomicInteger counter, AtomicInteger errorCounter,
                                  AtomicLong lastActivity, String baseUrl, String path) {
        Long siteId = site.getId();
        String siteUrl = site.getUrl();
        List<String> links = linkExtractor.extractLinks(pageUrl, siteUrl);
        logger.debug("🔗 Извлечено {} ссылок с {}", links.size(), pageUrl);
        links = limitLinksPerPage(links, pageUrl);
        if (links.isEmpty() || shouldStop(stopFlag, siteId)) {
            return;
        }
        List<CrawlTask> subtasks = createSubtasks(site, links, depth + 1, visited,
                stopFlag, counter, errorCounter, lastActivity);
        if (!subtasks.isEmpty()) {
            ForkJoinTask.invokeAll(subtasks);
        }
    }


    private List<String> limitLinksPerPage(List<String> links, String pageUrl) {
        int maxLinksPerPage = crawlerConfig.getMaxLinksPerPage();
        if (maxLinksPerPage > 0 && links.size() > maxLinksPerPage) {
            logger.debug("Страница {} содержит {} ссылок, обрабатываем только первые {}",
                    pageUrl, links.size(), maxLinksPerPage);
            return links.subList(0, maxLinksPerPage);
        }
        return links;
    }


    private List<CrawlTask> createSubtasks(Site site, List<String> links, int depth,
                                           Set<String> visited, AtomicBoolean stopFlag,
                                           AtomicInteger counter, AtomicInteger errorCounter,
                                           AtomicLong lastActivity) {
        List<CrawlTask> subtasks = new ArrayList<>();
        for (String link : links) {
            if (shouldStop(stopFlag, site.getId())) {
                stopFlag.set(true);
                break;
            }
            if (isValidLinkForCrawling(link, site, visited)) {
                subtasks.add(new CrawlTask(site, link, depth, visited,
                        stopFlag, counter, errorCounter, lastActivity));
            }
        }
        return subtasks;
    }


    private void handleTimeout(Site site, String pageUrl, Future<?> future,
                               AtomicInteger errorCounter, AtomicBoolean stopFlag) {
        String siteUrl = site.getUrl();
        int timeoutSeconds = crawlerConfig.getTimeout() / 1000;
        int errorLimit = crawlerConfig.getErrorLimit();
        logger.error("⏱ ТАЙМАУТ {} - страница не обработана за {} секунд", pageUrl, timeoutSeconds);
        if (future != null) {
            future.cancel(true);
        }
        int currentErrors = errorCounter.incrementAndGet();
        if (currentErrors > errorLimit) {
            logger.error("❌ Критическое количество таймаутов ({} > {}) для сайта {}, останавливаем обход",
                    currentErrors, errorLimit, siteUrl);
            stopFlag.set(true);
        } else {
            logger.warn("⚠️ Таймаут #{}/{} для сайта {}", currentErrors, errorLimit, siteUrl);
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
        return !visited.contains(path);
    }


    private class CrawlTask extends RecursiveAction {
        private final Site site;
        private final String url;
        private final int depth;
        private final Set<String> visited;
        private final AtomicBoolean stopFlag;
        private final AtomicInteger counter;
        private final AtomicInteger errorCounter;
        private final AtomicLong lastActivity;

        public CrawlTask(Site site, String url, int depth,
                         Set<String> visited, AtomicBoolean stopFlag,
                         AtomicInteger counter, AtomicInteger errorCounter,
                         AtomicLong lastActivity) {
            this.site = site;
            this.url = url;
            this.depth = depth;
            this.visited = visited;
            this.stopFlag = stopFlag;
            this.counter = counter;
            this.errorCounter = errorCounter;
            this.lastActivity = lastActivity;
        }

        @Override
        protected void compute() {
            crawlPage(site, url, depth, visited, stopFlag, counter,
                    errorCounter, lastActivity);
        }
    }
}