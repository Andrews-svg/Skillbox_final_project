package com.example.searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.searchengine.models.Index;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {

    Optional<Index> findByPageIdAndLemmaId(
            @Param("pageId") Integer pageId,
            @Param("lemmaId") Integer lemmaId
    );

    boolean existsByPageIdAndLemmaId(
            @Param("pageId") Integer pageId,
            @Param("lemmaId") Integer lemmaId
    );

    void deleteByPageId(Integer pageId);
    void deleteByLemmaId(Integer lemmaId);
    Integer findIdByPageId(Integer pageId);
    Integer findIdByLemmaId(Integer lemmaId);
}