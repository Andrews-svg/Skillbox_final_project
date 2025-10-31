package com.example.searchengine.models;

import com.example.searchengine.config.Site;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(
        name = "lemma",
        uniqueConstraints = @UniqueConstraint(columnNames = {"lemma", "site_id"}),

        indexes = {
                @Index(name = "idx_lemma_name", columnList = "lemma"),
                @Index(name = "idx_site_id", columnList = "site_id"),
                @Index(name = "idx_lemma_and_site", columnList = "lemma, site_id")
        }
)

@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Lemma implements Serializable, Comparable<Lemma> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private Site site;


    public Lemma(int id, String lemma, int frequency, Site site) {
        this.id = id;
        this.lemma = lemma;
        this.frequency = frequency;
        this.site = site;
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
                '}';
    }
}
