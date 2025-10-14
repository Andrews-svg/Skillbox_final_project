package com.example.searchengine.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SiteSettings {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String userAgent;
    private String referrer;
    private String webInterfacePath;
    private String baseUrl;

    private Map<Long, SiteConfig> sites = new LinkedHashMap<>();

    public String getUserAgent() {
        logger.debug("Getting User-Agent: {}", userAgent);
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        logger.info("Setting User-Agent to: {}", userAgent);
        this.userAgent = userAgent;
    }

    public String getReferrer() {
        logger.debug("Getting Referrer: {}", referrer);
        return referrer;
    }

    public void setReferrer(String referrer) {
        logger.info("Setting Referrer to: {}", referrer);
        this.referrer = referrer;
    }

    public String getWebInterfacePath() {
        logger.debug("Getting Web Interface Path: {}", webInterfacePath);
        return webInterfacePath;
    }

    public void setWebInterfacePath(String webInterfacePath) {
        logger.info("Setting Web Interface Path to: {}", webInterfacePath);
        this.webInterfacePath = webInterfacePath;
    }

    public Map<Long, SiteConfig> getSites() {
        logger.debug("Returning all Sites: {}", sites);
        return Collections.unmodifiableMap(sites);
    }

    public void setSites(Map<Long, SiteConfig> sites) {
        logger.info("Setting Sites with size: {}", sites.size());
        this.sites.putAll(sites);
    }

    public String getName(Long index) {
        SiteConfig site = sites.get(index);
        if (site != null) {
            logger.debug("Getting Name for index {}: {}", index, site.getName());
        } else {
            logger.warn("No site found for index: {}", index);
        }
        return (site != null) ? site.getName() : null;
    }

    public String getUrl(Long index) {
        SiteConfig site = sites.get(index);
        if (site != null) {
            logger.debug("Getting URL for index {}: {}", index, site.getUrl());
        } else {
            logger.warn("No site found for index: {}", index);
        }
        return (site != null) ? site.getUrl() : null;
    }

    public String getBaseUrl() {
        logger.debug("Getting Base URL: {}", baseUrl);
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        logger.info("Setting Base URL to: {}", baseUrl);
        this.baseUrl = baseUrl;
    }

    public List<String> getAllUrls() {
        List<String> urls = sites.values().stream()
                .map(SiteConfig::getUrl)
                .distinct()
                .collect(Collectors.toList());
        logger.debug("Collecting All URLs: {}", urls);
        return urls;
    }

    public List<String> getAllNames() {
        List<String> names = sites.values().stream()
                .map(SiteConfig::getName)
                .collect(Collectors.toList());
        logger.debug("Collecting All Names: {}", names);
        return names;
    }

    public static class SiteConfig {
        private String name;
        private String url;

        public SiteConfig() {}

        public SiteConfig(String name, String url) {
            this.name = name;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}