package com.example.searchengine.services;


import com.example.searchengine.config.Site;
import com.example.searchengine.exceptions.InvalidSiteException;
import com.example.searchengine.indexing.AdvancedIndexOperations;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Status;
import com.example.searchengine.repository.PageRepository;
import com.example.searchengine.repository.SiteRepository;
import com.example.searchengine.utils.ContentProcessor;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;


@Service
public class DatabaseService {

    private static final ConcurrentHashMap<String, Site> cachedSites = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SiteService siteService;
    private final PageService pageService;
    private final WebProcessingService webProcessingService;
    private final SearcherService searcherService;
    private final LemmaService lemmaService;
    private final ContentProcessor contentProcessor;
    private final AdvancedIndexOperations advancedIndexOperations;


    @Autowired
    public DatabaseService(
            PageRepository pageRepository,
            SiteRepository siteRepository,
            SiteService siteService,
            PageService pageService,
            WebProcessingService webProcessingService,
            SearcherService searcherService,
            LemmaService lemmaService,
            ContentProcessor contentProcessor,
            @Lazy AdvancedIndexOperations advancedIndexOperations
    ) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.siteService = siteService;
        this.pageService = pageService;
        this.webProcessingService = webProcessingService;
        this.searcherService = searcherService;
        this.lemmaService = lemmaService;
        this.contentProcessor = contentProcessor;
        this.advancedIndexOperations = advancedIndexOperations;
    }


    @Transactional
    public void ensureInitialData() {
        if (!siteRepository.existsByUrl("https://example.com")) {
            Optional<Site> maybeExampleSite = siteRepository.findByUrl("https://example.com");
            Site exampleSite;
            if (maybeExampleSite.isPresent()) {
                exampleSite = maybeExampleSite.get();
            } else {
                exampleSite = new Site(Status.INDEXING, LocalDateTime.now(),
                        "https://example.com", "Example Website");
                try {
                    siteService.saveSite(exampleSite);
                    logger.info("Сайт создан: {}", exampleSite.getUrl());
                } catch (SiteService.InvalidSiteException e) {
                    throw new RuntimeException(e);
                }
            }
            Page initialPage = new Page("/", 200,
                    "Пример содержания для индексации", exampleSite);
            pageRepository.save(initialPage);
            logger.info("Первая страница создана: {}", initialPage.getPath());
        }
    }


    @Transactional
    public void updateSiteStatus(long siteId, Status newStatus, String errorMessage) {
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


    @Transactional
    public void persistLink(String link) {
        String path = webProcessingService.parsePathFromLink(link);
        Site site = webProcessingService.resolveSiteFromLink(link);
        if (site != null && webProcessingService.validateLink(link)) {
            Page page = new Page(path, 200, "", site);
            pageRepository.save(page);
            logger.info("Ссылка сохранена: {}", link);
        }
    }


    @Transactional
    public void batchSavePages(List<Page> pages) {
        pageRepository.saveAll(pages);
    }


    public void prepareIndexing(long id) throws Exception {
        Site site = siteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сайт не найден"));
        pageRepository.deleteBySite(site);
    }


    @Transactional
    public void crawlAndIndex(Site site) throws Exception {
        Set<String> visitedUrls = new HashSet<>();
        Queue<String> urlsToVisit = new LinkedBlockingQueue<>();
        urlsToVisit.offer(site.getUrl());
        List<Page> pagesBatch = new ArrayList<>(100);
        while (!urlsToVisit.isEmpty()) {
            String currentUrl = urlsToVisit.poll();
            if (visitedUrls.contains(currentUrl)) continue;
            visitedUrls.add(currentUrl);
            webProcessingService.processSinglePage(currentUrl, site, pagesBatch, urlsToVisit);
            if (pagesBatch.size() >= 100) {
                batchSavePages(pagesBatch);
                pagesBatch.clear();
            }
        }
        if (!pagesBatch.isEmpty()) {
            batchSavePages(pagesBatch);
        }
    }

    @Transactional
    public void finishIndexing(long siteId, boolean isSuccess) {
        Status finalStatus = isSuccess ? Status.INDEXED : Status.FAILED;
        updateSiteStatus(siteId, finalStatus, "");
    }


    @Transactional
    public void indexSite(long siteId) throws Exception {
        prepareIndexing(siteId);
        Site site = siteRepository.findById(siteId).orElseThrow(() ->
                new IllegalArgumentException("Сайт не найден"));
        crawlAndIndex(site);
        finishIndexing(siteId, true);
    }


    @Transactional
    public void deleteEntireSiteData(Site site) {
        pageRepository.deleteBySite(site);
        siteRepository.delete(site);
    }


    @Transactional
    public void addPagesToDatabase(String url) throws IOException, SiteService.InvalidSiteException {
        String content = webProcessingService.fetchUrlContent(url);
        if (StringUtils.isBlank(content)) {
            logger.error("Не удалось получить контент страницы: {}", url);
            return;
        }
        List<Site> sites = searcherService.findByPartialUrl(webProcessingService.getHostFromLink(url));
        if (sites.isEmpty()) {
            logger.warn("Сайт не найден для URL: {}", url);
            return;
        }
        Site site = sites.getFirst();
        Page page = new Page(webProcessingService.getPathFromLink(url),
                webProcessingService.fetchUrlStatusCode(url), content, site);
        site.addPage(page);
        site.updateStatusTime();
        siteService.saveAll(List.of(site));
        pageService.saveAll(List.of(page));
        Map<String, Float> mapTitle = new HashMap<>();
        Map<String, Float> mapBody = new HashMap<>();
        lemmaService.generateLemmas(content, mapTitle, mapBody);
        Map<String, Float> mapToDB = contentProcessor.combineTwoMaps(mapTitle, mapBody);
        mapToDB.forEach((lemmaText, frequency) -> {
            advancedIndexOperations.saveLemmaAndIndex(site, page.getId(), lemmaText, frequency);
        });
        logger.info("Завершена обработка URL: {}", url);
    }


    @Transactional
    public void saveData(String url, String title, Set<String> outLinksSet)
            throws IOException, InvalidSiteException, SiteService.InvalidSiteException {
        if (url == null || url.isEmpty()) {
            logger.warn("Переданный URL пуст или null");
            return;
        }
        Optional<Page> existingPage = pageService.findByPath(webProcessingService.parsePathFromLink(url));
        if (existingPage.isPresent()) {
            Page oldPage = existingPage.get();
            String currentContent = webProcessingService.fetchUrlContent(url);
            if (!currentContent.equals(oldPage.getContent())) {
                oldPage.setContent(currentContent);
                pageService.savePage(oldPage);
            }
            logger.info("Обновлена страница с URL: {}", url);
            return;
        }
        Optional<Site> existingSiteOpt = siteRepository.findByUrl(url);
        Site site;
        if (existingSiteOpt.isPresent()) {
            site = existingSiteOpt.get();
        } else {
            site = new Site(Status.INDEXING, LocalDateTime.now(), url, title);
            siteService.saveSite(site);
        }
        Page page = new Page(webProcessingService.parsePathFromLink(url),
                200, webProcessingService.fetchUrlContent(url), site);
        pageService.savePage(page);
        logger.info("Данные о странице '{}' успешно сохранены с ID: {}", url, page.getId());
    }
}
