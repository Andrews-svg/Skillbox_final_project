package com.example.searchengine.dto.statistics;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.searchengine.config.Site;

import java.io.Serializable;

public class Data implements Comparable<Data>, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(Data.class);

    @JsonProperty("id")
    private Integer id;

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


    public Data() {
    }


    public Data(Integer id, Site site, String siteName, String uri,
                String path, String title, String snippet, double relevance) {
        this.id = id;
        this.site = site;
        this.siteName = siteName;
        this.uri = uri;
        this.path = path;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
    }

    // Геттеры и сеттеры

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public double getRelevance() {
        return relevance;
    }

    public void setRelevance(double relevance) {
        this.relevance = relevance;
    }


    @Override
    public int compareTo(Data other) {
        if (Math.abs(this.relevance - other.getRelevance()) < 0.00001) {
            return 0;
        }
        return Double.compare(this.relevance, other.getRelevance());
    }
}