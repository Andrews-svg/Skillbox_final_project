package com.example.searchengine.services;

import com.example.searchengine.config.CrawlerConfig;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;
import com.example.searchengine.services.indexing.IndexService;
import com.example.searchengine.services.indexing.IndexingState;
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
    private final Lemmatizer lemmatizer;
    private final CrawlerConfig crawlerConfig;
    private final UrlFilter urlFilter;
    private final IndexingState indexingState;
    private final SeleniumFetcher seleniumFetcher;

    public PageProcessor(PageService pageService,
                         LemmaService lemmaService,
                         IndexService indexService,
                         Lemmatizer lemmatizer,
                         CrawlerConfig crawlerConfig,
                         UrlFilter urlFilter,
                         IndexingState indexingState,
                         SeleniumFetcher seleniumFetcher) {
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.lemmatizer = lemmatizer;
        this.crawlerConfig = crawlerConfig;
        this.urlFilter = urlFilter;
        this.indexingState = indexingState;
        this.seleniumFetcher = seleniumFetcher;
    }


    public Optional<Page> processPage(Site site, String pageUrl) {
        long startTime = System.currentTimeMillis();
        try {
            if (!indexingState.isActive()) {
                logger.debug("Индексация остановлена, пропускаем {}", pageUrl);
                return Optional.empty();
            }
            logger.debug("📥 Загрузка страницы: {}", pageUrl);
            if (!urlFilter.shouldIndex(pageUrl)) {
                logger.debug("Страница отфильтрована: {}", pageUrl);
                return Optional.empty();
            }
            if (!indexingState.isActive()) {
                logger.debug("Индексация остановлена перед задержкой");
                return Optional.empty();
            }
            Thread.sleep(crawlerConfig.getRandomDelay());
            if (!indexingState.isActive()) {
                logger.debug("Индексация остановлена после задержки");
                return Optional.empty();
            }
            Document doc;
            if (seleniumFetcher.shouldUseBrowser(pageUrl)) {
                doc = seleniumFetcher.fetchWithBrowser(pageUrl);
                if (doc == null) {
                    return Optional.empty();
                }
                logger.debug(">>> Документ получен через Selenium");
                logger.debug(">>> Заголовок страницы: {}", doc.title());
                logger.debug(">>> Размер HTML: {} байт", doc.html().length());
                logger.debug(">>> Количество ссылок на странице: {}", doc.select("a[href]").size());
                boolean hasProducts = doc.html().contains("product") || doc.html().contains("catalog") || doc.html().contains("товар") || doc.html().contains("каталог");
                logger.debug(">>> Есть признаки товаров: {}", hasProducts);
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
            titleLemmas.forEach((lemma, count) -> textLemmas.merge(lemma, count * 2, Integer::sum));
            int lemmaCount = 0;
            for (Map.Entry<String, Integer> entry : textLemmas.entrySet()) {
                if (!indexingState.isActive()) {
                    logger.debug("Индексация остановлена во время обработки лемм");
                    break;
                }
                try {
                    var lemma = lemmaService.saveOrIncrement(entry.getKey(), site);
                    indexService.save(page, lemma, entry.getValue());
                    lemmaCount++;
                } catch (Exception e) {
                    logger.error("Ошибка при сохранении леммы '{}': {}", entry.getKey(), e.getMessage());
                }
            }
            long duration = System.currentTimeMillis() - startTime;
            logger.info("✅ Страница обработана: {} ({} лемм, {} мс)", normalizedPath, lemmaCount, duration);
            return Optional.of(page);
        } catch (IOException e) {
            logger.error("❌ Ошибка загрузки {}: {}", pageUrl, e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("⛔ Прервана загрузка {}", pageUrl);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("🔥 Неожиданная ошибка при обработке {}: {}", pageUrl, e.getMessage(), e);
            return Optional.empty();
        }
    }
}