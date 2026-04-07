package com.example.searchengine.utils;

import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Lemmatizer {

    private static final Logger logger = LoggerFactory.getLogger(Lemmatizer.class);
    private static final List<String> SERVICE_PARTS = List.of(
            "СОЮЗ", "ПРЕДЛ", "МЕЖД", "ЧАСТ"
    );

    private final RussianLuceneMorphology russianMorph;

    public Lemmatizer(RussianLuceneMorphology russianMorph) {
        this.russianMorph = russianMorph;
        logger.info("Lemmatizer initialized successfully");
    }


    private String cleanText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.toLowerCase()
                .replaceAll("[^а-яё\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }


    private String getNormalForm(String word) {
        try {
            List<String> normalForms = russianMorph.getNormalForms(word);
            return normalForms.isEmpty() ? null : normalForms.get(0);
        } catch (Exception e) {
            return null;
        }
    }


    private boolean isServiceWord(String word) {
        try {
            List<String> morphInfo = russianMorph.getMorphInfo(word);
            for (String info : morphInfo) {
                for (String part : SERVICE_PARTS) {
                    if (info.contains(part)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            return true;
        }
        return false;
    }


    public Map<String, Integer> getLemmasFrequency(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new HashMap<>();
        }
        String cleaned = cleanText(text);
        String[] words = cleaned.split("\\s+");
        Map<String, Integer> frequency = new HashMap<>();
        for (String word : words) {
            if (word.isEmpty() || word.length() < 2) continue;
            if (isServiceWord(word)) continue;
            String lemma = getNormalForm(word);
            if (lemma != null) {
                frequency.put(lemma, frequency.getOrDefault(lemma, 0) + 1);
            }
        }
        logger.debug("Extracted {} unique lemmas from text", frequency.size());
        return frequency;
    }


    public Set<String> getUniqueLemmas(String text) {
        return getLemmasFrequency(text).keySet();
    }


    public Set<String> getLemmasFromQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new HashSet<>();
        }
        String cleaned = cleanText(query);
        String[] words = cleaned.split("\\s+");
        Set<String> lemmas = new HashSet<>();
        for (String word : words) {
            if (word.length() < 2) continue;
            if (isServiceWord(word)) continue;
            String lemma = getNormalForm(word);
            if (lemma != null) {
                lemmas.add(lemma);
            }
        }
        return lemmas;
    }
}