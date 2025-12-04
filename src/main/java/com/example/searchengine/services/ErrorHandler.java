package com.example.searchengine.services;

import com.example.searchengine.models.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;

@Service
public class ErrorHandler {

    private final DatabaseService databaseService;

    @Autowired
    public ErrorHandler(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public void handleError(long id, Throwable t) {
        StringBuilder errorMsg = new StringBuilder("Ошибка индексации сайта ");
        if (t instanceof HttpStatusCodeException hse) {
            errorMsg.append(hse.getStatusCode().value())
                    .append(": ")
                    .append(hse.getResponseBodyAsString());
        } else if (t instanceof IOException ioex) {
            errorMsg.append(ioex.getLocalizedMessage());
        } else {
            errorMsg.append(t.getLocalizedMessage());
        }
        databaseService.updateSiteStatus(id, Status.FAILED, errorMsg.toString());
    }
}