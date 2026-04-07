package com.example.searchengine.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;


import java.util.HashSet;
import java.util.Set;

@Component
public class UrlFilter {

    private static final Logger logger = LoggerFactory.getLogger(UrlFilter.class);


    private static final List<String> BINARY_EXTENSIONS = Arrays.asList(
            ".pdf", ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".svg", ".webp", ".ico",
            ".zip", ".rar", ".7z", ".tar", ".gz",
            ".mp3", ".mp4", ".avi", ".mov", ".wmv", ".flv", ".webm",
            ".exe", ".dmg", ".iso",
            ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".css", ".js", ".json", ".xml", ".rss"
    );


    private static final List<String> STATIC_PATHS = Arrays.asList(
            "/assets/", "/static/", "/images/", "/img/", "/css/", "/js/",
            "/fonts/", "/uploads/", "/files/", "/download/", "/media/"
    );


    private final Set<String> visitedBaseUrls = new HashSet<>();


    public boolean hasInvalidCharacters(String url) {
        if (url == null || url.isEmpty()) {
            return true;
        }
        if (url.contains(" ")) {
            logger.debug("URL содержит пробел: {}", url);
            return true;
        }
        if (url.matches(".*[\\s<>\"{}|\\\\^`].*")) {
            logger.debug("URL содержит недопустимые символы: {}", url);
            return true;
        }
        return false;
    }


    public boolean shouldIgnoreUrl(String url) {
        if (hasInvalidCharacters(url)) {
            return true;
        }
        String baseUrl = url.split("[?#]")[0];
        if (visitedBaseUrls.contains(baseUrl) && (url.contains("?") || url.contains("#"))) {
            logger.debug("Игнорируем URL с параметрами (базовый уже посещен): {}", url);
            return true;
        }
        if (url.length() > 200) {
            logger.debug("URL слишком длинный: {}", url);
            return true;
        }
        return false;
    }


    public boolean isTrapUrl(String url) {
        String lowerUrl = url.toLowerCase();
        int paramCount = lowerUrl.split("[?&]").length - 1;
        if (paramCount > 3) {
            logger.debug("Обнаружена потенциальная ловушка (много параметров): {}", url);
            return true;
        }
        if (lowerUrl.contains("filter=") ||
                lowerUrl.contains("sort=") ||
                lowerUrl.contains("order=") ||
                lowerUrl.contains("view=") ||
                lowerUrl.contains("show=")) {
            if (lowerUrl.split("&").length > 4) {
                logger.debug("Обнаружена потенциальная ловушка (фильтры): {}", url);
                return true;
            }
        }
        if (isPaginationUrl(url)) {
            String baseUrl = url.split("[?#]")[0];
            if (url.matches(".*[?&]page=[5-9][0-9]*.*") ||
                    url.matches(".*/page/[5-9][0-9]*.*") ||
                    url.matches(".*[?&]p=[5-9][0-9]*.*")) {
                logger.debug("Слишком глубокая пагинация: {}", url);
                return true;
            }
        }
        if (lowerUrl.matches(".*/\\d{4}/\\d{2}/\\d{2}/.*") ||
                lowerUrl.matches(".*/\\d{4}/\\d{2}/.*") ||
                lowerUrl.matches(".*/\\d{4}/.*")) {
            logger.trace("Обнаружена дата в URL: {}", url);
        }
        return false;
    }


