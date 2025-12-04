package com.example.searchengine.utils;

import com.example.searchengine.indexing.IndexingServiceImpl;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppInitializer implements ApplicationRunner {
    private final IndexingServiceImpl indexingServiceImpl;

    public AppInitializer(IndexingServiceImpl indexingServiceImpl) {
        this.indexingServiceImpl = indexingServiceImpl;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        indexingServiceImpl.init();
    }
}