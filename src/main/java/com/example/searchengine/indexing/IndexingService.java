package com.example.searchengine.indexing;

import com.example.searchengine.config.Site;

public interface IndexingService {

     void init();
     boolean canStartIndexing(long id);
     String getSiteUrlForId(long id);
     boolean isIndexingInProgress();
     void setIndexingStopped();
     Site generateNewSite(String url);
     String getStatus(long id);
     void logInfo(String message, Object... args);

}