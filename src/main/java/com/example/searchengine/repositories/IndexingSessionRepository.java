package com.example.searchengine.repositories;

import com.example.searchengine.models.IndexingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndexingSessionRepository extends JpaRepository<IndexingSession, Long> {
}