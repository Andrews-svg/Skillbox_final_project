package com.example.searchengine.services;

import com.example.searchengine.config.SitesList;
import com.example.searchengine.exceptions.InvalidSiteException;
import com.example.searchengine.utils.JsoupWrapper;
import jakarta.annotation.PreDestroy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CrawlerService {

    private static final Logger logger = LoggerFactory.getLogger(CrawlerService.class);
    private static final int MAX_RECURSION_DEPTH = 10;
    private static final int DEFAULT_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    private final WebProcessingService webProcessingService;
    private final DataSaver dataSaver;
    private final SitesList sitesList;
    private final JsoupWrapper jsoupWrapper;
    private final ForkJoinPool pool;

    private final AtomicInteger recursionDepth = new AtomicInteger(0);
    private final Set<String> visitedLinks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final HashSet<String> outLinksSet = new HashSet<>();

    @Autowired
    public CrawlerService(WebProcessingService webProcessingService,
                          DataSaver dataSaver,
                          SitesList sitesList,
                          JsoupWrapper jsoupWrapper) {
        this.webProcessingService = webProcessingService;
        this.dataSaver = dataSaver;
        this.sitesList = sitesList;
        this.jsoupWrapper = jsoupWrapper;
        this.pool = new ForkJoinPool(DEFAULT_POOL_SIZE);
    }

    public HashSet<String> startParsing(String siteURL) throws Exception {
        resetState();
        logger.info("Начало парсинга URL: {}", siteURL);
        pool.invoke(new ParseTask(siteURL));
        saveData(siteURL);
        return outLinksSet;
    }

    private class ParseTask extends RecursiveAction {
        private final String url;

        ParseTask(String url) {
            this.url = url;
        }

        @Override
        protected void compute() {
            if (recursionDepth.incrementAndGet() > MAX_RECURSION_DEPTH) {
                logger.info("Превышена максимальная глубина рекурсии.");
                return;
            }
            if (visitedLinks.contains(url)) {
                logger.debug("Ссылка уже посещалась: {}", url);
                return;
            }
            try {
                visitPage(url);
            } catch (IOException e) {
                logger.error("Ошибка при загрузке страницы: {}", url, e);
            }
        }
    }

    private void visitPage(String url) throws IOException {
        visitedLinks.add(url);
        Document doc = jsoupWrapper.connect(url, sitesList.getUserAgent(),
                sitesList.getReferrer());
        Elements links = doc.select("a[href]");
        processLinks(links);
    }

    private void processLinks(Elements links) {
        List<Element> validLinks = links.stream()
                .filter(this::isValidLink)
                .limit(MAX_RECURSION_DEPTH)
                .toList();

        for (Element link : validLinks) {
            String href = link.attr("abs:href").trim();
            outLinksSet.add(href);
            if (!visitedLinks.contains(href)) {
                submitNextParseTask(href);
            }
        }
    }

    private void submitNextParseTask(String nextUrl) {
        pool.execute(new ParseTask(nextUrl));
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

    private void saveData(String siteURL) throws IOException,
            InvalidSiteException, SiteService.InvalidSiteException {
        Document doc = Jsoup.parse(fetchUrlContent(siteURL));
        String title = webProcessingService.extractTitleFromContent(doc);
        dataSaver.saveData(siteURL, title, outLinksSet);
    }

    private String fetchUrlContent(String url) throws IOException {
        Document document = jsoupWrapper.connect(url,
                sitesList.getUserAgent(), sitesList.getReferrer());
        return document.outerHtml();
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
                        logger.error("Пул потоков не завершился вовремя.");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Ошибка при завершении работы пула потоков.", e);
            }
        }
    }
}