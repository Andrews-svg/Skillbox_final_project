package com.example.searchengine.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class Lemmatizer {
    private static final Logger logger = LoggerFactory.getLogger(Lemmatizer.class);
    private static final List<String> AUXILIARY_PARTS = List.of("ПРЕДЛ", "МСП", "СОЮЗ", "МЕЖД", "ЧАСТ");
    private final LuceneMorphology morphology;

    @Autowired
    public Lemmatizer(LuceneMorphology luceneMorphology) {
        this.morphology = luceneMorphology;
        logger.info("Lemmatizer initialized successfully.");
    }

    public String normalizeText(String rawText) {
        logger.debug("Normalizing text: {}", rawText);
        String normalized = rawText.replaceAll("[^а-яА-ЯёЁ\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
        logger.debug("Normalized text: {}", normalized);
        return normalized;
    }

    public List<String> getBasicFormsFromString(String input) {
        logger.info("Extracting basic forms from input: {}", input);
        String cleanedInput = normalizeText(input);
        List<String> basicForms = new ArrayList<>();
        String[] words = cleanedInput.split(" ");

        for (String word : words) {
            List<String> morphInfo = morphology.getMorphInfo(word);
            if (!morphInfo.isEmpty()) {
                String[] parts = morphInfo.get(0).split("\\|");
                if (!AUXILIARY_PARTS.contains(parts[1]) && word.length() >= 3) {
                    basicForms.add(parts[0]);
                }
            }
        }
        logger.info("Basic forms extracted: {}", basicForms);
        return basicForms;
    }

    public Map<String, Integer> lemmasFrequencyMapFromString(String input) {
        logger.info("Creating frequency map of lemmas from input: {}", input);
        List<String> lemmas = getBasicFormsFromString(input);
        Map<String, Integer> frequencies = new HashMap<>();
        for (String lemma : lemmas) {
            frequencies.merge(lemma, 1, Integer::sum);
        }
        logger.info("Frequency map created: {}", frequencies);
        return frequencies;
    }

    public Map<String, Integer> countLemmas(List<String> lemmas) {
        logger.debug("Counting lemmas...");
        Map<String, Integer> frequencies = new HashMap<>();
        for (String lemma : lemmas) {
            frequencies.merge(lemma, 1, Integer::sum);
        }
        logger.debug("Counted lemmas: {}", frequencies);
        return frequencies;
    }
}