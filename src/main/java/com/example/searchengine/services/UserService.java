package com.example.searchengine.services;

import com.example.searchengine.models.AppUser;

import java.util.Optional;

public interface UserService {

    AppUser registerNewUser(AppUser newUser);

    Optional<AppUser> findByUsername(String username);

    AppUser updateProfile(AppUser updatedUser);
}