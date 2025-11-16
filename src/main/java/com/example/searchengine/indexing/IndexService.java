package com.example.searchengine.indexing;

import com.example.searchengine.models.Index;
import com.example.searchengine.models.Lemma;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.SearchResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


@Service
    @Transactional
    public interface IndexService {

    void indexPage(String url) throws Exception;

        int saveIndex(Index index);

        boolean checkIfIndexExists(Integer pageId, Integer lemmaId);

        Optional<Index> findIndex(Integer id);

        List<Index> findByLemmaId(Integer lemmaId);

        Index findById(Integer indexId);

        List<Index> findAllIndexes();

        String getUrlFromIndex(Index index);

        void deleteByPageId(Integer pageId, UUID sessionId);

        int deleteAllIndexes();

        Set<String> getParsedLinks(String url) throws Exception;

        List<SearchResult> findPagesForQuery(String query);
        Integer save(Index index);
        long saveOrUpdateIndex(Index index);
        Boolean checkIfIndexExists(Page page, Lemma lemma);
        Index findByIdPair(Page page, Lemma lemma);
        void update(Index index);
        List<Index> findAll();
        List<Index> findAll(int limit);

        void delete(Index index);
        void deleteAll();
        void deleteByPage(Page page);

        Integer count();

    }




