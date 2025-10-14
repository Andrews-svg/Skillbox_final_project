package com.example.searchengine.models;

import jakarta.persistence.*;
import com.example.searchengine.dto.statistics.Data;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Objects;
import java.util.Optional;

@Entity
@Access(AccessType.FIELD)
@Table(name = "page_table", indexes = {
        @jakarta.persistence.Index(name = "idx_path", columnList = "path"),
        @jakarta.persistence.Index(name = "idx_url", columnList = "url")
})

@NamedQueries({
        @NamedQuery(name = "Page.findByStatus",
                query = "SELECT p FROM Page p WHERE p.status = :status"),
        @NamedQuery(name = "Page.findByUrl",
                query = "SELECT p FROM Page p WHERE p.url = :url"),
        @NamedQuery(name = "Page.findAll",
                query = "SELECT p FROM Page p")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Page {

    private static final Logger logger = LoggerFactory.getLogger(Page.class);


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String url;

    @Column(nullable = false, unique = true, length = 255)
    private String uri;

    @Column(nullable = false, length = 255)
    private String path;

    @Column(nullable = false)
    private Integer code = 200;

    @Column(length = 255)
    private String name;

    @Lob
    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(length = 255)
    private String title;

    @Column(length = 255)
    private String snippet;

    @Column(nullable = false)
    private float relevance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private boolean available = true;



    public Page(
            String url,
            String path,
            String uri,
            Site site,
            String content,
            Integer code,
            String name,
            String title,
            String snippet,
            float relevance,
            Status status,
            boolean available
    ) {
        this.url = url;
        this.path = path;
        this.uri = uri;
        this.site = site;
        this.content = content;
        this.code = code != null ? code : 200;
        this.name = name;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
        this.status = status != null ? status : Status.PENDING;
        this.available = available;
    }

    public static Page createFullPage(String url, String path, Integer code,
                                      String uri, String content,
                                      Site site, String title, String snippet,
                                      float relevance, Status status,
                                      String name, boolean available) {

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content must not be null or empty");
        }

        Page page = new Page(url, path, uri, site, content, code,
                name, title, snippet, relevance, status, available);

        return page;
    }


    public static Integer generateCode(Status status) {
        return switch (status) {
            case FAILED, SERVER_ERROR -> 500;
            case NOT_FOUND -> 404;
            default -> 200;
        };
    }

    public void markAsSuccessful() {
        setCode(200);
    }

    public void markAsNotFound() {
        setCode(404);
    }

    public void markAsServerError() {
        setCode(500);
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isAvailable() {
        return available;
    }

    public Long getPageId() {
        return this.id;
    }

    public void setPageId(Long id) {
        this.id = id;
    }

    public Integer getCode() {
        if (code == null) {
            throw new IllegalStateException("Code is not set");
        }
        return code;
    }

    public void setCode(Integer code) {
        if (code == null) {
            logger.warn("Code was null, setting to default value: 200");
            this.code = 200;
        } else if (code <= 0) {
            throw new IllegalArgumentException("Code must be non-negative");
        } else {
            logger.debug("Setting Code to: {}", code);
            this.code = code;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL must not be null or empty");
        }
        logger.debug("Setting URL to: {}", url);
        this.url = url;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            throw new IllegalArgumentException("URI must not be null or empty");
        }
        logger.debug("Setting URI to: {}", uri);
        this.uri = uri;
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path must not be null or empty");
        }
        logger.debug("Setting Path to: {}", path);
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("Title must not be null or empty");
        }
        logger.debug("Setting Title to: {}", title);
        this.title = title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public Optional<Integer> getCodeOptional() {
        return Optional.ofNullable(code);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content must not be null or empty");
        }
        logger.debug("Setting Content");
        this.content = content;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        if (site == null) {
            throw new IllegalArgumentException("Site must not be null");
        }
        logger.debug("Setting Site to: {}", site);
        this.site = site;
    }

    public float getRelevance() {
        return relevance;
    }

    public void setRelevance(float relevance) {
        if (relevance < 0) {
            throw new IllegalArgumentException("Relevance must be non-negative");
        }
        logger.debug("Setting Relevance to: {}", relevance);
        this.relevance = relevance;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        logger.debug("Setting Status to: {}", status);
        this.status = status;
    }

    public void setAvailable(boolean b) {
        this.available = b;
        logger.debug("Доступность страницы изменена на: " +
                "{}", b ? "Да" : "Нет");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Page page)) return false;
        return Float.compare(page.relevance, relevance) == 0 &&
                Objects.equals(id, page.id) &&
                Objects.equals(url, page.url) &&
                Objects.equals(path, page.path) &&
                Objects.equals(code, page.code) &&
                Objects.equals(title, page.title) &&
                Objects.equals(snippet, page.snippet) &&
                status == page.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url, path, code,
                title, snippet, relevance, status);
    }

    @Override
    public String toString() {
        return "Page{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", path='" + path + '\'' +
                ", code=" + code +
                ", title='" + title + '\'' +
                ", snippet='" + snippet + '\'' +
                ", relevance=" + relevance +
                ", status=" + status +
                '}';
    }
}