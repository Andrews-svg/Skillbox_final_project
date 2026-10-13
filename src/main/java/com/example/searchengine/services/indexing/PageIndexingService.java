package com.example.searchengine.services.indexing;

import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


@Service
public class PageIndexingService {

    private static final Logger logger = LoggerFactory.getLogger(PageIndexingService.class);
    private static final int BATCH_SIZE = 100;

    private final PageLemmasBatchService pageLemmasBatchService;

    public PageIndexingService(PageLemmasBatchService pageLemmasBatchService) {
        this.pageLemmasBatchService = pageLemmasBatchService;
    }

    public int savePageLemmas(Page page, Site site, Map<String, Integer> lemmas) {
        if (lemmas == null || lemmas.isEmpty()) {
            return 0;
        }

        TreeMap<String, Integer> sorted = new TreeMap<>(lemmas);
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(sorted.entrySet());

        int totalCount = 0;
        int totalBatches = (entries.size() + BATCH_SIZE - 1) / BATCH_SIZE;

        logger.debug("🚀 savePageLemmas: {} лемм, {} батчей по {}",
                entries.size(), totalBatches, BATCH_SIZE);

        for (int i = 0; i < entries.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, entries.size());
            List<Map.Entry<String, Integer>> batch = entries.subList(i, end);
            int batchCount = pageLemmasBatchService.saveBatch(page, site, batch);
            totalCount += batchCount;
        }

        logger.debug("✅ savePageLemmas: сохранено {} лемм в {} батчах",
                totalCount, totalBatches);
        return totalCount;
    }
}