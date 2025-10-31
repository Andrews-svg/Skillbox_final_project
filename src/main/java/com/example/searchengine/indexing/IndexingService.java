package com.example.searchengine.indexing;

import com.example.searchengine.exceptions.IndexingStatusFetchException;
import com.example.searchengine.exceptions.InvalidSiteException;
import com.example.searchengine.models.Page;
import com.example.searchengine.repository.PageRepository;
import com.example.searchengine.repository.SiteRepository;
import com.example.searchengine.utils.DBSaver;
import com.example.searchengine.utils.UrlRecursiveParser;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Getter
@Setter
@Service
@Transactional
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);

    private final IndexRepository indexRepository;
    private final UrlRecursiveParser recursiveParser;
    private final JsoupWrapper jsoupWrapper;
    private final SiteService siteService;
    private final DBSaver dbSaver;
    private final SiteRepository siteRepository;
    private final SitesList siteSettings;
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
    private PageRepository pageRepository;

    @Autowired
    public IndexingService(
            IndexRepository indexRepository,
            UrlRecursiveParser recursiveParser,
            JsoupWrapper jsoupWrapper,
            SiteService siteService,
            DBSaver dbSaver, SiteRepository siteRepository,
            SitesList siteSettings
    ) {
        this.indexRepository = indexRepository;
        this.recursiveParser = recursiveParser;
        this.jsoupWrapper = jsoupWrapper;
        this.siteService = siteService;
        this.dbSaver = dbSaver;
        this.siteRepository = siteRepository;
        this.siteSettings = siteSettings;
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
                    logger.info("Сайт создан: {}", exampleSite.getUrl());
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
                    "Пример содержания для индексации",
                    200,
                    "Название по умолчанию",
                    "Заголовок примера",
                    "Описание примера",
                    0.5f,
                    Status.INDEXING,
                    true
            );

            pageRepository.save(initialPage);
            logger.info("Первая страница создана: {}", initialPage.getUrl());
        }
    }

    public void indexSite(String siteUrl, Integer id) {
        Status currentStatus = indexingStatuses.get(id);

        if (currentStatus == Status.INDEXING) {
            logger.warn("Индексация уже запущена для сайта: {}", siteUrl);
            return;
        }

        indexingStatuses.put(id, Status.INDEXING);
        logger.info("Началась индексация сайта с ID: {} для URL: {}", id, siteUrl);

        Optional<Integer> optionalSiteId = siteService.getSiteId(siteUrl);
        if (optionalSiteId.isEmpty()) {
            logger.warn("Не удалось получить идентификатор для сайта: {}", siteUrl);
            indexingStatuses.put(id, Status.FAILED);
            finishIndexing(id, false);
            return;
        }
        Integer siteId = optionalSiteId.get();

        try {
            UrlRecursiveParser parser = new UrlRecursiveParser(dbSaver, siteSettings, jsoupWrapper);
            HashSet<String> foundLinks = parser.startParsing(siteUrl);

            if (foundLinks.isEmpty()) {
                logger.warn("Нет ссылок обнаружено для сайта: {}", siteUrl);
            }

            for (String link : foundLinks) {
                if (validateLink(link)) {
                    persistLink(link);
                }
            }

            indexingStatuses.put(id, Status.INDEXED);
            finishIndexing(id, true);
        } catch (IOException | InterruptedException e) {
            logger.error("Ошибка при обработке сайта {}: {}", siteUrl, e.getMessage(), e);
            indexingStatuses.put(id, Status.FAILED);
            finishIndexing(id, false);
        }
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
                    Status.INDEXING,
                    true
            );

            pageRepository.save(page);
            logger.info("Ссылка сохранена: {}", link);
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
                Pattern.compile("^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
        Matcher matcher = pattern.matcher(link);
        return matcher.matches();
    }

    public boolean canStartIndexing(Integer id) {
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


    public void startFullIndexing() {
        List<Site> allSites = siteRepository.findAll();
        for (Site site : allSites) {
            int siteId = site.getId();
            startIndexing(siteId, false);
        }

        logger.info("Запущена полная индексация всех сайтов.");
    }


    public void startIndexing(Integer id, boolean isLemma) {
        if (!canStartIndexing(id)) {
            return;
        }

        indexingStatuses.put(id, Status.INDEXING);
        activeIndexingThreads.incrementAndGet();
        Thread currentThread = Thread.currentThread();
        activeIndexingThreadsList.add(currentThread);

        logger.info("Индексация для ID: {} начата.", id);
        executorService.submit(() -> processIndexingTask(id));
    }


    private void processIndexingTask(Integer id) {
        try {
            String siteUrl = getSiteUrlForId(id);
            if (siteUrl == null) {
                logger.error("Не найден URL для сайта с ID={}", id);
                indexingStatuses.put(id, Status.FAILED);
                finishIndexing(id, false);
                return;
            }
            indexSite(siteUrl, id);
            indexingStatuses.put(id, Status.INDEXED);
            finishIndexing(id, true);
        } catch (Exception ex) {
            indexingStatuses.put(id, Status.FAILED);
            finishIndexing(id, false);
            logger.error("Ошибка при индексе страницы с ID={}: {}", id, ex.getMessage(), ex);
        }
    }


    public String getSiteUrlForId(Integer id) {
        Optional<Site> siteOptional = siteRepository.findById(id);
        return siteOptional.map(Site::getUrl).orElse(null);
    }



    public void finishIndexing(Integer id, boolean success) {
        activeIndexingThreads.decrementAndGet();
        Thread currentThread = Thread.currentThread();
        activeIndexingThreadsList.remove(currentThread);

        if (success) {
            indexingStatuses.put(id, Status.INDEXED);
            logger.info("Индексация успешно завершена для ID: {}.", id);
        } else {
            indexingStatuses.put(id, Status.FAILED);
            logger.warn("Индексация завершилась неудачно для ID: {}.", id);
        }
    }


    public void stopIndexing() {
        if (!isIndexingInProgress()) {
            throw new IllegalStateException("Индексация не запущена");
        }
        setIndexingStopped();
        logger.info("Процесс индексации остановлен.");
    }

    private boolean isIndexingInProgress() {
        return indexingInProgress;
    }

    private void setIndexingStopped() {
        indexingInProgress = false;
    }


    public String getStatus(Integer id) throws IndexingStatusFetchException {
        Optional<Site> siteOptional = siteRepository.findById(id);
        if (siteOptional.isEmpty()) {
            throw new IndexingStatusFetchException("Страница с таким ID не найдена.");
        }

        Site site = siteOptional.get();
        return site.getStatus().toString();
    }


    private void logInfo(String message, Object... args) {
        logger.info(message, args);
    }
}
