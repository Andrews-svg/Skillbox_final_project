package com.example.searchengine.statisticsResponse;

public class ManagementResponse {
    private boolean indexingInProgress;
    private int indexedPagesCount;
    private int totalPagesCount;

    public boolean isIndexingInProgress() {
        return indexingInProgress;
    }

    public void setIndexingInProgress(boolean indexingInProgress) {
        this.indexingInProgress = indexingInProgress;
    }

    public int getIndexedPagesCount() {
        return indexedPagesCount;
    }

    public void setIndexedPagesCount(int indexedPagesCount) {
        this.indexedPagesCount = indexedPagesCount;
    }

    public int getTotalPagesCount() {
        return totalPagesCount;
    }

    public void setTotalPagesCount(int totalPagesCount) {
        this.totalPagesCount = totalPagesCount;
    }
}