package com.example.searchengine.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;

@Component
public class Lemmatizer {

    private static final Logger logger = LoggerFactory.getLogger(Lemmatizer.class);
    private static final Set<String> SERVICE_PARTS = Set.of(
            "СОЮЗ", "ПРЕДЛ", "МЕЖД", "ЧАСТ", "ЧАСТИЦА"
    );

    public Lemmatizer() {
        logger.info("Lemmatizer initialized with AOT library (version 2025.11.25)");
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
            var meanings = lookupForMeanings(word);
            if (meanings != null && !meanings.isEmpty()) {
                return meanings.get(0).toString();
            }
        } catch (Exception e) {
            logger.debug("Could not get lemma for word: {}", word);
        }
        return null;
    }

    private boolean isServiceWord(String word) {
        try {
            var meanings = lookupForMeanings(word);
            if (meanings != null && !meanings.isEmpty()) {
                for (var meaning : meanings) {
                    String morphInfo = meaning.getMorphology().toString();
                    for (String part : SERVICE_PARTS) {
                        if (morphInfo.contains(part)) {
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