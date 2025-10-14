package com.example.searchengine.indexing;

import com.example.searchengine.exceptions.InvalidSiteException;
import com.example.searchengine.models.Page;
import com.example.searchengine.repository.PageRepository;
import com.example.searchengine.utils.DBSaver;
import com.example.searchengine.utils.UrlRecursiveParser;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.searchengine.config.SiteSettings;
import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.repository.IndexRepository;
import com.example.searchengine.services.SiteService;
import com.example.searchengine.utils.JsoupWrapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class IndexingService {

    private final IndexRepository indexRepository;
    private volatile Status currentStatus = Status.PENDING;
    private CountDownLatch latch;

    private final UrlRecursiveParser recursiveParser;
    private final ReentrantLock lock = new ReentrantLock();
    private final JsoupWrapper jsoupWrapper;
    private final SiteService siteService;
    private final DBSaver dbSaver;
    private final SiteIndexer siteIndexer;
    private final SiteManager siteManager;
    private final LinkProcessor linkProcessor;
    private final SiteSettings siteSettings;
    private final IndexService indexService;
    private List<SiteSettings.SiteConfig> sites;
    private final Map<Long, Boolean> indexingInProgress =
            new ConcurrentHashMap<>();
    private final AtomicInteger activeIndexingThreads =
            new AtomicInteger(0);
    private final List<Thread> activeIndexingThreadsList =
            new ArrayList<>();
    private Long currentIndexingLemmaId;
    private Set<Long> indexedLemmaIds = ConcurrentHashMap.newKeySet();
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static final Logger logger =
            LoggerFactory.getLogger(IndexingService.class);

    @Autowired
    private PageRepository pageRepository;



    @Autowired
    public IndexingService(
            UrlRecursiveParser recursiveParser,
            JsoupWrapper jsoupWrapper,
            SiteService siteService, DBSaver dbSaver,
            SiteIndexer siteIndexer,
            SiteManager siteManager,
            LinkProcessor linkProcessor,
            SiteSettings siteSettings,
            IndexRepository indexRepository,
            IndexService indexService
    ) {
        this.recursiveParser = recursiveParser;
        this.jsoupWrapper = jsoupWrapper;
        this.siteService = siteService;
        this.dbSaver = dbSaver;
        this.siteIndexer = siteIndexer;
        this.siteManager = siteManager;
        this.linkProcessor = linkProcessor;
        this.siteSettings = siteSettings;
        this.indexRepository = indexRepository;
        this.indexService = indexService;

    }


    public synchronized void changeCurrentStatus(Status newStatus) {
        this.currentStatus = newStatus;
    }


    @PostConstruct
    public void init() {
        try {
            List<Long> availablePageIds = indexRepository.findAvailablePageIds();
            logger.info("Получено количество pageId: {}", availablePageIds.size());

            if (availablePageIds.isEmpty()) {
                logger.warn("Нет доступных pageId для индексации. " +
                        "Попытка обновить базу данных...");

                ensureInitialData();

                availablePageIds = indexRepository.findAvailablePageIds();
                logger.info("После обновления получено количество pageId: {}", availablePageIds.size());
            }

            if (availablePageIds.isEmpty()) {
                logger.error("Не удалось создать доступные pageId. Индексация невозможна!");
            }
        } catch (Exception e) {
            logger.error("Ошибка при инициализации индексации", e);
        }
    }


    private void ensureInitialData() {
        if (!pageRepository.existsByUrl("https://example.com")) {
            Optional<Site> maybeExampleSite = siteService.findByUrl("https://example.com");
            Site exampleSite;
            if (maybeExampleSite.isPresent()) {
                exampleSite = maybeExampleSite.get();
            } else {
                exampleSite = new Site("https://example.com",
                        "https://example.com", Status.INDEXING);
                try {
                    siteService.saveSite(exampleSite);
                    logger.info("Новый сайт создан: {}", exampleSite.getUrl());
                } catch (InvalidSiteException ise) {
                    logger.error("Ошибка при создании сайта: {}", ise.getMessage());
                    return;
                }
            }

            Page initialPage = new Page(
                    "https://example.com",
                    "/",
                    "https://example.com/",
                    exampleSite,
                    "Sample content for indexing",
                    200,
                    "Default Name",
                    "Example Title",
                    "Example snippet",
                    0.5f,
                    Status.INDEXING,
                    true
            );

            pageRepository.save(initialPage);
            logger.info("Создана первая страница для индексации: {}", initialPage.getUrl());
        }
    }


    private String convertPageIdToUrl(Long id) {
        Optional<String> optionalUrl = pageRepository.findUrlById(id);
        return optionalUrl.orElseThrow(() ->
                new IllegalArgumentException("URL не найден для pageId=" + id));
    }

    public void indexAllSites(Long id) {
        if (sites != null) {
            for (SiteSettings.SiteConfig site : sites) {
                indexSite(site.getUrl(), id);
            }
        }
    }

    public boolean isIndexing() {
        return currentStatus == Status.INDEXING;
    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }


    public void indexSite(String siteUrl, Long id) {
        lock.lock();
        try {
            if (currentStatus == Status.INDEXING) {
                logger.warn("Индексация уже в процессе для сайта: {}", siteUrl);
                return;
            }
            currentStatus = Status.INDEXING;
            logger.info("Индексация сайта с ID: {} для URL: {}", id, siteUrl);

            Optional<Long> optionalSiteId = siteService.getSiteId(siteUrl);
            if (optionalSiteId.isEmpty()) {
                logger.warn("Не удалось получить идентификатор для сайта: {}", siteUrl);
                currentStatus = Status.FAILED;
                finishIndexing(false);
                return;
            }
            Long siteId = optionalSiteId.get();

            try {
                UrlRecursiveParser parser = new UrlRecursiveParser(dbSaver, siteSettings, jsoupWrapper);

                HashSet<String> foundLinks = parser.startParsing(siteUrl);

                if (foundLinks.isEmpty()) {
                    logger.warn("Ни одной ссылки найдено не было для сайта: {}", siteUrl);
                }

                for (String link : foundLinks) {
                    if (validateLink(link)) {
                        persistLink(link);
                    }
                }
                currentStatus = Status.INDEXED;
                finishIndexing(true);
            } catch (InterruptedException e) {
                logger.error("Индексирование было прервано для сайта {}: {}", siteUrl, e.getMessage());
                Thread.currentThread().interrupt();
                currentStatus = Status.FAILED;
                finishIndexing(false);
            } catch (IOException e) {
                logger.error("Ошибка сети при доступе к сайту {}: {}", siteUrl, e.getMessage());
                currentStatus = Status.FAILED;
                finishIndexing(false);
            }
        } finally {
            lock.unlock();
        }
    }


    private Document connectWithRetry(String url, String userAgent,
                                      String referrer) throws IOException {
        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return jsoupWrapper.connect(url, userAgent, referrer);
            } catch (IOException ioEx) {
                if (attempt < maxRetries - 1) {
                    logger.warn("Ошибка подключения, повторная попытка #{}: {}",
                            attempt + 1, ioEx.getMessage());
                    continue;
                }
                throw ioEx;
            }
        }
        return null;
    }


    private void persistLink(String link) {
        String path = parsePathFromLink(link);
        Site site = resolveSiteFromLink(link);

        if (site != null && validateLink(link)) {
            Page page = new Page(
                    link,
                    path,
                    link,
                    site,
                    "",
                    200,
                    "Unnamed Link",
                    "Link Title",
                    "Link snippet",
                    0.0f,
                    Status.PENDING,
                    true
            );

            pageRepository.save(page);
            logger.info("Сохранена ссылка: {}", link);
        }
    }


    private String parsePathFromLink(String link) {
        try {
            return new URI(link).getPath();
        } catch (URISyntaxException e) {
            logger.error("Ошибка разбора пути: {}", e.getMessage());
            return "/";
        }
    }

    private Site resolveSiteFromLink(String link) {
        String host = extractHostFromLink(link);
        Optional<Site> siteOpt = siteService.findByUrl(host);
        return siteOpt.orElseGet(() -> {
            Site newSite = new Site(host, host, Status.INDEXING);
            try {
                siteService.saveSite(newSite);
                return newSite;
            } catch (InvalidSiteException e) {
                logger.error("Ошибка при создании сайта: {}", e.getMessage());
                return null;
            }
        });
    }

    private String extractHostFromLink(String link) {
        try {
            return new URI(link).getHost();
        } catch (URISyntaxException e) {
            logger.error("Ошибка разбора хостнейма: {}", e.getMessage());
            return "";
        }
    }


    private boolean validateLink(String link) {
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
                Pattern.compile(
                        "^(?:(?:https?):\\/\\/)(?:www.)" +
                                "?(?![-_])([a-zA-Z0-9.-]+)(\\.[a-z]" +
                                "{2,})?(:\\d+)?(.*)$");
        Matcher matcher = pattern.matcher(link);
        if (!matcher.matches()) {
            return false;
        }
        return true;
    }


    public boolean canStartIndexing(Long id) {
        if (id == null || id <= 0) {
            logger.warn("Недопустимый ID: {}", id);
            return false;
        }

        if (indexingInProgress.putIfAbsent(id, true) != null) {
            logger.warn("Индексация уже активна для ID: {}", id);
            return false;
        }

        currentStatus = Status.INDEXING;
        currentIndexingLemmaId = id;
        logger.info("Индексация для canStartIndexing: {} начата.", id);
        return true;
    }


    public void startIndexing(Long id, boolean isLemma) {
        lock.lock();
        try {
            if (!canStartIndexing(id)) {
                return;
            }

            activeIndexingThreads.incrementAndGet();

            logger.info("Индексация для ID: {} начата.", id);
            currentStatus = Status.INDEXING;

        } finally {
            lock.unlock();
        }
    }


    public void finishIndexing(boolean success) {
        lock.lock();
        try {
            if (currentStatus != Status.INDEXING) {
                logger.warn("Индексация не была активной.");
                return;
            }
            indexingInProgress.remove(currentIndexingLemmaId);
            activeIndexingThreads.decrementAndGet();

            if (success) {
                currentStatus = Status.INDEXED;
                logger.info("Индексация успешно завершена.");
            } else {
                currentStatus = Status.FAILED;
                logger.warn("Индексация завершена с ошибкой.");
            }
        } finally {
            lock.unlock();
        }
    }


    public void stopIndexing(Long id) {
        lock.lock();
        try {
            if (currentStatus == Status.INDEXING) {
                try {
                    if (isIndexingAsync()) {
                        stopAsyncIndexing();
                    }
                    currentStatus = Status.INDEXED;
                    logger.info("Индексация для ID: {} остановлена.", id);
                } catch (Exception e) {
                    logger.error("Ошибка при остановке индексации для ID: {}, сообщение: {}", id, e.getMessage());
                    currentStatus = Status.FAILED;
                }
            } else {
                logger.warn("Индексация для ID: {} уже была закончена или не началась.", id);
            }
        } finally {
            lock.unlock();
        }
    }


    private boolean isIndexingAsync() {
        return activeIndexingThreads.get() > 0;
    }

    private void stopAsyncIndexing() {
        for (Thread thread : activeIndexingThreadsList) {
            if (thread.isAlive()) {
                thread.interrupt();
            }
        }
        activeIndexingThreads.set(0);
    }


    public Status getCurrentStatus() {
        return currentStatus;
    }


    private void logInfo(String message, Object... args) {

        logger.info(message, args);
    }
}