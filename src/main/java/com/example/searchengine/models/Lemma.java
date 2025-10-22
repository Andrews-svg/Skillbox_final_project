package com.example.searchengine.models;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
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
