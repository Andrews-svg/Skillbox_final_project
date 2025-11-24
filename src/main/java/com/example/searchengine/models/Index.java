package com.example.searchengine.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "search_index")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class Index implements Serializable {

    @NotNull
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @NotNull
    @Column(name = "search_rank")
    private float rank;


    public Index() {}


    public Index(Integer id, Page page, Lemma lemma, float rank) {
        this.id = id;
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public Lemma getLemma() {
        return lemma;
    }

    public void setLemma(Lemma lemma) {
        this.lemma = lemma;
    }

    public float getRank() {
        return rank;
    }

    public void setRank(float rank) {
        this.rank = rank;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Index index)) return false;
        return Float.compare(index.rank, rank) == 0 &&
                Objects.equals(id, index.id) &&
                Objects.equals(page, index.page) &&
                Objects.equals(lemma, index.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, page, lemma, rank);
    }


    @Override
    public String toString() {
        return "Index{" +
                "id=" + id +
                ", page=" + page.getId() +
                ", lemma=" + lemma.getId() +
                ", rank=" + rank +
                '}';
    }
}
