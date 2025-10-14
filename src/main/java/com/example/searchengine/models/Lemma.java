package com.example.searchengine.models;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.util.Objects;



@Entity
@Table(
        name = "lemma_table",
        uniqueConstraints = @UniqueConstraint(columnNames = {"lemma", "site_id"}),
        indexes = @Index(name = "idx_lemma_name", columnList = "lemma")
)
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Lemma implements Serializable, Comparable<Lemma> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String lemma;

    @Column(nullable = false)
    private double frequency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private Site site;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;


    public Lemma() {}


    public Lemma(String lemma, double frequency, Site site, Status status) {
        this.setLemma(lemma);
        this.setFrequency(frequency);
        this.setSite(site);
        this.status = status;
    }


    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        if (lemma == null || lemma.isBlank()) {
            throw new IllegalArgumentException("Lemmas cannot be blank.");
        }
        this.lemma = lemma;
    }

    public double getFrequency() {
        return frequency;
    }

    public void setFrequency(double frequency) {
        if (frequency < 0) {
            throw new IllegalArgumentException("Frequency must be non-negative.");
        }
        this.frequency = frequency;
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
        if (site == null || site.getId() == null || site.getId() <= 0) {
            throw new IllegalArgumentException("Invalid site provided for the lemma.");
        }
        this.site = site;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }


    @Override
    public int compareTo(Lemma other) {
        return this.lemma.compareTo(other.lemma);
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Lemma that)) return false;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Lemma{" +
                "id=" + id +
                ", lemma='" + lemma + '\'' +
                ", frequency=" + frequency +
                ", site=" + site +
                ", status=" + status +
                '}';
    }
}


