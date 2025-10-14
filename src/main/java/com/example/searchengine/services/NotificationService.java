package com.example.searchengine.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final List<String> notifications = new ArrayList<>();


    public void sendAdminNotification(String message) {
        notifications.add(message);
        logger.info("Sent admin notification: {}", message);
    }

    private void sendNotification(String message) {
        this.sendAdminNotification(message);
        logger.info("Sent notification about mass marking sites as failed.");
    }

    public void addNotification(String message) {
        notifications.add(message);
    }


    public List<String> getNotifications() {
        return new ArrayList<>(notifications);
    }

    public void clearNotifications() {
        notifications.clear();
    }
}