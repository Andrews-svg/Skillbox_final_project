package com.example.searchengine.models;

import com.example.searchengine.config.Site;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;


@Indexed
@Entity
@Access(AccessType.FIELD)
@Table(name = "page", indexes = {
        @jakarta.persistence.Index(name = "idx_path", columnList = "path(255)")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull
    private long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    @NotNull
    private String path;

    @Column(nullable = false)
    @NotNull
    private long code;

    @FullTextField(analyzer = "standard")
    @Lob
    @Column(nullable = false)
    @NotNull
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "site_id", nullable = false)
    @NotNull
    private Site site;

    public Page() {}

    public Page(long id, String path, long code, String content, Site site) {
        this.id = id;
        this.path = path;
        this.code = code;
        this.content = content;
        this.site = site;
    }

    public Page(String path, long code, String content, Site site) {
        this.path = path;
        this.code = code;
        this.content = content;
        this.site = site;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}