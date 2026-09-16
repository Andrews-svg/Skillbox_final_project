package com.example.searchengine.services.indexing;

import com.example.searchengine.models.Index;
import com.example.searchengine.models.Lemma;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;
import com.example.searchengine.repositories.IndexRepository;
import com.example.searchengine.repositories.LemmaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;


@Service
public class PageIndexingService {

    private static final Logger logger = LoggerFactory.getLogger(PageIndexingService.class);

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public PageIndexingService(LemmaRepository lemmaRepository,
                               IndexRepository indexRepository) {
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }


    @Transactional
    public int savePageLemmas(Page page, Site site, Map<String, Integer> lemmas) {
        int count = 0;
        for (Map.Entry<String, Integer> entry : new TreeMap<>(lemmas).entrySet()) {
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
        logger.debug("✅ Индексация страницы {}: сохранено {} лемм в одной транзакции",
                page.getPath(), count);
        return count;
    }
}