    public String cleanUrl(String url) {
        if (url == null) return null;
        String cleaned = url.replaceAll("(?<=[^:])/{2,}", "/");
        if (cleaned.length() > 1 && cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }


    public void addVisitedBaseUrl(String url) {
        String baseUrl = url.split("[?#]")[0];
        visitedBaseUrls.add(baseUrl);
    }


    public void clearVisitedBaseUrls() {
        visitedBaseUrls.clear();
    }


    public boolean isSameDomain(String url, String siteUrl) {
        try {
            URI uri = new URI(url);
            URI siteUri = new URI(siteUrl);
            String host = uri.getHost();
            String siteHost = siteUri.getHost();
            if (host == null || siteHost == null) {
                return false;
            }
            host = host.replace("www.", "");
            siteHost = siteHost.replace("www.", "");
            return host.equals(siteHost);
        } catch (URISyntaxException e) {
            logger.debug("Ошибка парсинга URI: {}", e.getMessage());
            return url.startsWith(siteUrl) || url.startsWith(siteUrl.replace("www.", ""));
        }
    }


    public boolean isBinaryFile(String url) {
        String lowerUrl = url.toLowerCase();
        for (String ext : BINARY_EXTENSIONS) {
            if (lowerUrl.endsWith(ext)) {
                logger.debug("Пропущен бинарный файл (расширение {}): {}", ext, url);
                return true;
            }
        }
        for (String path : STATIC_PATHS) {
            if (lowerUrl.contains(path)) {
                logger.debug("Пропущен статический ресурс (путь {}): {}", path, url);
                return true;
            }
        }
        return false;
    }


    public boolean isPaginationUrl(String url) {
        String lowerUrl = url.toLowerCase();
        boolean isPagination = lowerUrl.contains("page=") ||
                lowerUrl.contains("p=") ||
                lowerUrl.contains("/page/") ||
                lowerUrl.contains("/p/") ||
                lowerUrl.contains("offset=") ||
                lowerUrl.contains("start=") ||
                lowerUrl.contains("limit=") ||
                lowerUrl.contains("per-page=") ||
                lowerUrl.contains("per_page=") ||
                lowerUrl.contains("?page") ||
                lowerUrl.contains("&page") ||
                lowerUrl.matches(".*[\\?&]p=\\d+.*") ||
                lowerUrl.matches(".*/page/\\d+.*") ||
                lowerUrl.matches(".*/p/\\d+.*");
        if (isPagination) {
            logger.debug("Обнаружена страница пагинации: {}", url);
        }
        return isPagination;
    }


    public String normalizePath(String fullUrl, String baseUrl) {
        if (hasInvalidCharacters(fullUrl)) {
            logger.warn("URL содержит недопустимые символы, нормализация невозможна: {}", fullUrl);
            return "/error-invalid-url";
        }
        try {
            if (!fullUrl.startsWith(baseUrl)) {
                logger.debug("URL не принадлежит сайту: {} (base: {})", fullUrl, baseUrl);
                return "/";
            }
            URI uri = new URI(fullUrl);
            String path = uri.getPath();
            String query = uri.getQuery();
            String fragment = uri.getFragment();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (query != null || fragment != null) {
                logger.debug("Нормализация URL с параметрами: path={}, query={}, fragment={}",
                        path, query, fragment);
            }
            if (path.endsWith("/")) {
                // ничего не делаем
            } else if (path.endsWith("index.html") || path.endsWith("index.php") ||
                    path.endsWith("index.htm") || path.endsWith("index.aspx") ||
                    path.endsWith("default.html") || path.endsWith("default.aspx") ||
                    path.endsWith("home.html") || path.endsWith("main.html")) {
                String newPath = path.substring(0, path.lastIndexOf('/') + 1);
                logger.debug("Нормализован служебный путь: {} -> {}", path, newPath);
                return newPath;
            }
            return cleanUrl(path);
        } catch (URISyntaxException e) {
            logger.error("Ошибка парсинга URL: {} - {}", fullUrl, e.getMessage());
            if (fullUrl.startsWith(baseUrl)) {
                String path = fullUrl.substring(baseUrl.length());
                if (path.contains("?")) {
                    path = path.substring(0, path.indexOf("?"));
                }
                if (path.contains("#")) {
                    path = path.substring(0, path.indexOf("#"));
                }
                if (path.isEmpty()) {
                    path = "/";
                }
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                logger.debug("Ручная нормализация: {} -> {}", fullUrl, path);
                return cleanUrl(path);
            }
            return "/";
        }
    }


    public String buildFullUrl(String baseUrl, String path) {
        if (path.startsWith("http")) {
            return path;
        }
        String cleanBaseUrl = baseUrl;
        if (cleanBaseUrl.endsWith("/")) {
            cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length() - 1);
        }
        String cleanPath = path;
        if (!cleanPath.startsWith("/")) {
            cleanPath = "/" + cleanPath;
        }
        String fullUrl = cleanBaseUrl + cleanPath;
        logger.trace("Построен полный URL: {} + {} = {}", baseUrl, path, fullUrl);
        return cleanUrl(fullUrl);
    }


    public boolean shouldIndex(String url) {
        if (isBinaryFile(url)) {
            return false;
        }
        if (hasInvalidCharacters(url)) {
            return false;
        }
        if (shouldIgnoreUrl(url)) {
            return false;
        }
        if (isTrapUrl(url)) {
            return false;
        }
        logger.debug("URL разрешен к индексации: {}", url);
        return true;
    }


    public boolean isValidForCrawling(String url) {
        try {
            new URI(url);
            return !url.startsWith("mailto:") &&
                    !url.startsWith("javascript:") &&
                    !url.startsWith("#") &&
                    !url.isEmpty() &&
                    !hasInvalidCharacters(url);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}