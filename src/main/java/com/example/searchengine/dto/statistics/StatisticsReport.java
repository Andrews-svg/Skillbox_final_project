package com.example.searchengine.dto.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StatisticsReport {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsReport.class);

    private long sites;
    private long pages;
    private long lemmas;
    private boolean isIndexing;

    public StatisticsReport() {}

    public long getSites() {
        return sites;
    }

    public void setSites(long sites) {
        validateNonNegative(sites, "Number of sites");
        this.sites = sites;
    }

    public long getPages() {
        return pages;
    }

    public void setPages(long pages) {
        validateNonNegative(pages, "Number of pages");
        this.pages = pages;
    }

    public long getLemmas() {
        return lemmas;
    }

    public void setLemmas(long lemmas) {
        validateNonNegative(lemmas, "Number of lemmas");
        this.lemmas = lemmas;
    }

    public boolean isIndexing() {
        return isIndexing;
    }

    public void setIndexing(boolean isIndexing) {
        this.isIndexing = isIndexing;
    }

    public void addPages(long pages) {
        validateNonNegative(pages, "Number of pages to add");
        this.pages += pages;
        logger.info("Added pages: {}. New pageNumber: {}", pages, this.pages);
    }

    public void addLemmas(long lemmas) {
        validateNonNegative(lemmas, "Number of lemmas to add");
        this.lemmas += lemmas;
        logger.info("Added lemmas: {}. New lemmaNumber: {}", lemmas, this.lemmas);
    }

    public long totalSitesAndPages() {
        return sites + pages;
    }

    public long totalSitesAndLemmas() {
        return sites + lemmas;
    }

    private void validateNonNegative(long number, String fieldName) {
        if (number < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative.");
        }
    }

    @Override
    public String toString() {
        return "StatisticsReport{" +
                "sites=" + sites +
                ", pages=" + pages +
                ", lemmas=" + lemmas +
                ", isIndexing=" + isIndexing +
                '}';
    }
}