package com.example.searchengine.models;

import com.example.searchengine.dto.statistics.Data;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;


public class SearchResult {

    @NotNull
    private boolean success;
    @NotNull
    private Integer totalCount;

    @NotNull
    private List<Data> data;
    @NotNull
    private String site;

    public SearchResult(boolean b, Integer totalResults, List<Data> paginatedResults) {
    }

    public SearchResult() {

    }


    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public List<Data> getData() {
        return data;
    }

    public void setData(List<Data> data) {
        this.data = data;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }


    @Override
    public String toString() {
        return "SearchResult{" +
                "success=" + success +
                ", totalCount=" + totalCount +
                ", data=" + data +
                ", site='" + site + '\'' +
                '}';
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchResult other)) return false;
        return success == other.success &&
                Objects.equals(totalCount, other.totalCount) &&
                Objects.equals(data, other.data) &&
                Objects.equals(site, other.site);
    }


    @Override
    public int hashCode() {
        return Objects.hash(success, totalCount, data, site);
    }
}
