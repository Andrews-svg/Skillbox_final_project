package com.example.searchengine.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class RussianLuceneMorphology extends LuceneMorphology {

    private static final Logger logger = LoggerFactory.getLogger(RussianLuceneMorphology.class);


    private final Lemmatizer internalAnalyzer;


    private final Map<String, String[]> exceptions = new HashMap<>();


    private static final List<String> IMPORTANT_POS_TAGS = Arrays.asList("СУЩ", "ГЛАГ", "ПРИЧ", "ДЕЕПР", "ПРИЛ");


    private final Map<String, Integer> difficultWords = new ConcurrentHashMap<>();

    public RussianLuceneMorphology(Lemmatizer analyzer) throws IOException {
        this.internalAnalyzer = analyzer;
        initializeExceptions();
    }


    public static RussianLuceneMorphology createInstance(Lemmatizer analyzer) throws IOException {
        return new RussianLuceneMorphology(analyzer);
    }

    @Override
    public List<String> getMorphInfo(String word) {
        if (exceptions.containsKey(word)) {
            return Arrays.asList(exceptions.get(word));
        }
        List<String> result = super.getMorphInfo(word);

        if (result.isEmpty() || result.get(0).startsWith("UNKNOWN")) {
            recordDifficultWord(word);
        }
        return result;
    }


    private void recordDifficultWord(String word) {
        difficultWords.merge(word, 1, Integer::sum);
    }


    public Map<String, Integer> getDifficultWords() {
        return new HashMap<>(difficultWords);
    }


    private void initializeExceptions() {
        exceptions.put("Москва", new String[]{"Москва", "СУЩ", "ЖЕН", "ЕДИН"});
        exceptions.put("Санкт-Петербург", new String[]{"Санкт-Петербург", "СУЩ", "МУЖ", "ЕДИН"});

    }


    public boolean isImportantPartOfSpeech(String posTag) {
        return IMPORTANT_POS_TAGS.contains(posTag);
    }


    public String detailedAnalysis(String word) {
        List<String> morphInfo = getMorphInfo(word);
        if (morphInfo.isEmpty()) {
            return "Не удалось проанализировать слово.";
        }
        return String.format("Слово '%s': Начальная форма='%s', " +
                "Часть речи='%s'", word, morphInfo.get(0), morphInfo.get(1));
    }


    public String normalize(String word) {
        List<String> morphInfo = getMorphInfo(word);
        return morphInfo.isEmpty() ? word : morphInfo.get(0);
    }
}