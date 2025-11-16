package com.example.searchengine.indexing;

import com.example.searchengine.config.Site;

public interface IndexingService {

     void init();
     boolean canStartIndexing(Integer id);
     String getSiteUrlForId(Integer id);
     boolean isIndexingInProgress();
     void setIndexingStopped();
     Site generateNewSite(String url);
     String getStatus(Integer id);
     void logInfo(String message, Object... args);

}