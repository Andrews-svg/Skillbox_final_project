package com.example.searchengine.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "page", indexes = {
        @jakarta.persistence.Index(name = "idx_path", columnList = "path")
})
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT NOT NULL")
    @NotNull
    private String path;

    @Column(nullable = false)
    @NotNull
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT NOT NULL")
    @NotNull
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    @NotNull
    private Site site;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Index> indices = new HashSet<>();

    public Page() {}

    public Page(String path, int code, String content, Site site) {
        this.path = path;
        this.code = code;
        this.content = content;
        this.site = site;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public Set<Index> getIndices() {
        return indices;
    }

    public void setIndices(Set<Index> indices) {
        this.indices = indices;
    }

    public void addIndex(Index index) {
        indices.add(index);
        index.setPage(this);
    }

    public void removeIndex(Index index) {
        indices.remove(index);
        index.setPage(null);
    }
}