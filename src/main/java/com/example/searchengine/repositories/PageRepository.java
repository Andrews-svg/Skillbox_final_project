package com.example.searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.searchengine.models.Page;
import com.example.searchengine.config.Site;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    boolean existsByPath(String path);
    Optional<Page> findByPath(String path);
    List<Page> findAllBySite(Site site);
    void deleteBySite(Site site);
    void deleteById(Long id);
    List<Page> findAllBySiteIn(List<Site> batch);
    @Query(value = "SELECT site_id, COUNT(*) FROM page WHERE site_id IN (:ids) GROUP BY site_id", nativeQuery = true)
    List<Object[]> countPagesGroupedBySiteIds(@Param("ids") List<Long> ids);

    default Map<Long, Long> countPagesGroupedBySiteIdsWithConversion(List<Long> ids) {
        List<Object[]> rawResults = countPagesGroupedBySiteIds(ids);
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rawResults) {
            result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return result;
    }
}



