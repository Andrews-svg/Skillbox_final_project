package com.example.searchengine.services.crawler;

import com.example.searchengine.config.CrawlerConfig;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;
import com.example.searchengine.services.LemmaService;
import com.example.searchengine.services.PageService;
import com.example.searchengine.services.indexing.IndexService;
import com.example.searchengine.services.indexing.IndexingState;
import com.example.searchengine.services.indexing.PageIndexingService;
import com.example.searchengine.utils.Lemmatizer;
import com.example.searchengine.utils.UrlFilter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
public class PageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(PageProcessor.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36...";
    private static final String REFERRER = "https://www.google.com/";

    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final PageIndexingService pageIndexingService;
    private final Lemmatizer lemmatizer;
    private final CrawlerConfig crawlerConfig;
    private final UrlFilter urlFilter;
    private final IndexingState indexingState;
    private final SeleniumFetcher seleniumFetcher;
    private final WatchdogService watchdogService;

    public PageProcessor(PageService pageService,
                         LemmaService lemmaService,
                         IndexService indexService,
                         PageIndexingService pageIndexingService,
                         Lemmatizer lemmatizer,
                         CrawlerConfig crawlerConfig,
                         UrlFilter urlFilter,
                         IndexingState indexingState,
                         SeleniumFetcher seleniumFetcher,
                         WatchdogService watchdogService) {
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.pageIndexingService = pageIndexingService;
        this.lemmatizer = lemmatizer;
        this.crawlerConfig = crawlerConfig;
        this.urlFilter = urlFilter;
        this.indexingState = indexingState;
        this.seleniumFetcher = seleniumFetcher;
        this.watchdogService = watchdogService;

    }

    public Optional<ProcessedPage> processPage(Site site, String pageUrl) {
        watchdogService.notifyActivity();
        long startTime = System.currentTimeMillis();

        if (!indexingState.isActive()) {
            logger.debug("Индексация остановлена, пропускаем {}", pageUrl);
            return Optional.empty();
        }

        if (!urlFilter.shouldIndex(pageUrl)) {
            logger.debug("Страница отфильтрована: {}", pageUrl);
            return Optional.empty();
        }

        try {
            Thread.sleep(crawlerConfig.getRandomDelay());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("Задержка прервана для {}", pageUrl);
            return Optional.empty();
        }

        if (!indexingState.isActive()) {
            logger.debug("Индексация остановлена после задержки");
            return Optional.empty();
        }

        Optional<Document> docOpt = fetchDocumentWithRetry(site, pageUrl);
        if (docOpt.isEmpty()) {
            return Optional.empty();
        }
        Document doc = docOpt.get();

        String normalizedPath = urlFilter.normalizePath(pageUrl, site.getUrl());
        String content = doc.html();
        String text = doc.body().text();
        String title = doc.title();

        logger.debug("Контент: HTML={} байт, TEXT={} символов", content.length(), text.length());
        if (text.length() < 100) {
            logger.warn("⚠️ Мало текста на странице {}: {} символов", pageUrl, text.length());
        }

        if (!indexingState.isActive()) {
            logger.debug("Индексация остановлена перед удалением старой версии");
            return Optional.empty();
        }

        if (pageService.existsByPathAndSite(normalizedPath, site)) {
            logger.debug("Страница уже существует, обновляем: {}", normalizedPath);
            pageService.findByPathAndSite(normalizedPath, site).ifPresent(oldPage -> {
                try {
                    indexService.deleteByPage(oldPage);
                    lemmaService.decrementAllForPage(oldPage);
                    pageService.delete(oldPage);
                } catch (Exception e) {
                    logger.warn("Ошибка при удалении старой версии: {}", e.getMessage());
                }
            });
        }

        if (!indexingState.isActive()) {
            logger.debug("Индексация остановлена перед сохранением страницы");
            return Optional.empty();
        }

        Page page = new Page(normalizedPath, 200, content, site);
        page = pageService.save(page);

        if (!indexingState.isActive()) {
            logger.debug("Индексация остановлена перед лемматизацией");
            return Optional.empty();
        }


        Map<String, Integer> textLemmas = lemmatizer.getLemmasFrequency(text);
        Map<String, Integer> titleLemmas = lemmatizer.getLemmasFrequency(title);
        titleLemmas.forEach((lemma, count) ->
                textLemmas.merge(lemma, count * 2, Integer::sum));

        if (!indexingState.isActive()) {
            logger.debug("Индексация остановлена перед сохранением лемм");
            return Optional.empty();
        }

        int lemmaCount = pageIndexingService.savePageLemmas(page, site, textLemmas);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("✅ Страница обработана: {} ({} лемм, {} мс)",
                normalizedPath, lemmaCount, duration);
        return Optional.of(new ProcessedPage(page, doc));
    }


    private Optional<Document> fetchDocumentWithRetry(Site site, String pageUrl) {
        boolean useBrowser = seleniumFetcher.shouldUseBrowser(pageUrl);
        logger.info("🔍 shouldUseBrowser для {} = {}", pageUrl, useBrowser);

        int maxAttempts = crawlerConfig.getRetryCount();
        int delayMs = crawlerConfig.getRetryDelay();
        int attempt = 0;

        while (attempt < maxAttempts) {
            attempt++;
            if (attempt > 1) {
                logger.info("🔄 Повторная попытка {}/{} для {}", attempt, maxAttempts, pageUrl);
                try {
                    Thread.sleep((long) delayMs * attempt);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Повторная попытка прервана для {}", pageUrl);
                    return Optional.empty();
                }
                if (!indexingState.isActive()) {
                    logger.debug("Индексация остановлена, прерываем попытки для {}", pageUrl);
                    return Optional.empty();
                }
            }

            try {
                Document doc;
                if (useBrowser) {
                    doc = seleniumFetcher.fetchWithBrowser(pageUrl);
                    if (doc == null) {
                        logger.warn("Selenium не смог загрузить {}, попытка {}", pageUrl, attempt);
                        continue;
                    }
                } else {
                    Connection.Response response = Jsoup.connect(pageUrl)
                            .userAgent(USER_AGENT)
                            .referrer(REFERRER)
                            .timeout(crawlerConfig.getTimeout())
                            .followRedirects(true)
                            .execute();

                    int statusCode = response.statusCode();
                    if (statusCode < 200 || statusCode >= 300) {
                        logger.debug("Страница {} недоступна, код: {}", pageUrl, statusCode);
                        return Optional.empty();
                    }

                    String finalUrl = response.url().toString();
                    if (!finalUrl.startsWith(site.getUrl())) {
                        logger.debug("Редирект на внешний ресурс: {} -> {}", pageUrl, finalUrl);
                        return Optional.empty();
                    }
                    doc = response.parse();
                }

                if (attempt > 1) {
                    logger.info("✅ Успешная загрузка {} после {} попыток", pageUrl, attempt);
                }
                return Optional.of(doc);

            } catch (IOException e) {
                logger.warn("Ошибка загрузки {} (попытка {}): {}", pageUrl, attempt, e.getMessage());
                if (attempt >= maxAttempts) {
                    logger.error("❌ Не удалось загрузить {} после {} попыток", pageUrl, maxAttempts);
                    return Optional.empty();
                }
            } catch (Exception e) {
                logger.error("🔥 Критическая ошибка при загрузке {} (попытка {}): {}",
                        pageUrl, attempt, e.getMessage());
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}