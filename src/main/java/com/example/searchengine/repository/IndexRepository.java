package com.example.searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.searchengine.models.Index;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {

    Optional<Index> findByPageId(Integer pageId);

    Optional<Index> findByLemmaId(Integer lemmaId);

    default Integer findIdByPageId(Integer pageId) {
        return findByPageId(pageId).map(Index::getId).orElse(null);
    }

    default Integer findIdByLemmaId(Integer lemmaId) {
        return findByLemmaId(lemmaId).map(Index::getId).orElse(null);
    }

    boolean existsByPageIdAndLemmaId(Integer pageId, Integer lemmaId);

    boolean existsIndexByPageId(Integer pageId);

    boolean existsByLemmaId(Integer lemmaId);

    Object getIndexingStatusByPageId(Integer pageId);

    void deleteByPageId(Integer pageId);

    void deleteAll();

    void deleteAllByLastModifiedBefore(LocalDateTime threshold);

    Optional<Index> findByPageIdAndLemmaId(Integer pageId, Integer lemmaId);

    @Query("SELECT p.id FROM Page p WHERE p.available = TRUE")
    List<Integer> findAvailablePageIds();
}
