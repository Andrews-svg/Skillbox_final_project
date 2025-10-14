package com.example.searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.searchengine.models.Index;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<Index, Long> {

    Optional<Index> findByPageId(Long pageId);

    Optional<Index> findByLemmaId(Long lemmaId);

    default Long findIdByPageId(Long pageId) {
        return findByPageId(pageId).map(Index::getId).orElse(null);
    }

    default Long findIdByLemmaId(Long lemmaId) {
        return findByLemmaId(lemmaId).map(Index::getId).orElse(null);
    }

    boolean existsByPageIdAndLemmaId(Long pageId, Long lemmaId);

    boolean existsIndexByPageId(Long pageId);

    boolean existsByLemmaId(Long lemmaId);

    Object getIndexingStatusByPageId(Long pageId);

    void deleteByPageId(Long pageId);

    void deleteAll();

    void deleteAllByLastModifiedBefore(LocalDateTime threshold);

    Optional<Index> findByPageIdAndLemmaId(Long pageId, Long lemmaId);

    @Query("SELECT p.id FROM Page p WHERE p.available = TRUE")
    List<Long> findAvailablePageIds();
}
