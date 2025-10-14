package com.example.searchengine.request;

import jakarta.validation.constraints.NotEmpty;
import com.example.searchengine.models.Index;
import com.example.searchengine.models.Lemma;

import java.util.List;


public class FilterRequest {

    @NotEmpty(message = "Список лемм не должен быть пустым")
    private List<Lemma> lemmaList;

    @NotEmpty(message = "Список индексов не должен быть пустым")
    private List<Index> indexes;

    public List<Lemma> getLemmaList() {
        return lemmaList;
    }

    public void setLemmaList(List<Lemma> lemmaList) {
        this.lemmaList = lemmaList;
    }

    public List<Index> getIndexes() {
        return indexes;
    }

    public void setIndexes(List<Index> indexes) {
        this.indexes = indexes;
    }
}