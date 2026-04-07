package com.example.searchengine.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "site")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    private Status status;

    @NotNull
    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String url;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Page> pages = new HashSet<>();

    public Site() {}

    public Site(Status status, LocalDateTime statusTime, String url, String name) {
        this.status = status;
        this.statusTime = statusTime;
        this.url = url;
        this.name = name;
    }

    public Site(String name, String url) {
        this.name = name;
        this.url = url;
        this.status = Status.INDEXING;
        this.statusTime = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.statusTime == null) {
            this.statusTime = LocalDateTime.now();
        }
    }

    public void addPage(Page page) {
        pages.add(page);
        page.setSite(this);
    }

    public void updateStatusTime() {
        this.statusTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Europe/Moscow")
    public LocalDateTime getStatusTime() {
        return statusTime;
    }

    public void setStatusTime(LocalDateTime statusTime) {
        this.statusTime = statusTime;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Set<Page> getPages() {
        return pages;
    }

    public void setPages(Set<Page> pages) {
        this.pages = pages;
    }
}