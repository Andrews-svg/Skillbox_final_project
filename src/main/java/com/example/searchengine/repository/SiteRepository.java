package com.example.searchengine.repository;

import com.example.searchengine.models.Page;
import com.example.searchengine.models.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.searchengine.config.Site;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {

    List<Site> findByUrlContaining(String url);

    boolean existsByUrl(String url);

    Optional<Site> findByUrl(String url);

    Optional<Site> findByDomain(String domain);

    List<Site> findByStatus(Status status);

    void updateSiteStatusTime(int siteId, LocalDateTime now);

    void savePage(Page page);

    void deletePagesForSite(int siteId);
}