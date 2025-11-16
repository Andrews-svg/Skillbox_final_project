package com.example.searchengine.services;

import com.example.searchengine.config.Site;
import com.example.searchengine.models.Lemma;
import com.example.searchengine.repository.LemmaRepository;
import com.example.searchengine.utils.ContentProcessor;
import com.example.searchengine.utils.Lemmatizer;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LemmaService {

    private static final Logger logger = LoggerFactory.getLogger(LemmaService.class);

    private final LemmaRepository lemmaRepository;
    private final ContentProcessor contentProcessor;
    private final Lemmatizer lemmatizer;


    @Transactional
    public void saveOrUpdateLemma(Lemma lemma) {
        logger.debug("Начало процесса сохранения или обновления леммы: {}", lemma);
        Optional<Lemma> existingLemma = lemmaRepository.findByLemmaAndSiteId(lemma.getLemma(),
                lemma.getSite().getId());
        if (existingLemma.isPresent()) {
            Lemma foundLemma = existingLemma.get();
            foundLemma.setFrequency(foundLemma.getFrequency() + 1);
            lemmaRepository.save(foundLemma);
            logger.info("Лемма успешно обновлена: {}", foundLemma);
        } else {
            lemmaRepository.save(lemma);
            logger.info("Новая лемма успешно создана: {}", lemma);
        }
    }


    @Cacheable(value = "lemmas", key = "#id")
    public Optional<Lemma> findById(Integer id) {
        logger.debug("Ищу лемму по идентификатору: {}", id);
        Optional<Lemma> lemma = lemmaRepository.findById(id);
        logger.info("Результат поиска по идентификатору {}: {}",
                id, lemma.isPresent() ? "найдено" : "не найдено");
        return lemma;
    }


    @Cacheable(value = "lemmas", key = "'all'")
    public List<Lemma> findAll() {
        logger.debug("Запрашиваю полный список всех лемм");
        List<Lemma> lemmas = lemmaRepository.findAll();
        logger.info("Всего получено лемм: {}", lemmas.size());
        return lemmas;
    }


    @CacheEvict(value = "lemmas", key = "#lemma.id")
    public void delete(Lemma lemma) {
        logger.debug("Удаляю лемму: {}", lemma);
        lemmaRepository.delete(lemma);
        logger.info("Лемма успешно удалена: {}", lemma);
    }


    @CacheEvict(value = "lemmas", allEntries = true)
    public void deleteAll() {
        logger.debug("Начинаю массовое удаление всех лемм");
        lemmaRepository.deleteAll();
        logger.info("Все леммы успешно удалены");
    }


    public void processContent(String content, Map<String,
            Float> mapTitle, Map<String, Float> mapBody) {
        logger.debug("Обрабатываю контент страницы: {}",
                content.substring(0, Math.min(content.length(), 100)));
        contentProcessor.processContent(content, mapTitle, mapBody);
        logger.info("Контент успешно обработан");
    }


    public Map<String, Float> delegateCombineMaps(Map<String, Float> mapTitle,
                                          Map<String, Float> mapBody) {
        logger.debug("Объединяю карты частот лемм");
        Map<String, Float> combinedMap = contentProcessor.combineMaps(mapTitle, mapBody);
        logger.info("Карты объединены успешно");
        return combinedMap;
    }


    public void generateLemmas(String content, Map<String, Float> mapTitle,
                               Map<String, Float> mapBody) {
        Document doc = Jsoup.parse(content);
        Elements titleElements = doc.select("title");
        Elements bodyElements = doc.select("body");
        if (!titleElements.isEmpty()) {
            String titleText = Objects.requireNonNull(titleElements.first()).text();
            Map<String, Integer> titleFreqMap = lemmatizer.lemmasFrequencyMapFromString(titleText);
            titleFreqMap.forEach((key, value) -> mapTitle.put(key, value.floatValue()));
        }
        if (!bodyElements.isEmpty()) {
            String bodyText = Objects.requireNonNull(bodyElements.first()).text();
            Map<String, Integer> bodyFreqMap = lemmatizer.lemmasFrequencyMapFromString(bodyText);
            bodyFreqMap.forEach((key, value) -> mapBody.put(key, value.floatValue() * 0.8f));
        }
    }


    public double computeRelevance(String content, String query) {
        logger.debug("Рассчитываю релевантность страницы: {}",
                content.substring(0, Math.min(content.length(), 100)));
        double relevance = contentProcessor.computeRelevance(content, query);
        logger.info("Релевантность рассчитана: {}", relevance);
        return relevance;
    }

    public Map<Integer, Integer> countLemmasGroupedBySite(List<Site> currentBatch) {
        Map<Integer, Integer> result = new HashMap<>();
        for (Site site : currentBatch) {
            List<Lemma> lemmas = lemmaRepository.findDistinctBySite(site);
            int uniqueLemmasCount = lemmas.size();
            result.put(site.getId(), uniqueLemmasCount);
        }
        return result;
    }

    public Integer countLemmas() {
        return Math.toIntExact(lemmaRepository.count());
    }

    public Object getTotalLemmas() {
        return lemmaRepository.count();
    }

    public Optional<Lemma> findByBaseFormAndSiteId(String form, Integer id) {
        return lemmaRepository.findByBaseFormAndSiteId(form, id);
    }

    public List<Lemma> findLemmaByName(String form) {
        return lemmaRepository.findAllByLemma(form);
    }
}
