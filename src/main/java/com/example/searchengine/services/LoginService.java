package com.example.searchengine.services;

import com.example.searchengine.models.LoginForm;
import org.springframework.security.core.Authentication;

public interface LoginService {
    Authentication authenticate(LoginForm loginForm);
}