package com.example.searchengine.dto.statistics.responses;

import com.example.searchengine.models.Site;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;


public class Data {

    @JsonProperty("site")
    private String site;

    @JsonProperty("siteName")
    private String siteName;

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("title")
    private String title;

    @JsonProperty("snippet")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String snippet;

    @JsonProperty("relevance")
    private double relevance;


    public Data() {
    }


    public Data(String site, String siteName, String uri,
                String title, String snippet, double relevance) {
        this.site = site != null ? site : "";
        this.siteName = siteName != null ? siteName : "";
        this.uri = uri != null ? uri : "";
        this.title = title != null ? title : "Без названия";
        this.snippet = snippet != null ? snippet : "";
        this.relevance = normalizeRelevance(relevance);
    }


    public Data(Site siteObj,
                String siteName, String uri,
                String title, String snippet, double relevance) {
        this.site = (siteObj != null && siteObj.getUrl() != null)
                ? siteObj.getUrl() : "";
        this.siteName = siteName != null ? siteName : "";
        this.uri = uri != null ? uri : "";
        this.title = title != null ? title : "Без названия";
        this.snippet = snippet != null ? snippet : "";
        this.relevance = normalizeRelevance(relevance);
    }


    private double normalizeRelevance(double relevance) {
        if (Double.isNaN(relevance)) return 0.0;
        if (relevance < 0.0) return 0.0;
        if (relevance > 1.0) return 1.0;
        return Math.round(relevance * 10000.0) / 10000.0;
    }


    public String getSite() {
        return site;
    }

    public String getSiteName() {
        return siteName;
    }

    public String getUri() {
        return uri;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public double getRelevance() {
        return relevance;
    }


    public void setSite(String site) {
        this.site = site != null ? site : "";
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName != null ? siteName : "";
    }

    public void setUri(String uri) {
        this.uri = uri != null ? uri : "";
    }

    public void setTitle(String title) {
        this.title = title != null ? title : "Без названия";
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet != null ? snippet : "";
    }

    public void setRelevance(double relevance) {
        this.relevance = normalizeRelevance(relevance);
    }

    @Override
    public String toString() {
        return "Data{" +
                "site='" + site + '\'' +
                ", siteName='" + siteName + '\'' +
                ", uri='" + uri + '\'' +
                ", title='" + title + '\'' +
                ", snippet='" + snippet + '\'' +
                ", relevance=" + relevance +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Data data = (Data) o;
        return Double.compare(data.relevance, relevance) == 0 &&
                site.equals(data.site) &&
                siteName.equals(data.siteName) &&
                uri.equals(data.uri) &&
                title.equals(data.title) &&
                snippet.equals(data.snippet);
    }

    @Override
    public int hashCode() {
        int result;
        result = site.hashCode();
        result = 31 * result + siteName.hashCode();
        result = 31 * result + uri.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + snippet.hashCode();
        result = 31 * result + Double.hashCode(relevance);
        return result;
    }
}