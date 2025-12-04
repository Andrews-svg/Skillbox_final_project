package com.example.searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.searchengine.models.Page;
import com.example.searchengine.config.Site;

import java.util.List;
import java.util.Optional; 

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    boolean existsByPath(String path);
    Optional<Page> findByPath(String path);
    List<Page> findAllBySite(Site site);
    void deleteBySite(Site site);
    void deleteById(Long id);
    List<Page> findAllBySiteIn(List<Site> batch);
}

