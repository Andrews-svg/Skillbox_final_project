package com.example.searchengine.dto.statistics.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;


public class SearchResponse {

    @JsonProperty("result")
    private final boolean result;

    @JsonProperty("count")
    private final long count;

    @JsonProperty("data")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<Data> data;

    @JsonProperty("error")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String error;


    private SearchResponse(boolean result, long count,
                           List<Data> data, String error) {
        this.result = result;
        this.count = count;
        this.data = data != null ? data : Collections.emptyList();
        this.error = error;
    }


    public static SearchResponse success(long count, List<Data> data) {
        return new SearchResponse(true, count, data, null);
    }


    public static SearchResponse error(String errorMessage) {
        return new SearchResponse(false, 0, Collections.emptyList(), errorMessage);
    }


    public boolean isResult() {
        return result;
    }

    public long getCount() {
        return count;
    }

    public List<Data> getData() {
        return data;
    }

    public String getError() {
        return error;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResponse that = (SearchResponse) o;
        return result == that.result &&
                count == that.count &&
                java.util.Objects.equals(data, that.data) &&
                java.util.Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(result, count, data, error);
    }

    @Override
    public String toString() {
        if (error != null) {
            return "SearchResponse{result=false, error='" + error + "'}";
        }
        return "SearchResponse{" +
                "result=" + result +
                ", count=" + count +
                ", data.size=" + data.size() +
                '}';
    }
}