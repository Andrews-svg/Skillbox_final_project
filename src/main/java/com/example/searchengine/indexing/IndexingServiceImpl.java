package com.example.searchengine.indexing;

import com.example.searchengine.exceptions.IndexingStatusFetchException;
import com.example.searchengine.exceptions.InvalidSiteException;
import com.example.searchengine.models.Page;
import com.example.searchengine.repository.PageRepository;
import com.example.searchengine.repository.SiteRepository;
import com.example.searchengine.utils.DBSaver;
import com.example.searchengine.utils.UrlRecursiveParser;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.searchengine.config.SitesList;
import com.example.searchengine.config.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.repository.IndexRepository;
import com.example.searchengine.services.SiteService;
import com.example.searchengine.utils.JsoupWrapper;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Getter
@Setter
@Service
@Transactional
public class IndexingServiceImpl implements IndexingService{


    private static final Logger logger = LoggerFactory.getLogger(IndexingServiceImpl.class);

    private final IndexRepository indexRepository;
    private final UrlRecursiveParser recursiveParser;
    private final JsoupWrapper jsoupWrapper;
    private final SiteService siteService;
    private final DBSaver dbSaver;
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private List<SitesList.SiteConfig> sites;



    private static final ConcurrentHashMap<Integer, Status> indexingStatuses = new ConcurrentHashMap<>();

    private static final AtomicInteger activeIndexingThreads = new AtomicInteger(0);

    private static final CopyOnWriteArrayList<Thread> activeIndexingThreadsList =
            new CopyOnWriteArrayList<>();

    private final ExecutorService executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final Set<Integer> indexedLemmaIds = Collections.synchronizedSet(new LinkedHashSet<>());

    private static final ThreadLocal<Integer> taskIdHolder = new ThreadLocal<>();

    private volatile boolean indexingInProgress = false;

    @Autowired
    private SitesList config;


    @Autowired
    public IndexingServiceImpl(
            IndexRepository indexRepository,
            UrlRecursiveParser recursiveParser,
            JsoupWrapper jsoupWrapper,
            SiteService siteService,
            DBSaver dbSaver, SiteRepository siteRepository,
             SitesList sitesList, PageRepository pageRepository
    ) {
        this.indexRepository = indexRepository;
        this.recursiveParser = recursiveParser;
        this.jsoupWrapper = jsoupWrapper;
        this.siteService = siteService;
        this.dbSaver = dbSaver;
        this.siteRepository = siteRepository;
        this.sitesList = sitesList;
        this.pageRepository = pageRepository;
    }

    @PostConstruct
    public void init() {
        try {
            List<Integer> availablePageIds = indexRepository.findAvailablePageIds();
            logger.info("Количество доступных pageId: {}", availablePageIds.size());

            if (availablePageIds.isEmpty()) {
                ensureInitialData();
                availablePageIds = indexRepository.findAvailablePageIds();
                logger.info("После обновления количество pageId: {}", availablePageIds.size());
            }

            if (availablePageIds.isEmpty()) {
                logger.error("Не удалось создать доступные pageId. Индексация невозможна!");
            }
        } catch (Exception e) {
            logger.error("Ошибка при инициализации индексации", e);
        }
    }

    @Override
    public void ensureInitialData() {
        if (!pageRepository.existsByUrl("https://example.com")) {
            Optional<Site> maybeExampleSite = siteService.findByUrl("https://example.com");
            Site exampleSite;
            if (maybeExampleSite.isPresent()) {
                exampleSite = maybeExampleSite.get();
            } else {
                exampleSite = new Site(Status.INDEXING, LocalDateTime.now(), "https://example.com", "Example Website");

                try {
                    siteService.saveSite(exampleSite);
                    logger.info("Сайт создан: {}", exampleSite.getUrl());
                } catch (InvalidSiteException ise) {
                    logger.error("Ошибка ensureInitialData() при создании сайта: {}", ise.getMessage());
                    return;
                }
            }

            Page initialPage = new Page("/", 200,
                    "Пример содержания для индексации", exampleSite);

            pageRepository.save(initialPage);
            logger.info("Первая страница создана: {}", initialPage.getPath());
        }
    }

    @Override
    public void updateSiteStatus(int siteId, Status newStatus, String errorMessage) {
        Optional<Site> siteOptional = siteRepository.findById(siteId);
        if (siteOptional.isPresent()) {
            Site site = siteOptional.get();
            site.setStatus(newStatus);
            if (errorMessage != null && !errorMessage.isEmpty()) {
                site.setLastError(errorMessage);
            } else {
                site.setLastError(null);
            }
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        } else {
            throw new EntityNotFoundException("Сайт с ID=" + siteId + " не найден");
        }
    }

