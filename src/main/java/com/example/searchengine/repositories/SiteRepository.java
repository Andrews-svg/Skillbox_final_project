package com.example.searchengine.repositories;

import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    Optional<Site> findByUrl(String url);

    boolean existsByUrl(String url);

    List<Site> findByStatus(Status status);

    boolean existsByStatus(Status status);

    long countByStatus(Status status);
}
