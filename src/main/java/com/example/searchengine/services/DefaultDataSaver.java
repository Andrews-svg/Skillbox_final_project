package com.example.searchengine.services;

import com.example.searchengine.exceptions.InvalidSiteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Set;

@Service
public class DefaultDataSaver implements DataSaver {

    private final DatabaseService databaseService;

    @Autowired
    public DefaultDataSaver(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public void saveData(String url, String title, Set<String> outLinksSet) throws
            IOException, InvalidSiteException, SiteService.InvalidSiteException {
        databaseService.saveData(url, title, outLinksSet);
    }
}