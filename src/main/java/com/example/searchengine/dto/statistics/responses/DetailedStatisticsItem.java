package com.example.searchengine.dto.statistics.responses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetailedStatisticsItem {

    private static final Logger logger = LoggerFactory.getLogger(DetailedStatisticsItem.class);

    private String url;
    private String name;
    private String status;
    private Long statusTime;
    private String error;
    private long pages;
    private long lemmas;

    public DetailedStatisticsItem(String url, String name,
                                  String status, Long statusTime, String error,
                                  long pages, long lemmas) {

        if (url == null || url.isEmpty()) {
            logger.error("Invalid URL - value: {}", url);
            throw new IllegalArgumentException("URL cannot be null or empty.");
        }
        this.url = url;
        if (name == null || name.isEmpty()) {
            logger.error("Invalid name - value: {}", name);
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }
        this.name = name;
        if (status == null || status.isEmpty()) {
            logger.error("Invalid status - value: {}", status);
            throw new IllegalArgumentException("Status cannot be null or empty.");
        }
        this.status = status;
        this.statusTime = statusTime;
        this.error = error;

        if (pages < 0) {
            logger.error("Invalid pages - value: {}", pages);
            throw new IllegalArgumentException("Pages cannot be negative.");
        }
        this.pages = pages;

        if (lemmas < 0) {
            logger.error("Invalid lemmas - value: {}", lemmas);
            throw new IllegalArgumentException("Lemmas cannot be negative.");
        }
        this.lemmas = lemmas;
        logger.info("DetailedStatisticsItem created for site: {}", url);
    }

    public DetailedStatisticsItem() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (url == null || url.isEmpty()) {
            logger.error("Invalid URL value: {}", url);
            throw new IllegalArgumentException("URL cannot be null or empty.");
        }
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            logger.error("Invalid name value: {}", name);
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (status == null || status.isEmpty()) {
            logger.error("Invalid status value: {}", status);
            throw new IllegalArgumentException("Status cannot be null or empty.");
        }
        this.status = status;
    }

    public Long getStatusTime() {
        return statusTime;
    }

    public void setStatusTime(Long statusTime) {
        this.statusTime = statusTime;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public long getPages() {
        return pages;
    }

    public void setPages(long pages) {
        if (pages < 0) {
            logger.error("Invalid pages value: {}", pages);
            throw new IllegalArgumentException("Pages cannot be negative.");
        }
        this.pages = pages;
    }

    public long getLemmas() {
        return lemmas;
    }

    public void setLemmas(long lemmas) {
        if (lemmas < 0) {
            logger.error("Invalid lemmas value: {}", lemmas);
            throw new IllegalArgumentException("Lemmas cannot be negative.");
        }
        this.lemmas = lemmas;
    }

    @Override
    public String toString() {
        return "DetailedStatisticsItem{" +
                "url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", statusTime=" + statusTime +
                ", error='" + error + '\'' +
                ", pages=" + pages +
                ", lemmas=" + lemmas +
                '}';
    }
}