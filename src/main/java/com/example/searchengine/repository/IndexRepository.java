package com.example.searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.searchengine.models.Index;

import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<Index, Long> {

    Optional<Index> findByPageIdAndLemmaId(
            @Param("pageId") long pageId,
            @Param("lemmaId") long lemmaId
    );

    boolean existsByPageIdAndLemmaId(
            @Param("pageId") long pageId,
            @Param("lemmaId") long lemmaId
    );

    void deleteByPageId(long pageId);
    void deleteByLemmaId(long lemmaId);
    long findIdByPageId(long pageId);
    long findIdByLemmaId(long lemmaId);
}