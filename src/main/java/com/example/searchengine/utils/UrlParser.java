package com.example.searchengine.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class UrlParser {

    private static final Logger logger = LoggerFactory.getLogger(UrlParser.class);
    private static final Pattern REMOVE_LATINS_AND_PUNCTUATION_PATTERN = Pattern.compile("[^а-яА-Я\\s]");
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("\\s+");


    public String normalizeString(String input) {
        return input.replaceAll(REMOVE_LATINS_AND_PUNCTUATION_PATTERN.pattern(), "")
                .replaceAll(MULTIPLE_SPACES_PATTERN.pattern(), " ").trim();
    }


    public static String getPathFromLink(String link) {
        try {
            URI uri = new URI(link);
            return uri.getPath();
        } catch (Throwable t) {
            logger.error("Ошибка при извлечении пути из ссылки: {}", link, t);
            return "";
        }
    }


    public Optional<String> extractDomain(String link) {
        try {
            return Optional.of(new URL(link)).map(URL::getHost);
        } catch (Throwable t) {
            logger.error("Ошибка при извлечении доменного имени из ссылки: {}", link, t);
            return Optional.empty();
        }
    }


    public String replaceAuxiliarySymbols(String input) {
        logger.trace("Исходная строка: {}", input);
        String normalized = normalizeString(input);
        logger.trace("Нормализованная строка: {}", normalized);
        return normalized;
    }
}