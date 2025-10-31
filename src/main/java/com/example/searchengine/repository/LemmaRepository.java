package com.example.searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.searchengine.models.Lemma;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    List<Lemma> findAllByLemma(String lemma);


    List<Lemma> findAllBySite_Id(Integer siteId);

    @Query("SELECT l.lemma, l.frequency FROM Lemma l WHERE l.lemma = " +
            ":lemma AND l.site.id = :siteId")
    List<Object[]> findByLemmaAndSiteId(
            @Param("lemma") String lemma,
            @Param("siteId") Integer siteId
    );
}
