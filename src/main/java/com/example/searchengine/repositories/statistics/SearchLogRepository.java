package com.example.searchengine.repositories;

import com.example.searchengine.models.SearchQueryLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchQueryLog, Long> {


    @Query("SELECT l.query, COUNT(l) as cnt FROM SearchQueryLog l " +
            "GROUP BY l.query ORDER BY cnt DESC")
    List<Object[]> findTopQueries(Pageable pageable);


    @Query("SELECT l.query, COUNT(l) FROM SearchQueryLog l " +
            "WHERE l.resultsCount = 0 GROUP BY l.query")
    List<Object[]> findZeroResultQueries();


    @Query("SELECT COUNT(l) FROM SearchQueryLog l WHERE l.clicked = true")
    long countClicks();


    @Query("SELECT AVG(l.responseTimeMs) FROM SearchQueryLog l")
    Double averageResponseTime();
}