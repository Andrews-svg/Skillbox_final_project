package com.example.searchengine.dto.statistics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.searchengine.models.Site;

import java.io.Serializable;


@Setter
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
public class Data implements Comparable<Data>, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(Data.class);

    @JsonProperty("id")
    private Long id;

    @JsonProperty("site")
    private Site site;

    @JsonProperty("siteName")
    private String siteName;

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("path")
    private String path;

    @JsonProperty("title")
    private String title;

    @JsonProperty("snippet")
    private String snippet;

    @JsonProperty("relevance")
    private double relevance;

    @Override
    public int compareTo(Data other) {
        if (Math.abs(this.relevance - other.getRelevance()) < 0.00001) {
            return 0;
        }
        return Double.compare(this.relevance, other.getRelevance());
    }
}