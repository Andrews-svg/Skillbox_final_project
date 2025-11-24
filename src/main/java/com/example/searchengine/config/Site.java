package com.example.searchengine.config;

import com.example.searchengine.models.Page;
import com.example.searchengine.models.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;



@Entity
@Table(name = "site")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Site {


    @NotNull
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    @NotNull
    @Enumerated(EnumType.STRING)
    private Status status;


    @NotNull
    @Column(nullable = false)
    private LocalDateTime statusTime;


    @NotBlank(message = "URL cannot be blank.")
    @Column(nullable = false, unique = true, length = 255)
    private String url;


    @NotBlank(message = "Name cannot be blank.")
    @Column(nullable = false, length = 255)
    private String name;


    @Column(length = 65535)
    private String lastError;


    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    private Set<Page> pages = new HashSet<>();


    public Site() {}


    public Site(Status status, LocalDateTime statusTime, String url, String name) {
        this.status = status;
        this.statusTime = statusTime;
        this.url = url;
        this.name = name;
    }

    public Site(Integer id, Status status, LocalDateTime statusTime,
                String url, String name, String lastError) {
        this.id = id;
        this.status = status;
        this.statusTime = statusTime;
        this.url = url;
        this.name = name;
        this.lastError = lastError;
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

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