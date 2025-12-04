package com.example.searchengine.indexing;

import com.example.searchengine.models.Index;
import com.example.searchengine.repository.IndexRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@EnableAsync
@Transactional
public class IndexServiceImpl implements IndexService {

    private final IndexRepository indexRepository;

    private static final Logger log = LoggerFactory.getLogger(IndexServiceImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    public IndexServiceImpl(IndexRepository indexRepository) {
        this.indexRepository = indexRepository;
    }


    @Override
    public long saveIndex(Index index) {
        if (index == null) {
            log.error("Невозможно сохранить пустой индекс");
            return -1;
        }
        Index savedIndex = indexRepository.save(index);
        return savedIndex.getId();
    }


    @Override
    public void update(Index index) {
        if (index == null) {
            log.error("Невозможно обновить пустой индекс");
            return;
        }
        indexRepository.save(index);
    }


    @Override
    public boolean checkIfIndexExists(long indexId) {
        return indexRepository.existsById(indexId);
    }


    @Override
    public boolean checkIfIndexExists(long pageId, long lemmaId) {
        log.info("Проверка существования индекса для страницы ID: {}, леммы ID: {}", pageId, lemmaId);
        return indexRepository.existsByPageIdAndLemmaId(pageId, lemmaId);
    }


    @Override
    public Optional<Index> findIndex(long id) {
        return indexRepository.findById(id);
    }


    public List<Index> findAllIndexes() {
        return indexRepository.findAll();
    }


    @Override
    public List<Long> findAllAvailablePageIds() {
        return indexRepository.findAll()
                .stream()
                .map(index -> index.getPage().getId())
                .distinct()
                .collect(Collectors.toList());
    }


    @Override
    public void delete(Index index) {
        if (index == null) {
            log.error("Невозможно удалить пустой индекс");
            return;
        }
        indexRepository.delete(index);
    }


    @Override
    public void deleteAll() {
        indexRepository.deleteAll();
    }


    public void deleteByPageId(long pageId) {
        indexRepository.deleteByPageId(pageId);
    }


    public long deleteAllIndexes() {
        long countBeforeDeletion = (long) indexRepository.count();
        indexRepository.deleteAll();
        return countBeforeDeletion;
    }


    @Override
    public long count() {
        return Math.toIntExact(indexRepository.count());
    }


    protected <T> T executeWithLogging(Supplier<T> action) {
        try {
            return action.get();
        } catch (Exception e) {
            log.error("{}: {}", "Failed to check if index exists", e.getMessage());
            throw new RuntimeException("Failed to check if index exists", e);
        }
    }
}