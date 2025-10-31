package com.example.searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SitesList {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String userAgent;
    private String referrer;
    private String webInterfacePath;
    private String baseUrl;

    private Map<Integer, SiteConfig> sites = new LinkedHashMap<>();


    public Map<Integer, SiteConfig> getSites() {
        logger.debug("Returning all Sites: {}", sites);
        return Collections.unmodifiableMap(sites);
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