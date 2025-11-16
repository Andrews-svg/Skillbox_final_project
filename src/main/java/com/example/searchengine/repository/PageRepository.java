package com.example.searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.searchengine.models.Page;
import com.example.searchengine.config.Site;

import java.util.List;
import java.util.Optional; 

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    boolean existsByPath(String path);

    Optional<Page> findByPath(String path);

    Integer countByUrl(String url);

    Optional<Page> findByUrl(String url);

    List<Page> findAllBySite(Site site);

    boolean existsByUri(String uri);

    @Query("SELECT p.url FROM Page p WHERE p.id = :id")
    Optional<String> findUrlById(@Param("id") Integer id);

    Optional<Page> findByUri(String uri);

    boolean existsByUrl(String url);

    void deleteBySite(Site site);

    void batchInsert(List<Page> pages);
    void deleteByPageId(Integer pageId);
}

