package com.example.searchengine.repositories;

import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    Optional<Page> findByPathAndSite(String path, Site site);

    List<Page> findBySite(Site site);

    boolean existsByPathAndSite(String path, Site site);

    void deleteBySite(Site site);

    long countBySite(Site site);
}