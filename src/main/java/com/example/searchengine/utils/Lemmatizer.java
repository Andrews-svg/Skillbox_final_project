package com.example.searchengine.utils;

import java.util.List;
import java.util.Map;

public interface Lemmatizer {


    String normalizeText(String rawText);
    List<String> getBasicFormsFromString(String input);
    Map<String, Integer> lemmasFrequencyMapFromString(String input);
    Map<String, Integer> countLemmas(List<String> lemmas);
}