package com.example.searchengine.services.indexing;

import com.example.searchengine.models.Index;
import com.example.searchengine.models.Lemma;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;
import com.example.searchengine.repositories.IndexRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class IndexService {

    private static final Logger logger = LoggerFactory.getLogger(IndexService.class);
    private final IndexRepository indexRepository;

    public IndexService(IndexRepository indexRepository) {
        this.indexRepository = indexRepository;
    }

    public void save(Page page, Lemma lemma, float rank) {
        Optional<Index> existing = indexRepository.findByPageAndLemma(page, lemma);
        if (existing.isPresent()) {
            Index index = existing.get();
            index.setRank(index.getRank() + rank);
            indexRepository.save(index);
            logger.debug("Обновлен существующий индекс для страницы {} и леммы {}",
                    page.getId(), lemma.getId());
        } else {
            Index index = new Index(page, lemma, rank);
            indexRepository.save(index);
            logger.debug("Создан новый индекс для страницы {} и леммы {}",
                    page.getId(), lemma.getId());
        }
    }


    public void saveAll(List<Index> indices) {
        indexRepository.saveAll(indices);
        logger.debug("Сохранено {} записей индекса", indices.size());
    }

    @Transactional(readOnly = true)
    public List<Index> findByPage(Page page) {
        return indexRepository.findByPage(page);
    }

    @Transactional(readOnly = true)
    public List<Index> findByLemmaAndSite(Lemma lemma, Site site) {
        return indexRepository.findByLemmaAndPage_Site(lemma, site);
    }

    @Transactional(readOnly = true)
    public List<Index> findByLemmasAndSite(List<Lemma> lemmas, Site site) {
        return indexRepository.findByLemmaInAndPage_Site(lemmas, site);
    }

    public void deleteByPage(Page page) {
        indexRepository.deleteByPage(page);
        logger.debug("Удалены индексы для страницы id={}", page.getId());
    }

    public void deleteAllBySite(Site site) {
        indexRepository.deleteBySite(site);
        logger.debug("Удалены все индексы для сайта {}", site.getUrl());
    }

    @Transactional(readOnly = true)
    public long countBySite(Site site) {
        return indexRepository.countByPageSite(site);
    }

    @Transactional(readOnly = true)
    public long getTotalIndexEntries() {
        return indexRepository.count();
    }

    @Transactional(readOnly = true)
    public Optional<Index> findByPageAndLemma(Page page, Lemma lemma) {
        return indexRepository.findByPageAndLemma(page, lemma);
    }
}