    @Override
    public void persistLink(String link) {
        String path = parsePathFromLink(link);
        Site site = resolveSiteFromLink(link);

        if (site != null && validateLink(link)) {
            Page page = new Page(path, 200, "", site);

            pageRepository.save(page);
            logger.info("Ссылка сохранена: {}", link);
        }
    }

    @Override
    public String parsePathFromLink(String link) {
        try {
            return new URI(link).getPath();
        } catch (URISyntaxException e) {
            logger.error("Ошибка разбора пути: {}", e.getMessage());
            return "/";
        }
    }

    @Override
    public Site resolveSiteFromLink(String link) {
        String host = extractHostFromLink(link);
        Optional<Site> siteOpt = siteService.findByUrl(host);
        return siteOpt.orElseGet(() -> {
            Site newSite = new Site(Status.INDEXING, LocalDateTime.now(), host, host);

            try {
                siteService.saveSite(newSite);
                return newSite;
            } catch (InvalidSiteException e) {
                logger.error("Ошибка при создании сайта: {}", e.getMessage());
                return null;
            }
        });
    }

    @Override
    public String extractHostFromLink(String link) {
        try {
            return new URI(link).getHost();
        } catch (URISyntaxException e) {
            logger.error("Ошибка разбора хостнейма: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public boolean validateLink(String link) {
        if (link.length() < 10) {
            return false;
        }

        if (!(link.startsWith("http://") || link.startsWith("https://"))) {
            return false;
        }
        if (link.endsWith(".pdf") ||
                link.endsWith(".zip") ||
                link.endsWith(".rar") ||
                link.endsWith(".exe")) {
            return false;
        }
        if (link.length() > 2048) {
            return false;
        }
        Pattern pattern =
                Pattern.compile("^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
        Matcher matcher = pattern.matcher(link);
        return matcher.matches();
    }


    @Override
    public void batchSavePages(List<Page> pages) {
        pageRepository.batchInsert(pages);
    }


    @Override
    public void prepareIndexing(int siteId) throws Exception {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new IllegalArgumentException("Сайт не найден"));
        siteRepository.deletePagesForSite(siteId);
    }


    @Override
    public void crawlAndIndex(Site site) throws Exception {
        Set<String> visitedUrls = new HashSet<>();
        Queue<String> urlsToVisit = new LinkedBlockingQueue<>();
        urlsToVisit.offer(site.getUrl());
        List<Page> pagesBatch = new ArrayList<>(100);
        while (!urlsToVisit.isEmpty()) {
            String currentUrl = urlsToVisit.poll();
            if (visitedUrls.contains(currentUrl)) continue;
            visitedUrls.add(currentUrl);
            processSinglePage(currentUrl, site, pagesBatch, urlsToVisit);
            if (pagesBatch.size() >= 100) {
                batchSavePages(pagesBatch);
                pagesBatch.clear();
            }
        }
        if (!pagesBatch.isEmpty()) {
            batchSavePages(pagesBatch);
        }
    }


    @Override
    public void processSinglePage(String currentUrl, Site site,
                                   List<Page> pagesBatch,
                                   Queue<String> urlsToVisit) throws IOException {
        Document doc = Jsoup.connect(currentUrl).get();

        Page page = createPageFromDocument(site, currentUrl, doc);
        pagesBatch.add(page);
        addLinksFromPage(site, doc, urlsToVisit);
    }


    @Override
    public Page createPageFromDocument(Site site, String path, Document doc) {
        Page page = new Page();
        page.setSite(site);
        page.setPath(path);
        page.setContent(doc.html());
        page.setCode(HttpURLConnection.HTTP_OK);
        return page;
    }


    @Override
    public void addLinksFromPage(Site site, Document doc, Queue<String> urlsToVisit) {
        Elements linksOnPage = doc.select("a[href]");
        for (Element link : linksOnPage) {
            String nextUrl = link.attr("abs:href");
            if (!nextUrl.startsWith(site.getUrl())) continue;

            ForkJoinPool.commonPool().execute(() -> {
                try {
                    urlsToVisit.offer(nextUrl);
                } catch (Exception e) {
                    logger.error("Ошибка добавления ссылки {}", nextUrl, e);
                }
            });
        }
    }


    @Override
    public void finishIndexing(int siteId, boolean isSuccess) {
        Status finalStatus = isSuccess ? Status.INDEXED : Status.FAILED;
        updateSiteStatus(siteId, finalStatus, "");
    }


    @Override
    public void indexSite(int siteId) throws Exception {
        prepareIndexing(siteId);
        Site site = siteRepository.findById(siteId).orElseThrow(() ->
                new IllegalArgumentException("Сайт не найден"));
        crawlAndIndex(site);
        finishIndexing(siteId, true);
    }



    @Override
    public boolean canStartIndexing (Integer id){
            if (id == null || id <= 0) {
                logger.warn("Недопустимый ID: {}", id);
                return false;
            }
            if (indexingStatuses.putIfAbsent(id, Status.INDEXING) != null) {
                logger.warn("Индексация уже активна для ID: {}", id);
                return false;
            }
            return true;
        }


    @Override
    public void startFullIndexing() {
        Map<Integer, SitesList.SiteConfig> configuredSites = sitesList.getSites();

        for (Map.Entry<Integer, SitesList.SiteConfig> entry : configuredSites.entrySet()) {
            Integer siteKey = entry.getKey();
            SitesList.SiteConfig siteConfig = entry.getValue();

            Optional<Site> existingSiteOptional = siteRepository.findByUrl(siteConfig.getUrl());
            existingSiteOptional.ifPresent(this::deleteEntireSiteData);

            Site newSite = generateNewSite(siteConfig.getUrl());
            siteRepository.save(newSite);

            Integer newSiteId = newSite.getId();
            startIndexing(newSiteId, false);
        }

        logger.info("Запущена полная индексация всех сайтов.");
    }


    @Override
    public Site generateNewSite(String url) {
        Site newSite = new Site();
        newSite.setUrl(url);
        newSite.setStatus(Status.INDEXING);
        newSite.setStatusTime(LocalDateTime.now());
        return newSite;
    }


    @Override
    public void startIndexing(Integer id, boolean isLemma) {
        if (!canStartIndexing(id)) {
            return;
        }
        indexingStatuses.put(id, Status.INDEXING);
        activeIndexingThreads.incrementAndGet();
        Thread currentThread = Thread.currentThread();
        activeIndexingThreadsList.add(currentThread);
        try {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
            {
                try {
                    indexSite(id);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executorService);
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    updateSiteStatus(id, Status.INDEXED, "");
                } else {
                    handleError(id, throwable);
                }
            });
        } catch (Exception e) {
            logger.error("Ошибка при запуске индексации сайта с ID {}", id, e);
            updateSiteStatus(id, Status.FAILED, "Ошибка запуска индексации");
        }
        logger.info("Индексация для ID: {} начата.", id);
    }


