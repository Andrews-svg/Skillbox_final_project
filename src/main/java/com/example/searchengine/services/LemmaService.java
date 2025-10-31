package com.example.searchengine.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.searchengine.dao.LemmaDao;
import com.example.searchengine.models.Lemma;
import com.example.searchengine.config.Site;
import com.example.searchengine.repository.LemmaRepository;
import com.example.searchengine.repository.SiteRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LemmaService {

    private static final Logger logger =
            LoggerFactory.getLogger(LemmaService.class);

    private final LemmaDao lemmaDao;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;

    @Autowired
    public LemmaService(LemmaDao lemmaDao,
                        LemmaRepository lemmaRepository,
                        SiteRepository siteRepository) {
        this.lemmaDao = lemmaDao;
        this.lemmaRepository = lemmaRepository;
        this.siteRepository = siteRepository;
    }

    public void saveOrUpdateLemma(Lemma lemma) {
        logger.info("Сохранение или обновление леммы: {}", lemma);
        validateLemma(lemma);
        lemmaDao.saveOrUpdateLemma(lemma);
    }

    public void updateLemma(Lemma lemma) {
        logger.info("Обновление леммы: {}", lemma);
        validateLemma(lemma);
        lemmaDao.update(lemma);
    }

    public Integer getCountBySiteUrl(String url) {
        logger.info("Получение количества лемм для сайта с URL: {}", url);

        return siteRepository.findByUrl(url)
                .map(Site::getId)
                .map(lemmaDao::countLemmasOnSite)
                .orElse(0);
    }

    public Integer countLemmasOnSite(Integer siteID) {
        logger.info("Подсчет лемм на сайте с ID: {}", siteID);
        return lemmaDao.countLemmasOnSite(siteID);
    }

    public boolean isLemmaTooFrequent(Lemma lemma) {
        logger.info("Проверка частоты леммы: {}", lemma);
        return lemmaDao.isLemmaTooFrequent(lemma);
    }

    public Long getTotalLemmas() {
        logger.info("Получение общего количества лемм");

        long totalCount = lemmaRepository.count();

        if (totalCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Количество лемм превышает допустимый размер для типа int.");
        }
        return totalCount;
    }

    private void validateLemma(Lemma lemma) {
        validateNotNull(lemma);
        validateNotEmpty(lemma);
        validateLength(lemma);
        validateCharacters(lemma);
    }

    private void validateNotNull(Lemma lemma) {
        if (lemma == null) {
            logger.error("Ошибка валидации: лемма не может быть null");
            throw new IllegalArgumentException("Лемма не может быть null");
        }
    }

    private void validateNotEmpty(Lemma lemma) {
        if (lemma.getLemma() == null || lemma.getLemma().isEmpty()) {
            logger.error("Ошибка валидации: лемма не может быть пустой");
            throw new IllegalArgumentException("Лемма не может быть пустой");
        }
    }

    private void validateLength(Lemma lemma) {
        long length = lemma.getLemma().length();
        if (length < 3 || length > 100) {
            logger.error(
                    "Ошибка валидации: длина леммы должна быть от 3 до 100 символов");
            throw new IllegalArgumentException(
                    "Длина леммы должна быть от 3 до 100 символов");
        }
    }

    private void validateCharacters(Lemma lemma) {
        if (!lemma.getLemma().matches("^[a-zA-Zа-яА-ЯёЁ0-9\\s]+$")) {
            logger.error("Ошибка валидации: лемма содержит недопустимые символы");
            throw new IllegalArgumentException("Лемма содержит недопустимые символы");
        }
    }


    public List<String> findByLemmaAndSiteId(String lemma, Integer siteId) {
        List<Object[]> results = lemmaRepository.findByLemmaAndSiteId(lemma, siteId);
        ObjectMapper objectMapper = new ObjectMapper();
        return results.stream()
                .map(obj -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("lemma", (String) obj[0]);
                    map.put("frequency", String.valueOf(obj[1]));
                    try {
                        return objectMapper.writeValueAsString(map);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<Integer, Integer> countLemmasGroupedBySite(List<Site> sites) {
        Set<Integer> siteIds = sites.stream()
                .map(Site::getId)
                .collect(Collectors.toSet());
        return lemmaDao.countLemmasGroupedBySite(siteIds);
    }
}
