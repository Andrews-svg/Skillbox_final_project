package com.example.searchengine.dto.statistics;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.searchengine.models.Site;

import java.io.Serializable;
import java.util.Objects;

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


    public Data(Long id, Site site, String siteName,
                String uri, String title,
                String snippet, double relevance) {
        this.id = id;
        setSite(site);
        setSiteName(siteName);
        setUri(uri);
        setTitle(title);
        setSnippet(snippet);
        setRelevance(relevance);
        logger.info("Data object created: {}", this);
    }


    public Data() {
    }

    public static class Builder {
        private Long id;
        private Site site;
        private String siteName;
        private String uri;
        private String path;
        private String title;
        private String snippet;
        private double relevance;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder site(Site site) {
            this.site = site;
            return this;
        }

        public Builder siteName(String siteName) {
            this.siteName = siteName;
            return this;
        }

        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder snippet(String snippet) {
            this.snippet = snippet;
            return this;
        }

        public Builder relevance(double relevance) {
            this.relevance = relevance;
            return this;
        }

        public Data build() {
            return new Data(id, site, siteName, uri,
                    title, snippet, relevance);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        if (site == null) {
            logger.error("Invalid site value: {}", site);
            throw new IllegalArgumentException("Site cannot be null.");
        }
        this.site = site;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        if (siteName == null || siteName.isEmpty()) {
            logger.error("Invalid site name value: {}", siteName);
            throw new IllegalArgumentException(
                    "Site name cannot be null or empty.");
        }
        this.siteName = siteName;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            logger.error("Invalid URI value: {}", uri);
            throw new IllegalArgumentException("URI cannot be null or empty.");
        }
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
        if (title == null || title.isEmpty()) {
            logger.error("Invalid title value: {}", title);
            throw new IllegalArgumentException(
                    "Title cannot be null or empty.");
        }
        this.title = title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        if (snippet == null || snippet.isEmpty()) {
            logger.error("Invalid snippet value: {}", snippet);
            throw new IllegalArgumentException(
                    "Snippet cannot be null or empty.");
        }
        this.snippet = snippet;
    }

    public double getRelevance() {
        return relevance;
    }

    public void setRelevance(double relevance) {
        if (relevance < 0 || relevance > 1) {
            logger.error("Invalid relevance value: {}", relevance);
            throw new IllegalArgumentException(
                    "Relevance must be between 0 and 1.");
        }
        this.relevance = relevance;
        logger.info("Relevance updated to: {}", relevance);
    }

    public String getUrl() {
        return uri;
    }

    public Integer getCode() {
        return (uri != null && !uri.isEmpty()) ? 1 : 0;
    }


    @Override
    public int compareTo(Data other) {
        return Double.compare(this.relevance, other.getRelevance());
    }


    @Override
    public String toString() {
        return "Data{" +
                "id=" + id +
                ", site=" + site +
                ", siteName='" + siteName + '\'' +
                ", uri='" + uri + '\'' +
                ", path='" + path + '\'' +
                ", title='" + title + '\'' +
                ", snippet='" + snippet + '\'' +
                ", relevance=" + relevance +
                '}';
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Data)) return false;
        Data that = (Data) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.site, that.site) &&
                Objects.equals(this.siteName, that.siteName) &&
                Objects.equals(this.uri, that.uri) &&
                Objects.equals(this.path, that.path) &&
                Objects.equals(this.title, that.title) &&
                Objects.equals(this.snippet, that.snippet) &&
                Double.doubleToLongBits(this.relevance) ==
                        Double.doubleToLongBits(that.relevance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, site, siteName, uri,
                path, title, snippet, relevance);
    }
}
