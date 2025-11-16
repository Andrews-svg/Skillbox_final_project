package com.example.searchengine.services;


import com.example.searchengine.config.Site;
import com.example.searchengine.config.SitesList;
import com.example.searchengine.exceptions.InvalidSiteException;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Status;
import jakarta.transaction.Transactional;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WebProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(WebProcessingService.class);

    private final SiteService siteService;
    private final SitesList sitesList;

    public WebProcessingService(SiteService siteService, SitesList sitesList) {
        this.siteService = siteService;
        this.sitesList = sitesList;
    }

    @Transactional
    public String parsePathFromLink(String link) {
        try {
            return new URI(link).getPath();
        } catch (URISyntaxException e) {
            logger.error("Ошибка разбора пути: {}", e.getMessage());
            return "/";
        }
    }


    @Transactional
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


    @Transactional
    public String extractHostFromLink(String link) {
        try {
            return new URI(link).getHost();
        } catch (URISyntaxException e) {
            logger.error("Ошибка разбора хостнейма: {}", e.getMessage());
            return "";
        }
    }


    @Transactional
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


    @Transactional
    public Page createPageFromDocument(Site site, String path, Document doc) {
        Page page = new Page();
        page.setSite(site);
        page.setPath(path);
        page.setContent(doc.html());
        page.setCode(HttpURLConnection.HTTP_OK);
        return page;
    }


    @Transactional
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


    @Transactional
    public void processSinglePage(String currentUrl, Site site,
                                  List<Page> pagesBatch,
                                  Queue<String> urlsToVisit) throws IOException {
        Document doc = Jsoup.connect(currentUrl).get();

        Page page = createPageFromDocument(site, currentUrl, doc);
        pagesBatch.add(page);
        addLinksFromPage(site, doc, urlsToVisit);
    }


    public boolean isValidLink(String absUrl, String domainHost, String domainCurrentLink) {
        return absUrl != null && !absUrl.isEmpty()
                && (absUrl.startsWith("http://") || absUrl.startsWith("https://"))
                && domainHost.equals(domainCurrentLink);
    }


    public int fetchUrlStatusCode(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(sitesList.getUserAgent())
                .referrer(sitesList.getReferrer())
                .execute().statusCode();
    }


    public String fetchUrlContent(String url) throws IOException {
        logger.debug("Загрузка контента для URL: {}", url);
        Connection.Response response = Jsoup.connect(url)
                .userAgent(sitesList.getUserAgent())
                .referrer(sitesList.getReferrer())
                .timeout(10_000)
                .followRedirects(true)
                .execute();
        if (response.statusCode() == 200) {
            logger.debug("Контент успешно загружен для URL: {}", url);
            return response.body();
        } else {
            logger.warn("HTTP Error Code: {} at URL: {}", response.statusCode(), url);
            throw new IOException("Не удалось получить контент страницы. Код ошибки: " + response.statusCode());
        }
    }


    public Document parseDOM(String content) {
        return Jsoup.parse(content);
    }


    public String extractTitleFromContent(Document document) {
        return document.title();
    }


    public String getHostFromLink(String link) {
        try {
            URL url = new URL(link);
            return url.getHost();
        } catch (MalformedURLException e) {
            logger.error("Неверный URL: {}", link, e);
            return "";
        }
    }


    @Cacheable(value = "pathCache", key = "#link.hashCode()")
    public String getPathFromLink(String link) {
        if (link == null || !link.contains("://")) {
            logger.warn("Переданная ссылка некорректна или пуста: {}", link);
            return null;
        }
        try {
            URL url = new URL(link);
            String path = url.getPath();
            if (path == null || path.isEmpty()) {
                logger.warn("Пустой путь обнаружен для ссылки: {}", link);
                return "/";
            }
            logger.debug("Полученный путь из ссылки {}: {}", link, path);
            return path;
        } catch (MalformedURLException e) {
            logger.error("Ошибка при разборе URL: {}", link, e);
            return null;
        }
    }
}
