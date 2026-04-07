package com.example.searchengine.repositories;

import com.example.searchengine.models.Lemma;
import com.example.searchengine.models.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {

    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);

    List<Lemma> findAllByLemmaInAndSite(Set<String> lemmas, Site site);

    List<Lemma> findBySite(Site site);

    void deleteBySite(Site site);

    long countBySite(Site site);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO lemma (lemma, frequency, site_id) " +
            "VALUES (?1, 1, ?2) " +
            "ON DUPLICATE KEY UPDATE frequency = frequency + 1",
            nativeQuery = true)
    void upsert(String lemma, Long siteId);
}