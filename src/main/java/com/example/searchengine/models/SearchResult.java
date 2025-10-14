package com.example.searchengine.models;

import com.example.searchengine.dto.statistics.Data;

import java.util.List;

public class SearchResult {

    private boolean success;
    private Long totalCount;
    private List<Data> data;

    public SearchResult(boolean success, long totalCount, List<Data> data) {
        this.success = success;
        this.totalCount = totalCount;
        this.data = data;
    }


    public boolean isSuccess() {
        return success;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public List<Data> getData() {
        return data;
    }


    @Override
    public String toString() {
        return "SearchResult{" +
                "success=" + success +
                ", totalCount=" + totalCount +
                ", data=" + data +
                '}';
    }
}