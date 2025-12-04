package com.example.searchengine.repository;

import com.example.searchengine.config.Site;
import com.example.searchengine.models.Index;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.searchengine.models.Lemma;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {

    List<Lemma> findAllByLemma(String lemma);
    List<Lemma> findAllBySite_Id(long siteId);
    List<Index> findIndexesByLemma(Lemma lemma);
    List<Lemma> findDistinctBySite(Site site);
    Optional<Lemma> findOneByLemmaAndSiteId(String lemma, long siteId);
}
