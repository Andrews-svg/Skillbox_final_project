package com.example.searchengine.statisticsResponse;

import com.example.searchengine.dto.statistics.Data;
import java.util.List;
import java.util.Objects;

public class SearchResponse {
    private final boolean result;
    private final long count;
    private final List<Data> data;

    public SearchResponse(boolean result, long count, List<Data> data) {
        this.result = result;
        this.count = count;
        this.data = data;
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

    @Override
    public String toString() {
        return "SearchResponse{" +
                "result=" + result +
                ", count=" + count +
                ", data=" + data +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchResponse)) return false;
        SearchResponse that = (SearchResponse) o;
        return result == that.result && count == that.count && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, count, data);
    }
}
