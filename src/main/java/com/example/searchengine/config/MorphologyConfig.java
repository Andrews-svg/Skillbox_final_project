package com.example.searchengine.config;

import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class MorphologyConfig {

    @Bean
    public RussianLuceneMorphology russianMorphology() {
        try {
            return new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create Russian morphology", e);
        }
    }
}