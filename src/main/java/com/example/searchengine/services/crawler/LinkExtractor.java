package com.example.searchengine.services;

import com.example.searchengine.config.CrawlerConfig;
import com.example.searchengine.utils.UrlFilter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


@Component
public class LinkExtractor {

    private static final Logger logger = LoggerFactory.getLogger(LinkExtractor.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String REFERRER = "https://www.google.com/";

    private final CrawlerConfig crawlerConfig;
    private final UrlFilter urlFilter;


    public LinkExtractor(CrawlerConfig crawlerConfig, UrlFilter urlFilter) {
        this.crawlerConfig = crawlerConfig;
        this.urlFilter = urlFilter;
    }


    public List<String> extractLinks(String pageUrl, String baseUrl) {
        List<String> links = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        try {
            logger.debug("🔗 Извлечение ссылок из: {}", pageUrl);
            Document doc = Jsoup.connect(pageUrl)
                    .userAgent(USER_AGENT)
                    .referrer(REFERRER)
                    .timeout(crawlerConfig.getTimeout())
                    .followRedirects(true)
                    .get();
            Elements elements = doc.select("a[href]");
            int totalCount = 0;
            int externalCount = 0;
            int binaryCount = 0;
            int invalidCount = 0;
            int addedCount = 0;
            String normalizedBaseUrl = normalizeSiteUrl(baseUrl);
            String baseUrlWithWww = baseUrl;
            String baseUrlWithoutWww = baseUrl.replace("www.", "");
            for (Element element : elements) {
                String href = element.attr("href");
                totalCount++;
                if (href.isEmpty() || href.startsWith("#") ||
                        href.startsWith("javascript:") || href.startsWith("mailto:")) {
                    invalidCount++;
                    continue;
                }
                String absUrl = element.attr("abs:href");
                if (absUrl.isEmpty()) {
                    absUrl = constructAbsoluteUrl(pageUrl, href);
                }
                if (absUrl.isEmpty()) {
                    invalidCount++;
                    continue;
                }
                if (!isSameSite(absUrl, baseUrlWithWww, baseUrlWithoutWww)) {
                    externalCount++;
                    continue;
                }
                if (urlFilter.isBinaryFile(absUrl)) {
                    binaryCount++;
                    continue;
                }
                links.add(absUrl);
                addedCount++;
                if (urlFilter.isPaginationUrl(absUrl)) {
                    logger.debug("📄 Найдена страница пагинации: {}", absUrl);
                }
            }
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("📊 Статистика ссылок для {} ({} мс): всего={}, " +
                            "внешних={}, бинарных={}, служебных={}, добавлено={}",
                    pageUrl, duration, totalCount, externalCount, binaryCount, invalidCount, addedCount);
        } catch (IOException e) {
            logger.error("❌ Ошибка загрузки {} для извлечения ссылок: {}", pageUrl, e.getMessage());
        }
        return links;
    }


    public boolean hasPaginationGap(Document doc, String url) {
        try {
            Elements products = doc.select(".product, .item, .catalog, " +
                    "[class*='product'], [class*='item'], .goods, .products");
            boolean isCatalogPage = products.size() > 5 ||
                    doc.html().contains("товар") ||
                    doc.html().contains("каталог");
            if (!isCatalogPage) {
                return false;
            }
            Elements paginationLinks = doc.select("a:contains(далее), a:contains(следующая), " +
                    "a:contains(→), a:contains(>), " +
                    "a[rel=next], .pagination a, " +
                    "a[href*='page='], a[href*='/page/']");
            boolean hasNextLink = !paginationLinks.isEmpty();
            if (!hasNextLink) {
                logger.debug("Обнаружен потенциальный JS-тупик на {}", url);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.warn("Ошибка при проверке пагинации для {}: {}", url, e.getMessage());
            return false;
        }
    }



    private String constructAbsoluteUrl(String pageUrl, String href) {
        try {
            URI baseUri = new URI(pageUrl);
            URI resolved = baseUri.resolve(href);
            return resolved.toString();
        } catch (URISyntaxException | IllegalArgumentException e) {
            logger.trace("Не удалось сконструировать абсолютный URL из {} и {}", pageUrl, href);
            return "";
        }
    }


    private boolean isSameSite(String url, String siteUrlWithWww, String siteUrlWithoutWww) {
        return url.startsWith(siteUrlWithWww) || url.startsWith(siteUrlWithoutWww);
    }


    private String normalizeSiteUrl(String siteUrl) {
        String normalized = siteUrl.toLowerCase();
        if (normalized.startsWith("http://")) {
            normalized = "https://" + normalized.substring(7);
        }
        return normalized;
    }
}