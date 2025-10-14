package com.example.searchengine.repository;

import com.example.searchengine.models.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.searchengine.models.Site;
import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    List<Site> findByUrlContaining(String url);

    boolean existsByUrl(String url);

    Optional<Site> findByUrl(String url);

    List<Long> findByGroupId(Long groupId);

    Site findByDomain(String domain);

    List<Site> findByStatus(Status status);
}