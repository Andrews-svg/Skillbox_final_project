package com.example.searchengine.indexing;

import com.example.searchengine.models.Index;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
    @Transactional
    public interface IndexService {

        long saveIndex(Index index);
        void update(Index index);
        boolean checkIfIndexExists(long indexId);
        boolean checkIfIndexExists(long pageId, long lemmaId);
        Optional<Index> findIndex(long id);
        List<Index> findAllIndexes();
        List<Long> findAllAvailablePageIds();
        void delete(Index index);
        void deleteAll();
        void deleteByPageId(long pageId);
        long deleteAllIndexes();
        long count();
    }




