package com.example.searchengine.dto.adminLogs;

import java.util.Objects;

public class ZeroResultQueryDto {
    private String query;
    private Long count;

    public ZeroResultQueryDto(String query, Long count) {
        this.query = query;
        this.count = count;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ZeroResultQueryDto that = (ZeroResultQueryDto) o;
        return Objects.equals(query, that.query) &&
                Objects.equals(count, that.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, count);
    }

    @Override
    public String toString() {
        return "ZeroResultQueryDto{" +
                "query='" + query + '\'' +
                ", count=" + count +
                '}';
    }
}
