package com.example.searchengine.repositories;

import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
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

    @Modifying
    @Query("UPDATE Site s SET s.status = :status, s.statusTime = :statusTime WHERE s.id = :id")
    int updateStatusById(@Param("id") Long id,
                         @Param("status") Status status,
                         @Param("statusTime") LocalDateTime statusTime);


    @Modifying
    @Query("UPDATE Site s SET s.status = :status, s.statusTime = :statusTime, s.lastError = :error WHERE s.id = :id")
    int updateStatusWithErrorById(@Param("id") Long id,
                                  @Param("status") Status status,
                                  @Param("statusTime") LocalDateTime statusTime,
                                  @Param("error") String error);


    @Modifying
    @Query("UPDATE Site s SET s.statusTime = :statusTime WHERE s.id = :id")
    int updateStatusTimeById(@Param("id") Long id,
                             @Param("statusTime") LocalDateTime statusTime);
}
