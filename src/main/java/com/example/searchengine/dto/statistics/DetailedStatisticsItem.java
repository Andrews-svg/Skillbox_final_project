package com.example.searchengine.dto.statistics;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.searchengine.models.Status;
import org.springframework.stereotype.Component;

@Getter
@Component
public class DetailedStatisticsItem {

    private static final Logger logger = LoggerFactory.getLogger(DetailedStatisticsItem.class);

    private String url;
    private String name;
    private Status status;
    private long statusTime;
    private String error;
    private long pages;
    private long lemmas;

    public DetailedStatisticsItem(String url, String name,
                                  Status status, long statusTime, String error,
                                  long pages, long lemmas) {
        setUrl(url);
        setName(name);
        setStatus(status);
        setStatusTime(statusTime);
        setError(error);
        setPages(pages);
        setLemmas(lemmas);
        logger.info("DetailedStatisticsItem created: {}", this);
    }


    public DetailedStatisticsItem() {
    }

    public void setUrl(String url) {
        if (url == null || url.isEmpty()) {
            logger.error("Invalid URL value: {}", url);
            throw new IllegalArgumentException("URL cannot be null or empty.");
        }
        this.url = url;
    }

    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            logger.error("Invalid name value: {}", name);
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }
        this.name = name;
    }

    public void setStatus(Status status) {
        if (status == null) {
            logger.error("Invalid status value: {}", status);
            throw new IllegalArgumentException("Status cannot be null.");
        }
        this.status = status;
    }

    public void setStatusTime(long statusTime) {
        if (statusTime < 0) {
            logger.error("Invalid status time value: {}", statusTime);
            throw new IllegalArgumentException("Status time cannot be negative.");
        }
        this.statusTime = statusTime;
    }

    public void setError(String error) {
        if (error != null && error.length() > 255) {
            logger.error("Invalid error message length: {}", error);
            throw new IllegalArgumentException("Error message cannot exceed 255 characters.");
        }
        this.error = error;
    }

    public void setPages(long pages) {
        if (pages < 0) {
            logger.error("Invalid pages value: {}", pages);
            throw new IllegalArgumentException("Pages cannot be negative.");
        }
        this.pages = pages;
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
                ", status=" + status +
                ", statusTime=" + statusTime +
                ", error='" + error + '\'' +
                ", pages=" + pages +
                ", lemmas=" + lemmas +
                '}';
    }
}