    @Override
    public void handleError(Integer id, Throwable t) {
        StringBuilder errorMsg = new StringBuilder("Ошибка индексации сайта ");
        if (t instanceof HttpStatusCodeException hse) {
            errorMsg.append(hse.getStatusCode().value())
                    .append(": ")
                    .append(hse.getResponseBodyAsString());
        } else if (t instanceof IOException ioex) {
            errorMsg.append(ioex.getLocalizedMessage());
        } else {
            errorMsg.append(t.getLocalizedMessage());
        }
        updateSiteStatus(id, Status.FAILED, errorMsg.toString());
    }


    @Override
    public void stopIndexing() {
        if (!isIndexingInProgress()) {
            throw new IllegalStateException("Индексация не запущена");
        }
        setIndexingStopped();
        logger.info("Процесс индексации остановлен.");
    }


    @Override
    public void processIndexingTask(Integer id) {
        try {
            Optional<Site> siteOptional = siteRepository.findById(id);
            if (siteOptional.isEmpty()) {
                logger.error("Не найден сайт с ID={}", id);
                indexingStatuses.put(id, Status.FAILED);
                finishIndexing(id, false);
                return;
            }
            Site site = siteOptional.get();
            indexSite(id);
            indexingStatuses.put(id, Status.INDEXED);
            finishIndexing(id, true);
        } catch (Exception ex) {
            indexingStatuses.put(id, Status.FAILED);
            finishIndexing(id, false);
            logger.error("Ошибка при индексе страницы с ID={}: {}", id, ex.getMessage(), ex);
        }
    }


    @Override
    public String getSiteUrlForId(Integer id) {
        Optional<Site> siteOptional = siteRepository.findById(id);
        return siteOptional.map(Site::getUrl).orElse(null);
    }


    @Override
    public boolean isIndexingInProgress() {
        return indexingInProgress;
    }


    @Override
    public void setIndexingStopped() {
        indexingInProgress = false;
    }


    @Override
    public void deleteEntireSiteData(Site site) {
        pageRepository.deleteBySite(site);
        siteRepository.delete(site);
    }


    @Override
    public String getStatus(Integer id) throws IndexingStatusFetchException {
        Optional<Site> siteOptional = siteRepository.findById(id);
        if (siteOptional.isEmpty()) {
            throw new IndexingStatusFetchException("Страница с таким ID не найдена.");
        }
        Site site = siteOptional.get();
        return site.getStatus().toString();
    }


    @Override
    public void logInfo(String message, Object... args) {
        logger.info(message, args);
    }
}
