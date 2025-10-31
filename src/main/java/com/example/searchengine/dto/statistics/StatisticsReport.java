package com.example.searchengine.dto.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StatisticsReport {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsReport.class);

    private Integer siteNumber;
    private Integer pageNumber;
    private Integer lemmaNumber;
    private boolean isIndexing;

    public StatisticsReport(Integer siteNumber, Integer pageNumber, Integer lemmaNumber, Boolean isIndexing) {
        this.siteNumber = siteNumber != null ? siteNumber : 0;
        this.pageNumber = pageNumber != null ? pageNumber : 0;
        this.lemmaNumber = lemmaNumber != null ? lemmaNumber : 0;
        this.isIndexing = isIndexing != null ? isIndexing : false;

        logger.info("Total object created: siteNumber={}, pageNumber={}, lemmaNumber={}, isIndexing={}",
                siteNumber, pageNumber, lemmaNumber, isIndexing);
    }


    public Integer getSiteNumber() {
        return siteNumber;
    }

    public void setSiteNumber(Integer siteNumber) {
        if (siteNumber < 0) {
            throw new IllegalArgumentException("Number of sites cannot be negative.");
        }
        this.siteNumber = siteNumber;
    }

    public long getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("Number of pages cannot be negative.");
        }
        this.pageNumber = pageNumber;
    }

    public Integer getLemmaNumber() {
        return lemmaNumber;
    }

    public void setLemmaNumber(Integer lemmaNumber) {
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

    public int getPages() {
        return pageNumber;
    }

    public int getLemmas() {
        return lemmaNumber;
    }

    public void addPages(int pages) {
        if (pages < 0) {
            throw new IllegalArgumentException("Number of pages to add cannot be negative.");
        }
        this.pageNumber += pages;
        logger.info("Added pages: {}. New pageNumber: {}", pages, this.pageNumber);
    }

    public void addLemmas(int lemmas) {
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