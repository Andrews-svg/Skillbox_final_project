package com.example.searchengine.services.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class LinkExtractor {

    private static final Logger logger = LoggerFactory.getLogger(LinkExtractor.class);

    private static final Pattern PAGINATION_PATTERNS = Pattern.compile(
            "(page|p|pg|pagina|seite|sid|start|offset|limit|skip)=\\d+|" +
                    "/page/\\d+|" +
                    "\\?\\d+$|" +
                    "/\\d+$"
    );

    public List<String> extractLinks(String pageUrl, String baseUrl) {
        Set<String> uniqueLinks = new HashSet<>();
        try {
            logger.debug("🔗 Извлечение ссылок с: {}", pageUrl);
            Document doc = Jsoup.connect(pageUrl)
                    .timeout(10000)
                    .get();
            for (Element link : doc.select("a[href]")) {
                String absUrl = link.absUrl("href");
                if (absUrl.startsWith(baseUrl)) {
                    String normalized = normalizeUrl(absUrl);
                    if (isValidCrawlableUrl(normalized, baseUrl)) {
                        uniqueLinks.add(normalized);
                        if (isPaginationLink(normalized)) {
                            logger.info("📑 НАЙДЕНА ССЫЛКА ПАГИНАЦИИ: {} -> {}", pageUrl, normalized);
                        }
                    }
                }
            }
            for (Element element : doc.select("[data-url], [data-href], [data-page]")) {
                String dataUrl = element.attr("data-url");
                if (!dataUrl.isEmpty()) {
                    String fullUrl = dataUrl.startsWith("http") ? dataUrl : baseUrl + dataUrl;
                    if (isPaginationLink(fullUrl)) {
                        logger.info("📑 ПАГИНАЦИЯ В DATA-АТРИБУТЕ: {} -> {}", pageUrl, fullUrl);
                        uniqueLinks.add(normalizeUrl(fullUrl));
                    }
                }
            }
            logger.debug("✅ Найдено {} уникальных ссылок на {}", uniqueLinks.size(), pageUrl);
        } catch (IOException e) {
            logger.error("❌ Ошибка при извлечении ссылок с {}: {}", pageUrl, e.getMessage());
        }
        return new ArrayList<>(uniqueLinks);
    }

    private boolean isPaginationLink(String url) {
        return PAGINATION_PATTERNS.matcher(url).find() ||
                url.matches(".*/\\d+$") ||
                url.contains("?page=") ||
                url.contains("&page=") ||
                url.contains("/page/") ||
                url.contains("?p=") ||
                url.contains("&p=") ||
                url.contains("?offset=") ||
                url.contains("start=");
    }

    private String normalizeUrl(String url) {
        if (url.contains("#")) {
            url = url.substring(0, url.indexOf("#"));
        }
        if (url.contains("?utm_")) {
            url = url.substring(0, url.indexOf("?utm_"));
        } else if (url.contains("&utm_")) {
            url = url.substring(0, url.indexOf("&utm_"));
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private boolean isValidCrawlableUrl(String url, String baseUrl) {
        return !url.contains("#") &&
                !url.contains("mailto:") &&
                !url.contains("tel:") &&
                !url.contains("javascript:") &&
                !url.contains(".pdf") &&
                !url.contains(".jpg") &&
                !url.contains(".png") &&
                !url.contains(".gif") &&
                !url.contains(".css") &&
                !url.contains(".js") &&
                !url.matches(".*\\?[a-z]*=&.*");
    }
}