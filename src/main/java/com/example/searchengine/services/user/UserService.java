package com.example.searchengine.services.user;

import com.example.searchengine.models.AppUser;

import java.util.List;

public interface UserService {


    void registerNewUser(AppUser newUser);

    AppUser updateProfile(AppUser updatedUser);

    void update(AppUser updatedUser);

    void activateAccount(String activationToken);

    void save(AppUser user);

    boolean isPasswordAlreadyEncoded(String password);

    void assignDefaultRole(AppUser user);

    void updateUserRoles(Long userId, List<String> roleNames);

}