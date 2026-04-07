package com.example.searchengine.services;

import com.example.searchengine.models.Index;
import com.example.searchengine.models.Lemma;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;
import com.example.searchengine.repositories.LemmaRepository;
import com.example.searchengine.services.indexing.IndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class LemmaService {

    private static final Logger logger = LoggerFactory.getLogger(LemmaService.class);

    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private IndexService indexService;


    @Transactional
    public Lemma saveOrIncrement(String lemmaText, Site site) {
        lemmaRepository.upsert(lemmaText, site.getId());
        return lemmaRepository.findByLemmaAndSite(lemmaText, site)
                .orElseThrow(() -> new RuntimeException("Лемма не найдена: " + lemmaText));
    }


    @Transactional
    public void decrementFrequency(Lemma lemma) {
        Lemma freshLemma = lemmaRepository.findById(lemma.getId())
                .orElseThrow(() -> new RuntimeException("Лемма не найдена: " + lemma.getId()));

        if (freshLemma.getFrequency() > 1) {
            freshLemma.setFrequency(freshLemma.getFrequency() - 1);
            lemmaRepository.save(freshLemma);
        } else {
            lemmaRepository.delete(freshLemma);
        }
    }


    @Transactional
    public void decrementAllForPage(Page page) {
        List<Index> indexes = indexService.findByPage(page);
        for (Index index : indexes) {
            decrementFrequency(index.getLemma());
        }
    }


    @Transactional(readOnly = true)
    public Optional<Lemma> findByLemmaAndSite(String lemmaText, Site site) {
        return lemmaRepository.findByLemmaAndSite(lemmaText, site);
    }


    @Transactional(readOnly = true)
    public List<Lemma> findAllByLemmaInAndSite(Set<String> lemmas, Site site) {
        return lemmaRepository.findAllByLemmaInAndSite(lemmas, site);
    }


    @Transactional(readOnly = true)
    public List<Lemma> findAllBySite(Site site) {
        return lemmaRepository.findBySite(site);
    }


    @Transactional
    public void deleteAllBySite(Site site) {
        lemmaRepository.deleteBySite(site);
    }


    @Transactional(readOnly = true)
    public long countBySite(Site site) {
        return lemmaRepository.countBySite(site);
    }


    @Transactional(readOnly = true)
    public long getTotalLemmas() {
        return lemmaRepository.count();
    }
}