package com.example.searchengine.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchData {

    @JsonProperty("site")
    private String site;

    @JsonProperty("siteName")
    private String siteName;

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("title")
    private String title;

    @JsonProperty("snippet")
    private String snippet;

    @JsonProperty("relevance")
    private double relevance;

    public SearchData() {
    }

    public SearchData(String site, String siteName, String uri,
                      String title, String snippet, double relevance) {
        this.site = site;
        this.siteName = siteName;
        this.uri = uri;
        this.title = title != null ? title : "Без названия";
        this.snippet = snippet;
        this.relevance = relevance;
    }


    public String getSite() { return site; }
    public String getSiteName() { return siteName; }
    public String getUri() { return uri; }
    public String getTitle() { return title; }
    public String getSnippet() { return snippet; }
    public double getRelevance() { return relevance; }


    public void setSite(String site) { this.site = site; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public void setUri(String uri) { this.uri = uri; }
    public void setTitle(String title) { this.title = title != null ? title : "Без названия"; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
    public void setRelevance(double relevance) { this.relevance = relevance; }
}
