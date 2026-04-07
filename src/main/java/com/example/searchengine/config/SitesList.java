package com.example.searchengine.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SitesList {
    private static final Logger logger = LoggerFactory.getLogger(SitesList.class);

    private String userAgent = "HeliontSearchBot/1.0";
    private String referrer = "https://www.google.com";
    private String baseUrl = "http://localhost:8080";
    private List<SiteConfig> sites = new ArrayList<>();


    public SitesList() {
        logger.debug("SitesList initialized with default values");
    }


    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            throw new IllegalArgumentException("userAgent cannot be null or empty");
        }
        this.userAgent = userAgent;
        logger.debug("User-Agent set to: {}", userAgent);
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
        logger.debug("Referrer set to: {}", referrer);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        logger.debug("Base URL set to: {}", baseUrl);
    }


    public List<SiteConfig> getSites() {
        return Collections.unmodifiableList(sites);
    }

    public void setSites(List<SiteConfig> sites) {
        if (sites == null) {
            this.sites = new ArrayList<>();
        } else {
            this.sites = new ArrayList<>(sites);
        }
        logger.debug("Loaded {} sites for indexing", this.sites.size());
    }


    public int getSiteCount() {
        return sites.size();
    }


    public boolean hasSites() {
        return !sites.isEmpty();
    }


    public SiteConfig getSite(int index) {
        if (index < 0 || index >= sites.size()) {
            throw new IndexOutOfBoundsException(
                    String.format("Site index %d out of bounds [0, %d)", index, sites.size())
            );
        }
        return sites.get(index);
    }


    public void addSite(SiteConfig site) {
        if (site == null) {
            throw new IllegalArgumentException("Site cannot be null");
        }
        sites.add(site);
        logger.debug("Added site: {}", site);
    }


    public void clearSites() {
        sites.clear();
        logger.debug("Cleared all sites");
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
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Site name cannot be null or empty");
            }
            this.name = name.trim();
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            if (url == null || url.trim().isEmpty()) {
                throw new IllegalArgumentException("Site URL cannot be null or empty");
            }
            this.url = url.trim();
        }


        public boolean isValid() {
            return name != null && !name.isEmpty() &&
                    url != null && !url.isEmpty() &&
                    (url.startsWith("http://") || url.startsWith("https://"));
        }

        @Override
        public String toString() {
            return String.format("SiteConfig{name='%s', url='%s'}", name, url);
        }
    }

    @Override
    public String toString() {
        return String.format(
                "SitesList{sites=%d, userAgent='%s', baseUrl='%s'}",
                sites.size(), userAgent, baseUrl
        );
    }
}