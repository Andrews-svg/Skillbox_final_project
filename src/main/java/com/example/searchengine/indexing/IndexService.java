package com.example.searchengine.indexing;

import com.example.searchengine.models.Index;
import com.example.searchengine.models.Lemma;
import com.example.searchengine.models.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
    @Transactional
    public interface IndexService {

        int saveIndex(Index index);
        void update(Index index);
        boolean checkIfIndexExists(Integer indexId);
        boolean checkIfIndexExists(Integer pageId, Integer lemmaId);
        Boolean checkIfIndexExists(Page page, Lemma lemma);
        Optional<Index> findIndex(Integer id);
        List<Index> findAllIndexes();
        List<Integer> findAllAvailablePageIds();
        void delete(Index index);
        void deleteAll();
        void deleteByPageId(Integer pageId);
        int deleteAllIndexes();
        Integer count();
    }




