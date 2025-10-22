package com.example.searchengine.indexing;

import com.example.searchengine.exceptions.InvalidSiteException;
import com.example.searchengine.models.Index;
import com.example.searchengine.models.SearchResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;




    @Service
    @Transactional
    public interface IndexService {

        void indexPage(String url) throws IOException,
                InvalidSiteException, InterruptedException;

        long saveIndex(Index index, UUID sessionId);

        boolean checkIfIndexExists(Long pageId, Long lemmaId);

        Optional<Index> findIndex(Long id);

        List<Index> findByLemmaId(Long lemmaId);

        Index findById(Long indexId);

        List<Index> findAllIndexes();

        String getUrlFromIndex(Index index);

        List<String> getNotifications();

        void clearNotifications();

        void deleteByPageId(Long pageId, UUID sessionId);

        int deleteAllIndexes();

        Set<String> getParsedLinks(String url) throws IOException,
                InvalidSiteException, InterruptedException;

        List<SearchResult> findPagesForQuery(String query);
    }




