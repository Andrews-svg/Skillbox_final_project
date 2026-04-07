package com.example.searchengine.repositories;

import com.example.searchengine.config.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    List<Site> findByUrlContainingIgnoreCase(String partialUrl);

    boolean existsByUrl(String url);

    Optional<Site> findByUrl(String url);

    List<Site> findByStatus(com.example.searchengine.models.Status status);

    void deleteById(long id);

    @Query(value = "SELECT s.id AS site_id, COUNT(p.id) AS page_count "
            + "FROM site s LEFT JOIN page p ON s.id = p.site_id "
            + "WHERE s.id IN (:ids) GROUP BY s.id",
            nativeQuery = true)
    List<Object[]> countSitesGroupedByIdsRaw(@Param("ids") List<Long> ids);

    default Map<Long, Long> countSitesGroupedByIds(List<Long> ids) {
        List<Object[]> rawResults = countSitesGroupedByIdsRaw(ids);
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rawResults) {
            result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return result;
    }

    long count();
}