package com.example.searchengine.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "index_table")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class Index implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "search_rank", precision=10, scale=2)
    private BigDecimal rank;

    @Column(name = "last_modified")
    private LocalDateTime lastModified;

    @Column(name = "available")
    private Boolean available;


    public Index(Page page, Lemma lemma, Site site, BigDecimal rank) {
        this.page = page;
        this.lemma = lemma;
        this.site = site;
        this.rank = rank;
    }

    public Index() {}

    public Index(Long id) {
        this.id = id;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        if (page == null || page.getId() == null || page.getId() <= 0) {
            throw new IllegalArgumentException("Page object must have a valid positive ID.");
        }
        this.page = page;
    }

    public Lemma getLemma() {
        return lemma;
    }

    public void setLemma(Lemma lemma) {
        if (lemma == null || lemma.getId() == null || lemma.getId() <= 0) {
            throw new IllegalArgumentException("Lemma object must have a valid positive ID.");
        }
        this.lemma = lemma;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        if (site == null || site.getId() <= 0) {
            throw new IllegalArgumentException("Site object must have a valid positive ID.");
        }
        this.site = site;
    }

    public BigDecimal getRank() {
        return rank;
    }

    public void setRank(BigDecimal rank) {
        if (rank == null || rank.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Rank value must be non-negative.");
        }
        this.rank = rank;
    }

    public Boolean isAvailable() {
        return available; }

    public void setAvailable(Boolean available) {
        this.available = available; }

    @Override
    public String toString() {
        return "Index{" +
                "id=" + id +
                ", page=" + page.getId() +
                ", lemma=" + lemma.getId() +
                ", site=" + site.getId() +
                ", rank=" + rank +
                ", lastModified=" + lastModified +
                '}';
    }
}