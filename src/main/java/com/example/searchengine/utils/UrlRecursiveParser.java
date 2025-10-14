package com.example.searchengine.utils;

import jakarta.annotation.PreDestroy;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.example.searchengine.config.SiteSettings;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Component
public class UrlRecursiveParser extends RecursiveTask<HashSet<String>> {

    private static final Logger logger = LoggerFactory.getLogger(UrlRecursiveParser.class);
    private static final Pattern REMOVE_LATINS_AND_PUNCTUATION_PATTERN = Pattern.compile("[^а-яА-Я\\s]");
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("\\s+");
    private static final int MAX_RECURSION_DEPTH = 10;
    private static final int DEFAULT_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    private final DBSaver dbSaver;
    private final SiteSettings sitesList;
    private final JsoupWrapper jsoupWrapper;

    private final ForkJoinPool pool = new ForkJoinPool(DEFAULT_POOL_SIZE);
    private final AtomicInteger recursionDepth = new AtomicInteger(0);
    private final Set<String> visitedLinks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final HashSet<String> outLinksSet = new HashSet<>();

    private volatile String url;

    public UrlRecursiveParser(DBSaver dbSaver, SiteSettings sitesList, JsoupWrapper jsoupWrapper) {
        this.dbSaver = dbSaver;
        this.sitesList = sitesList;
        this.jsoupWrapper = jsoupWrapper;
    }


    public HashSet<String> startParsing(String siteURL)
            throws InterruptedException, IOException {
        resetState();
        url = siteURL;
        Instant startTime = Instant.now();
        logger.info("Начало парсинга URL: {}", siteURL);

        pool.submit(this);
        boolean quiesced = pool.awaitQuiescence(60, TimeUnit.SECONDS);
        if (!quiesced) {
            logger.warn("Timeout истек до завершения всех задач.");
        }

        long queuedTasks = pool.getQueuedTaskCount();
        if (queuedTasks > 0) {
            logger.warn("Осталось {} ожидающих задач.", queuedTasks);
        }


        String initialContent = fetchUrlContent(siteURL);
        String title = dbSaver.extractTitleFromContent(initialContent);

        dbSaver.saveData(siteURL, title, outLinksSet);

        logger.info("Парсинг завершён за {} секунд",
                Duration.between(startTime, Instant.now()).getSeconds());

        return (HashSet<String>) outLinksSet;
    }


    public String fetchUrlContent(String url) throws IOException {
        Document document = jsoupWrapper.connect(url,
                sitesList.getUserAgent(), sitesList.getReferrer());
        return document.outerHtml();
    }

@Override
    protected HashSet<String> compute() {
        if (recursionDepth.incrementAndGet() > MAX_RECURSION_DEPTH) {
            logger.info("Превышена максимальная глубина рекурсии.");
            return outLinksSet;
        }

        if (visitedLinks.contains(url)) {
            logger.debug("Ссылка уже посещалась: {}", url);
            return outLinksSet;
        }

        try {
            visitPage(url);
        } catch (IOException e) {
            logger.error("Ошибка при загрузке страницы: {}", url, e);
        }

        return outLinksSet;
    }


    private void visitPage(String url) throws IOException {
        visitedLinks.add(url);
        Document doc = jsoupWrapper.connect(url,
                sitesList.getUserAgent(), sitesList.getReferrer());
        Elements links = doc.select("a[href]");
        processLinks(links);
    }


    private void processLinks(Elements links) {
        List<Element> validLinks = links.stream()
                .filter(link -> isValidLink(link))
                .limit(MAX_RECURSION_DEPTH)
                .collect(Collectors.toList());

        for (Element link : validLinks) {
            String href = link.attr("abs:href").trim();
            outLinksSet.add(href);
            if (!visitedLinks.contains(href)) {
                submitNextParseTask(href);
            }
        }
    }


    private void submitNextParseTask(String nextUrl) {
        CompletableFuture.supplyAsync(() -> {
                    url = nextUrl;
                    return compute();
                }, pool)
                .exceptionally(e -> {
                    logger.error("Ошибка при обработке ссылки: {}", nextUrl, e);
                    return null;
                }).join();
    }


    private boolean isValidLink(Element link) {
        String href = link.attr("abs:href");
        return checkLink(href) && !visitedLinks.contains(href);
    }


    private boolean checkLink(String link) {
        try {
            URI uri = new URI(link);
            return uri.isAbsolute() &&
                    !link.contains("#") &&
                    !link.contains("?") &&
                    !link.contains("%") &&
                    !link.endsWith(".pdf") &&
                    (link.startsWith("http://") || link.startsWith("https://"));
        } catch (URISyntaxException e) {
            logger.error("Ошибка синтаксиса URL: {}", link, e);
            return false;
        }
    }


    private synchronized void resetState() {
        recursionDepth.set(0);
        visitedLinks.clear();
        outLinksSet.clear();
    }


    @PreDestroy
    public void shutdownPool() {
        if (!pool.isShutdown()) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                    if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                        logger.error("Пул потоков не завершил работу после принудительной остановки.");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Ошибка при остановке пула потоков.", e);
            }
        }
    }


    public String normalizeString(String input) {
        return input.replaceAll(REMOVE_LATINS_AND_PUNCTUATION_PATTERN.pattern(), "")
                .replaceAll(MULTIPLE_SPACES_PATTERN.pattern(), " ")
                .trim();
    }

    public static String getPathFromLink(String link) {
        try {
            URI uri = new URI(link);
            return uri.getPath();
        } catch (Throwable t) {
            logger.error("Ошибка при извлечении пути из ссылки: {}", link, t);
            return "";
        }
    }

    public Optional<String> extractDomain(String link) {
        try {
            return Optional.of(new URL(link)).map(URL::getHost);
        } catch (Throwable t) {
            logger.error("Ошибка при извлечении доменного имени из ссылки: {}", link, t);
            return Optional.empty();
        }
    }

    public String replaceAuxiliarySymbols(String input) {
        logger.trace("Исходная строка: {}", input);
        String normalized = normalizeString(input);
        logger.trace("Нормализованная строка: {}", normalized);
        return normalized;
    }
}