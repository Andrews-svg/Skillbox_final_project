package com.example.searchengine.exceptions;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String username) {
        super("Пользователь с логином '" + username + "' уже существует.");
    }
}
