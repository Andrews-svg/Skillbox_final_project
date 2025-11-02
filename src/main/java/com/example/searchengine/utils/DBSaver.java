package com.example.searchengine.utils;

import com.example.searchengine.config.Site;
import com.example.searchengine.indexing.IndexService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import com.example.searchengine.config.SitesList;
import com.example.searchengine.exceptions.InvalidSiteException;
import com.example.searchengine.models.*;
import com.example.searchengine.services.*;
import org.springframework.transaction.annotation.Propagation;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class DBSaver implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(DBSaver.class);

    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final Lemmatizer lemmatizer;
    private final SitesList sitesList;
    private final SearcherService searcherService;
    private static final ConcurrentHashMap<String, Site> cachedSites = new ConcurrentHashMap<>();

    @Autowired
    public DBSaver(SiteService siteService, PageService pageService, LemmaService lemmaService,
                   @Lazy IndexService indexService,
                   Lemmatizer lemmatizer, SitesList sitesList, SearcherService searcherService) {
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.lemmatizer = lemmatizer;
        this.sitesList = sitesList;
        this.searcherService = searcherService;
    }

    @Override
    public void close() {}

    public boolean isValidLink(String absUrl, String domainHost, String domainCurrentLink) {
        return absUrl != null && !absUrl.isEmpty()
                && (absUrl.startsWith("http://") || absUrl.startsWith("https://"))
                && domainHost.equals(domainCurrentLink);
    }


    @Transactional
    public void addPagesToDatabase(String url) throws IOException, InvalidSiteException {
        String content = fetchUrlContent(url);
        if (StringUtils.isBlank(content)) {
            logger.error("Не удалось получить контент страницы: {}", url);
            return;
        }

        List<Site> sites = searcherService.findByPartialUrl(getHostFromLink(url));
        if (sites.isEmpty()) {
            logger.warn("Сайт не найден для URL: {}", url);
            return;
        }

        Site site = sites.get(0);
        Page page = new Page(
                url,
                getPathFromLink(url),
                url,
                site,
                content,
                fetchUrlStatusCode(url),
                "Newly Added Page",
                "Page Title",
                "Page snippet",
                0.0f,
                Status.INDEXED,
                true
        );

        site.addPage(page);
        site.updateStatusTime();

        siteService.saveAll(List.of(site));
        pageService.saveAll(List.of(page));

        Map<String, Float> mapTitle = new HashMap<>();
        Map<String, Float> mapBody = new HashMap<>();
        generateLemmas(content, mapTitle, mapBody);
        Map<String, Float> mapToDB = combineTwoMaps(mapTitle, mapBody);

        mapToDB.forEach((lemmaText, frequency) -> {
            saveLemmaAndIndex(site, page.getId(), lemmaText, frequency);
        });

        logger.info("Завершена обработка URL: {}", url);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void saveLemmaAndIndex(Site site, Integer pageId, String lemmaText, float rank) {
        Lemma lemma = new Lemma(0, lemmaText, 1, site);
        lemmaService.saveOrUpdateLemma(lemma);

        Index index = new Index();
        index.setLemma(lemma);
        index.setPage(pageService.findById(pageId).orElseThrow(() ->
                new IllegalStateException("Страница с ID=" + pageId + " не найдена")));
        index.setSite(site);
        index.setRank(rank);

        indexService.saveIndex(index);
    }


    public int fetchUrlStatusCode(String url) throws IOException {
        logger.debug("Получение статуса для URL: {}", url);
        int statusCode = Jsoup.connect(url)
                .userAgent(sitesList.getUserAgent())
                .referrer(sitesList.getReferrer())
                .execute().statusCode();
        logger.debug("Получен статус-код {} для URL: {}", statusCode, url);
        return statusCode;
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


    private void generateLemmas(String content, Map<String, Float> mapTitle, Map<String, Float> mapBody) {
        Document doc = Jsoup.parse(content);
        Elements titleElements = doc.select("title");
        Elements bodyElements = doc.select("body");

        if (!titleElements.isEmpty()) {
            String titleText = titleElements.first().text();
            Map<String, Integer> titleFreqMap = lemmatizer.lemmasFrequencyMapFromString(titleText);
            titleFreqMap.forEach((key, value) -> mapTitle.put(key, value.floatValue()));
        }

        if (!bodyElements.isEmpty()) {
            String bodyText = bodyElements.first().text();
            Map<String, Integer> bodyFreqMap = lemmatizer.lemmasFrequencyMapFromString(bodyText);
            bodyFreqMap.forEach((key, value) -> mapBody.put(key, value.floatValue() * 0.8f));
        }
    }


    private Map<String, Float> combineTwoMaps(Map<String, Float> mapTitle, Map<String, Float> mapBody) {
        mapTitle.forEach((k, v) -> mapBody.merge(k, v, Float::sum));
        return mapBody;
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


    @Transactional
    public void saveData(String url, String title, Set<String> outLinksSet) throws IOException {
        if (url == null || url.isEmpty()) {
            logger.warn("Переданный URL пуст или null");
            return;
        }

        Optional<Page> existingPage = pageService.findByPath(getPathFromLink(url));
        if (existingPage.isPresent()) {
            Page oldPage = existingPage.get();
            String currentContent = fetchUrlContent(url);
            if (!currentContent.equals(oldPage.getContent())) {
                oldPage.setContent(currentContent);
                oldPage.setTitle(title);
                pageService.savePage(oldPage);
            }
            logger.info("Обновлена страница с URL: {}", url);
            return;
        }

        Optional<Site> existingSiteOpt = Optional.ofNullable(cachedSites.computeIfAbsent(url,
                k -> siteService.findByUrl(url).orElse(null)));

        Site site;
        if (existingSiteOpt.isPresent()) {
            site = existingSiteOpt.get();
        } else {
            site = new Site(title, url, Status.INDEXING);
            try {
                siteService.saveSite(site);
            } catch (InvalidSiteException e) {
                logger.error("Ошибка при сохранении сайта: {}", e.getMessage(), e);
            }
        }

        Page page = new Page(
                url,
                getPathFromLink(url),
                url,
                site,
                fetchUrlContent(url),
                200,
                title,
                title,
                "Page snippet",
                0.0f,
                Status.INDEXED,
                true
        );
        site.addPage(page);
        pageService.savePage(page);

        logger.info("Данные о странице '{}' успешно сохранены с ID: {}", url, page.getId());
    }


    public String extractTitleFromContent(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        return doc.title();
    }


    private String extractNameFromUrl(String url) {
        try {
            URL urlObj = new URL(url);
            String host = urlObj.getHost();
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (MalformedURLException e) {
            logger.error("Неправильный URL: {}", url, e);
            return "Unknown Site";
        }
    }
}