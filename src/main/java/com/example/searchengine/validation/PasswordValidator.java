package com.example.searchengine.validation;

import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 20;

    public boolean isValid(String password) {

        if (password == null || password.isEmpty()) {
            return false;
        }
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return false;
        }
        if (!password.matches(".*\\d.*")) {
            return false;
        }
        if (!password.matches(".*[a-z].*") || !password.matches(".*[A-Z].*")) {
            return false;
        }
        if (!password.matches(".*[!@#%^&*()_+= ${};':\"|,.<>/?].*")) {
            return false;
        }
        if (password.contains(" ")) {
            return false;
        }
        if (password.contains("123456") || password.contains("abcdef") || password.contains("qwerty")) {
            return false;
        }
        return true;
    }
}

