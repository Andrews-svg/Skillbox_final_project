package com.example.searchengine.services;

import com.example.searchengine.models.AppUser;

public interface EmailService {
    void sendActivationEmail(AppUser user);

    void sendPasswordResetEmail(AppUser user);
}