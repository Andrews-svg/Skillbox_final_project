package com.example.searchengine.repository;

import com.example.searchengine.config.Site;
import com.example.searchengine.models.Index;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.searchengine.models.Lemma;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    List<Lemma> findAllByLemma(String lemma);
    List<Lemma> findAllBySite_Id(Integer siteId);
    List<Index> findIndexesByLemma(Lemma lemma);
    List<Lemma> findDistinctBySite(Site site);

    @Query("SELECT l FROM Lemma l WHERE l.lemma = :lemma AND l.site.id = :siteId")
    Optional<Lemma> findByLemmaAndSiteId(@Param("lemma") String lemma, @Param("siteId") Integer siteId);

    @Query("SELECT l FROM Lemma l WHERE l.form = :form AND l.site.id = :siteId")
    Optional<Lemma> findByBaseFormAndSiteId(@Param("form") String baseForm, @Param("siteId") Integer siteId);

}
