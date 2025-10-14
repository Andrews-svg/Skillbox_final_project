package com.example.searchengine.dto.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StatisticsReport {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsReport.class);

    private Long siteNumber;
    private Long pageNumber;
    private Long lemmaNumber;
    private boolean isIndexing;

    public StatisticsReport(Long siteNumber, Long pageNumber, Long lemmaNumber, Boolean isIndexing) {
        this.siteNumber = siteNumber != null ? siteNumber : 0;
        this.pageNumber = pageNumber != null ? pageNumber : 0;
        this.lemmaNumber = lemmaNumber != null ? lemmaNumber : 0;
        this.isIndexing = isIndexing != null ? isIndexing : false;

        logger.info("Total object created: siteNumber={}, pageNumber={}, lemmaNumber={}, isIndexing={}",
                siteNumber, pageNumber, lemmaNumber, isIndexing);
    }


    public Long getSiteNumber() {
        return siteNumber;
    }

    public void setSiteNumber(Long siteNumber) {
        if (siteNumber < 0) {
            throw new IllegalArgumentException("Number of sites cannot be negative.");
        }
        this.siteNumber = siteNumber;
    }

    public long getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(long pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("Number of pages cannot be negative.");
        }
        this.pageNumber = pageNumber;
    }

    public Long getLemmaNumber() {
        return lemmaNumber;
    }

    public void setLemmaNumber(Long lemmaNumber) {
        if (lemmaNumber < 0) {
            throw new IllegalArgumentException("Number of lemmas cannot be negative.");
        }
        this.lemmaNumber = lemmaNumber;
    }

    public boolean isIndexing() {
        return isIndexing;
    }

    public void setIndexing(boolean indexing) {
        isIndexing = indexing;
    }

    public long getPages() {
        return pageNumber;
    }

    public long getLemmas() {
        return lemmaNumber;
    }

    public void addPages(long pages) {
        if (pages < 0) {
            throw new IllegalArgumentException("Number of pages to add cannot be negative.");
        }
        this.pageNumber += pages;
        logger.info("Added pages: {}. New pageNumber: {}", pages, this.pageNumber);
    }

    public void addLemmas(long lemmas) {
        if (lemmas < 0) {
            throw new IllegalArgumentException("Number of lemmas to add cannot be negative.");
        }
        this.lemmaNumber += lemmas;
        logger.info("Added lemmas: {}. New lemmaNumber: {}", lemmas, this.lemmaNumber);
    }

    @Override
    public String toString() {
        return "Total{" +
                "siteNumber=" + siteNumber +
                ", pageNumber=" + pageNumber +
                ", lemmaNumber=" + lemmaNumber +
                ", isIndexing=" + isIndexing +
                '}';
    }
}