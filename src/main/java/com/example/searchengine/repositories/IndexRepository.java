package com.example.searchengine.repositories;

import com.example.searchengine.models.Index;
import com.example.searchengine.models.Lemma;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<Index, Long> {

    List<Index> findByPage(Page page);

    List<Index> findByLemmaAndPage_Site(Lemma lemma, Site site);

    List<Index> findByLemmaInAndPage_Site(List<Lemma> lemmas, Site site);

    Optional<Index> findByPageAndLemma(Page page, Lemma lemma);

    void deleteByPage(Page page);

    @Modifying
    @Query("DELETE FROM Index i WHERE i.page.site = :site")
    void deleteBySite(@Param("site") Site site);

    long countByPageSite(Site site);
}
