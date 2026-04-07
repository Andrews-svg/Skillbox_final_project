package com.example.searchengine.utils;

import java.util.UUID;

public class TokenUtils {

    public static String generateActivationToken() {
        return UUID.randomUUID().toString();
    }

    public static String generateResetToken() {
        return UUID.randomUUID().toString();
    }
}