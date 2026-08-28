package com.example.searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class Lemmatizer {

    private static final Logger logger = LoggerFactory.getLogger(Lemmatizer.class);
    private static final Set<String> SERVICE_PARTS = Set.of(
            "СОЮЗ", "ПРЕДЛ", "МЕЖД", "ЧАСТ", "ЧАСТИЦА"
    );

    private final LuceneMorphology morphology;

    public Lemmatizer() {
        try {
            this.morphology = new RussianLuceneMorphology();
            logger.info("Lemmatizer initialized with RussianLuceneMorphology (Lucene 9.3 compatible)");
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize RussianLuceneMorphology", e);
        }
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
            List<String> normalForms = morphology.getNormalForms(word);
            if (normalForms != null && !normalForms.isEmpty()) {
                return normalForms.get(0);
            }
        } catch (Exception e) {
            logger.debug("Could not get lemma for word: {}", word, e);
        }
        return null;
    }

    private boolean isServiceWord(String word) {
        try {
            List<String> morphInfos = morphology.getMorphInfo(word);
            if (morphInfos != null) {
                for (String info : morphInfos) {
                    for (String part : SERVICE_PARTS) {
                        if (info.contains(part)) {
                            return true;
                        }
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
        if (cleaned.isEmpty()) {
            return new HashMap<>();
        }
        String[] words = cleaned.split("\\s+");
        Map<String, Integer> frequency = new HashMap<>();
        for (String word : words) {
            if (word.length() < 2) continue;
            if (isServiceWord(word)) continue;
            String lemma = getNormalForm(word);
            if (lemma != null && !lemma.isEmpty()) {
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
        if (cleaned.isEmpty()) {
            return new HashSet<>();
        }
        String[] words = cleaned.split("\\s+");
        Set<String> lemmas = new HashSet<>();
        for (String word : words) {
            if (word.length() < 2) continue;
            if (isServiceWord(word)) continue;
            String lemma = getNormalForm(word);
            if (lemma != null && !lemma.isEmpty()) {
                lemmas.add(lemma);
            }
        }
        return lemmas;
    }
}
