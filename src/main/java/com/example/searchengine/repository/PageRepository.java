package com.example.searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;

import java.util.List;
import java.util.Optional; 

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    boolean existsByPath(String path);

    Optional<Page> findByPath(String path);

    Long countByUrl(String url);

    Optional<Page> findByUrl(String url);

    List<Page> findAllBySite(Site site);

    boolean existsByUri(String uri);

    @Query("SELECT p.url FROM Page p WHERE p.id = :id")
    Optional<String> findUrlById(@Param("id") Long id);

    Optional<Page> findByUri(String uri);

    boolean existsByUrl(String url);
}

