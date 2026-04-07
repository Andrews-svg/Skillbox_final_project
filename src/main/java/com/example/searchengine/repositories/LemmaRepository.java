package com.example.searchengine.repositories;

import com.example.searchengine.config.Site;
import com.example.searchengine.models.Index;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.searchengine.models.Lemma;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {

    List<Lemma> findAllByLemma(String lemma);
    List<Lemma> findAllBySite_Id(long siteId);
    List<Index> findIndexesByLemma(Lemma lemma);
    List<Lemma> findDistinctBySite(Site site);
    Optional<Lemma> findOneByLemmaAndSiteId(String lemma, long siteId);
    @Query(value = "SELECT site_id, COUNT(*) FROM lemma WHERE site_id IN (:ids) GROUP BY site_id", nativeQuery = true)
    List<Object[]> countLemmasGroupedBySiteIds(@Param("ids") List<Long> ids);

    default Map<Long, Long> countLemmasGroupedBySiteIdsWithConversion(List<Long> ids) {
        List<Object[]> rawResults = countLemmasGroupedBySiteIds(ids);
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rawResults) {
            result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return result;
    }
}
