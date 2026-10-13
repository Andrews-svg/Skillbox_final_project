package com.example.searchengine.services.indexing;

import com.example.searchengine.models.Index;
import com.example.searchengine.models.Lemma;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;
import com.example.searchengine.repositories.IndexRepository;
import com.example.searchengine.repositories.LemmaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PageLemmasBatchService {

    private static final Logger logger = LoggerFactory.getLogger(PageLemmasBatchService.class);

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public PageLemmasBatchService(LemmaRepository lemmaRepository,
                                  IndexRepository indexRepository) {
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Retryable(
            retryFor = {PessimisticLockingFailureException.class, CannotAcquireLockException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2)
    )
    @Transactional
    public int saveBatch(Page page, Site site, List<Map.Entry<String, Integer>> batch) {
        int count = 0;
        for (Map.Entry<String, Integer> entry : batch) {
            String lemmaText = entry.getKey();
            int rank = entry.getValue();

            lemmaRepository.upsert(lemmaText, site.getId());
            Lemma lemma = lemmaRepository.findByLemmaAndSite(lemmaText, site)
                    .orElseThrow(() -> new IllegalStateException("Лемма не найдена: " + lemmaText));

            Optional<Index> existing = indexRepository.findByPageAndLemma(page, lemma);
            if (existing.isPresent()) {
                Index idx = existing.get();
                idx.setRank(idx.getRank() + rank);
                indexRepository.save(idx);
            } else {
                indexRepository.save(new Index(page, lemma, rank));
            }
            count++;
        }
        logger.debug("✅ Батч сохранён: {} лемм", count);
        return count;
    }
}
