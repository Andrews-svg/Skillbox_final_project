package com.example.searchengine.dto.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;

public class SearchResponse {

    @JsonProperty("result")
    private final boolean result;

    @JsonProperty("count")
    private final int count;

    @JsonProperty("data")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<SearchData> data;

    @JsonProperty("error")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String error;

    public SearchResponse(boolean result, int count,
                          List<SearchData> data, String error) {
        this.result = result;
        this.count = count;
        this.data = data != null ? data : Collections.emptyList();
        this.error = error;
    }

    public static SearchResponse success(int count, List<SearchData> data) {
        return new SearchResponse(true, count, data, null);
    }

    public static SearchResponse error(String errorMessage) {
        return new SearchResponse(false, 0, Collections.emptyList(), errorMessage);
    }

    public String getError() {
        return error;
    }

    public List<SearchData> getData() {
        return data;
    }

    public int getCount() {
        return count;
    }

    public boolean isResult() {
        return result;
    }

}
