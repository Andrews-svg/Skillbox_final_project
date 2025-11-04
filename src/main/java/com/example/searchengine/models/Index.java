package com.example.searchengine.models;

import com.example.searchengine.config.Site;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "index")
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
    @Column(name = "search_rank", precision=10, scale=2)
    private float rank;


    @Override
    public String toString() {
        return "Index{" +
                "id=" + id +
                ", page=" + page.getId() +
                ", lemma=" + lemma.getId() +
                ", rank=" + rank +
                '}';
    }

    public void setSite(Site site) {
    }
}