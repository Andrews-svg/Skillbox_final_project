package com.example.searchengine.utils;

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
import com.example.searchengine.config.SiteSettings;
import com.example.searchengine.exceptions.InvalidSiteException;
import com.example.searchengine.indexing.IndexService;
import com.example.searchengine.services.IndexingHistoryService;
import com.example.searchengine.models.*;
import com.example.searchengine.services.*;
import org.springframework.transaction.annotation.Propagation;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Repository
public class DBSaver implements Closeable {

    private static final Logger logger =
            LoggerFactory.getLogger(DBSaver.class);
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(4);

    private final SiteService siteService;
    private final PageService pageService;
    private final FieldService fieldService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final Lemmatizer lemmatizer;
    private final SiteSettings sitesList;
    private final SearcherService searcherService;
    private final IndexingHistoryService indexingHistoryService;
    private static final ConcurrentHashMap<String, Optional<Site>> cachedSites = new ConcurrentHashMap<>();

    @Autowired
    public DBSaver(
            SiteService siteService,
            PageService pageService,
            FieldService fieldService,
            LemmaService lemmaService,
            @Lazy IndexService indexService,
            IndexingHistoryService indexingHistoryService,
            Lemmatizer lemmatizer,
            SiteSettings sitesList, SearcherService searcherService
    ) {
        this.siteService = siteService;
        this.pageService = pageService;
        this.fieldService = fieldService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.lemmatizer = lemmatizer;
        this.sitesList = sitesList;
        this.indexingHistoryService = indexingHistoryService;
        this.searcherService = searcherService;
    }

    @Override
    public void close() {
        executorService.shutdown();
    }

    public boolean isValidLink(String absUrl,
                               String domainHost, String domainCurrentLink) {
        return absUrl != null && !absUrl.isEmpty() &&
                (absUrl.startsWith("http://") || absUrl.startsWith("https://")) &&
                domainHost.equals(domainCurrentLink);
    }


    @Transactional
    public void addPagesToDatabase(String url) throws IOException, InvalidSiteException, InterruptedException {
        int retries = 3;
        String content = null;

        while (retries > 0) {
            try {
                int code = fetchUrlStatusCode(url);
                content = fetchUrlContent(url);

                if (!StringUtils.isBlank(content)) {
                    break;
                } else {
                    logger.warn("Пустой контент страницы: {}", url);
                    retries--;
                    Thread.sleep(retries * 1000);
                }
            } catch (IOException e) {
                retries--;
                logger.warn("Ошибка при загрузке контента страницы: {}", e.getMessage());
                Thread.sleep(retries * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (content == null || StringUtils.isBlank(content)) {
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

        page.setCode(fetchUrlStatusCode(url));
        page.setContent(content);

        site.addPage(page);
        site.updateStatusTime();

        siteService.saveSite(site);
        pageService.savePage(page);

        List<Field> fields = fieldService.findAllFields();
        Map<String, Float> mapTitle = new HashMap<>();
        Map<String, Float> mapBody = new HashMap<>();
        String finalContent = content;
        fields.forEach(field -> processField(finalContent, field, mapTitle, mapBody));
        Map<String, Float> mapToDB = combineTwoMaps(mapTitle, mapBody);

        UUID sessionId = indexingHistoryService.startIndexingSession();
        mapToDB.forEach((lemmaText, frequency) -> {
            BigDecimal rank = BigDecimal.valueOf(frequency);
            executorService.submit(() -> {
                saveLemmaAndIndexAsync(sessionId, site, page.getId(), lemmaText, rank);
            });
        });
        logger.info("Завершена обработка URL: {}", url);
    }



    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void saveLemmaAndIndexAsync(UUID sessionId, Site site, long pageId,
                                        String lemmaText, BigDecimal rank) {

        Lemma lemma = new Lemma(lemmaText, 1, site, site.getStatus());
        lemmaService.saveOrUpdateLemma(lemma);

        Index index = new Index();
        index.setLemma(lemma);

        Optional<Page> optionalPage = pageService.findById(pageId);
        if (!optionalPage.isPresent()) {
            logger.error("Страница с ID {} не найдена.", pageId);
            return;
        }
        Page page = optionalPage.get();
        index.setPage(page);

        index.setSite(site);
        index.setRank(rank);

        try {
            indexService.saveIndex(index, sessionId);
            logger.debug("Индекс успешно сохранён для страницы с ID: {}", pageId);
        } catch (Exception e) {
            logger.error("Ошибка при сохранении индекса: {}", e.getMessage(), e);
        }
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
                .timeout(10000)
                .followRedirects(true)
                .execute();

        if (response.statusCode() == 200) {
            logger.debug("Контент успешно загружен для URL: {}", url);
            return response.body();
        } else {
            logger.warn("HTTP Error Code: {} at URL: {}", response.statusCode(), url);
            throw new IOException("Не удалось получить контент страницы. " +
                    "Код ошибки: " + response.statusCode());
        }
    }


    private void processField(String content, Field field,
                              Map<String, Float> mapTitle, Map<String, Float> mapBody) {
        Document doc = Jsoup.parse(content);
        Elements elements = doc.select(field.getSelector());

        if (elements.isEmpty()) {
            logger.warn("Поле с селектором {} не найдено на странице", field.getSelector());
            return;
        }

        String text = elements.text();
        if (!text.isEmpty()) {
            Map<String, Integer> tempMap = lemmatizer.lemmasFrequencyMapFromString(text);
            Map<String, Float> map = new HashMap<>();
            tempMap.forEach((k, v) -> map.put(k, v.floatValue()));

            if ("title".equalsIgnoreCase(field.getSelector())) {
                mapTitle.putAll(map);
            } else {
                map.replaceAll((k, v) -> v * 0.8f);
                mapBody.putAll(map);
            }
        }
    }


    private Map<String, Float> combineTwoMaps(Map<String,
            Float> mapTitle, Map<String, Float> mapBody) {
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


    public void saveData(String url, String title,
                         Set<String> outLinksSet) throws IOException {
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
        Optional<Site> existingSiteOpt = cachedSites.computeIfAbsent(url,
                siteService::findByUrl);
        Site site;
        if (existingSiteOpt.isEmpty()) {
            site = new Site(title, url, Status.INDEXING);
            try {
                siteService.saveSite(site);
            } catch (InvalidSiteException e) {
                logger.error("Ошибка при сохранении сайта: {}", e.getMessage(), e);
            }
        } else {
            site = existingSiteOpt.get();
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
        long pageId = pageService.validateAndSavePage(page);

        outLinksSet.parallelStream().forEach(link -> {
            String name = null;
            try {
                name = extractTitleFromContent(fetchUrlContent(link));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (name == null || name.isEmpty()) {
                name = extractNameFromUrl(link);
            }
            Optional<Site> existingSiteOutlink = cachedSites.computeIfAbsent(link,
                    siteService::findByUrl);
            if (existingSiteOutlink.isEmpty()) {
                Site siteOutlink = new Site(name, link, Status.INDEXING);
                try {
                    siteService.saveSite(siteOutlink);
                } catch (InvalidSiteException e) {
                    logger.error("Ошибка при сохранении сайта: {}", e.getMessage(), e);
                }
            }
        });

        logger.info("Данные о странице '{}' успешно сохранены с ID: {}", url, pageId);
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