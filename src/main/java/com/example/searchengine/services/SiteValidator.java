package com.example.searchengine.services;

import com.example.searchengine.config.SitesList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@Component
public class SiteValidator {
    private static final Logger logger = LoggerFactory.getLogger(SiteValidator.class);


    public boolean isAllowedDomain(String url, SitesList sitesList) {
        return findSiteByUrl(url, sitesList).isPresent();
    }


    public Optional<SitesList.SiteConfig> findSiteByUrl(String url, SitesList sitesList) {
        if (url == null || sitesList == null || sitesList.getSites() == null) {
            return Optional.empty();
        }

        try {
            URI inputUri = new URI(url.trim());
            String inputHost = normalizeHost(inputUri.getHost());

            if (inputHost == null) {
                return Optional.empty();
            }

            for (SitesList.SiteConfig site : sitesList.getSites()) {
                URI siteUri = new URI(site.getUrl());
                String siteHost = normalizeHost(siteUri.getHost());

                if (siteHost != null && siteHost.equalsIgnoreCase(inputHost)) {
                    return Optional.of(site);
                }
            }
        } catch (URISyntaxException e) {
            logger.debug("Invalid URL format: {}", url);
        }
        return Optional.empty();
    }


    public boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return false;
        }

        try {
            URI uri = new URI(trimmed);
            return uri.getHost() != null && !uri.getHost().isEmpty();
        } catch (URISyntaxException e) {
            return false;
        }
    }


    public boolean belongsToSite(String pageUrl, String siteUrl) {
        try {
            URI pageUri = new URI(pageUrl);
            URI siteUri = new URI(siteUrl);
            String pageHost = normalizeHost(pageUri.getHost());
            String siteHost = normalizeHost(siteUri.getHost());
            return pageHost != null && pageHost.equalsIgnoreCase(siteHost);
        } catch (URISyntaxException e) {
            return false;
        }
    }


    private String normalizeHost(String host) {
        if (host == null) {
            return null;
        }
        String normalized = host.toLowerCase().trim();
        if (normalized.startsWith("www.")) {
            normalized = normalized.substring(4);
        }
        return normalized;
    }
}