package com.example.searchengine.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;



@ConfigurationProperties(prefix = "indexing-settings")
public class SitesList {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String userAgent;
    private String referrer;
    private String webInterfacePath;
    private String baseUrl;


    public SitesList(String userAgent, String referrer,
                     String webInterfacePath, String baseUrl,
                     Map<Long, SiteConfig> sites) {
        this.userAgent = userAgent;
        this.referrer = referrer;
        this.webInterfacePath = webInterfacePath;
        this.baseUrl = baseUrl;
        this.sites = sites;
    }

    private Map<Long, SiteConfig> sites = new LinkedHashMap<>();


    public Map<Long, SiteConfig> getSites() {
        logger.debug("Returning all Sites: {}", sites);
        return Collections.unmodifiableMap(sites);
    }


    public boolean isAllowedDomain(String url) {
        for (SiteConfig site : sites.values()) {
            try {
                URI inputUri = new URI(url);
                URI allowedUri = new URI(site.getUrl());

                if (inputUri.getHost().equalsIgnoreCase(allowedUri.getHost())) {
                    return true;
                }
            } catch (URISyntaxException e) {
                logger.error("Ошибка парсинга URI: {}", e.getMessage());
            }
        }
        return false;
    }


    public Logger getLogger() {
        return logger;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public String getWebInterfacePath() {
        return webInterfacePath;
    }

    public void setWebInterfacePath(String webInterfacePath) {
        this.webInterfacePath = webInterfacePath;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setSites(Map<Long, SiteConfig> sites) {
        this.sites = sites;
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