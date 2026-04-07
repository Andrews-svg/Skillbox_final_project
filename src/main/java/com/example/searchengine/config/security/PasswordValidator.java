package com.example.searchengine.config.security;

import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

    public boolean isValid(String password) {

        if (password.length() < 8) {
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