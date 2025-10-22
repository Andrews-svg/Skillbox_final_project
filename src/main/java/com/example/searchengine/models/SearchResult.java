package com.example.searchengine.models;

import com.example.searchengine.dto.statistics.Data;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Objects;


@RequiredArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class SearchResult {

    @NotNull
    private boolean success;
    @NotNull
    private Long totalCount;
    @NotNull
    private List<Data> data;
    @NotNull
    private String site;


    public SearchResult(boolean success, String site,
                        List<Data> data, Long totalCount) {
        this.success = success;
        this.site = site;
        this.data = data;
        this.totalCount = totalCount;
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
        if (!(o instanceof SearchResult)) return false;
        SearchResult other = (SearchResult) o;
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
