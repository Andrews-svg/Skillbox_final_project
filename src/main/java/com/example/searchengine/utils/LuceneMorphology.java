package com.example.searchengine.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class LuceneMorphology {
    private static final Logger logger = LoggerFactory.getLogger(LuceneMorphology.class);

    protected List<String> getMorphInfoInternal(String word) {
        logger.debug("Getting morphological info for word: {}", word);
        List<String> morphInfo = new ArrayList<>();

        switch (word) {
            case "кот":
                morphInfo.add("кот|NOUN|SINGULAR|MASCULINE");
                break;
            case "коты":
                morphInfo.add("кот|NOUN|PLURAL|MASCULINE");
                break;
            case "бегает":
                morphInfo.add("бегать|VERB|PRES|SINGULAR|3RD|MASCULINE");
                break;
            default:
                morphInfo.add(word + "|UNKNOWN");
                logger.warn("Morphological info for word '{}' is unknown", word);
                break;
        }

        logger.debug("Morphological info for word '{}': {}", word, morphInfo);
        return morphInfo;
    }

    public List<String> getMorphInfo(String word) {
        logger.info("Requesting morphological info for word: {}", word);
        return getMorphInfoInternal(word);
    }
}