package com.example.searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.searchengine.indexing.IndexingHistoryRecord;

@Repository
public interface IndexingHistoryRecordRepository extends JpaRepository<IndexingHistoryRecord, Long> {

}