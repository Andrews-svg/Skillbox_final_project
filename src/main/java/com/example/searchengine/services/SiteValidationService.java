package com.example.searchengine.services;

import com.example.searchengine.exceptions.InvalidSiteException;
import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SiteValidationService {

    private static final int MAX_URL_LENGTH = 2500;

    private static final Logger logger =
            LoggerFactory.getLogger(SiteValidationService.class);


    public void validateSite(Site site) throws InvalidSiteException {
        checkForNull(site);
        checkUrlIsValid(site);
        checkNameIsPresent(site);
        checkStatusIsValid(site);
        checkUrlLength(site);
    }


    public boolean checkSiteAvailability(String url) {
        try {
            URL siteUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) siteUrl.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            return (responseCode >= 200 && responseCode < 400);
        } catch (IOException e) {
            logger.error("Failed to connect to site at URL: {}", url, e);
            return false;
        }
    }


    public boolean checkContentValidity(Map<String, String> content) {
        return content != null && !content.isEmpty();
    }


    private boolean validStatus(Status status) {
        return List.of(Status.INDEXING, Status.INDEXED, Status.FAILED).contains(status);
    }


    private void checkNameIsPresent(Site site) throws InvalidSiteException {
        if (StringUtils.isBlank(site.getName())) {
            logAndThrow("Имя сайта отсутствует.", site);
        }
    }


    private void checkStatusIsValid(Site site) throws InvalidSiteException {
        if (!validStatus(site.getStatus())) {
            logAndThrow("Недопустимый статус сайта.", site);
        }
    }


    private void checkUrlLength(Site site) throws InvalidSiteException {
        if (site.getUrl().length() > MAX_URL_LENGTH) {
            logAndThrow("Длина URL превышает максимальный предел.", site);
        }
    }


    private void checkForNull(Site site) throws InvalidSiteException {
        if (site == null) {
            logAndThrow("Предоставлен нулевой объект сайта.", site);
        }
    }


    private void checkUrlIsValid(Site site) throws InvalidSiteException {
        if (StringUtils.isBlank(site.getUrl())) {
            logAndThrow("URL сайта отсутствует или недействителен.", site);
        }
    }


    private void logAndThrow(String errorMessage, Site site) throws InvalidSiteException {
        logger.error("Некорректный объект сайта: {}. Подробности: {}", errorMessage, site);
        throw new InvalidSiteException("Некорректный объект сайта: " + errorMessage);
    }
}