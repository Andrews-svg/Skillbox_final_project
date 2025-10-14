package com.example.searchengine.services;

import com.example.searchengine.models.AppUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.searchengine.models.AppUser;
import com.example.searchengine.repository.UserRepository;

import java.util.Optional;

@Service
public class AppUserDetailsService implements
        org.springframework.security.core.userdetails.UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<AppUser> appUserOptional = userRepository.findByUsername(username);
        return appUserOptional.map(AppUserPrincipal::new).orElseThrow(() ->
                new UsernameNotFoundException("Пользователь не найден"));
    }


    public String encodePassword(String plainPassword) {
        return encoder.encode(plainPassword);
    }


    public boolean checkPasswordMatch(String plainPassword, String hashedPassword) {
        return encoder.matches(plainPassword, hashedPassword);
    }


    public void registerNewUser(AppUser appUser) {
        appUser.setPassword(encodePassword(appUser.getPassword()));
        appUser.setEnabled(true);
        userRepository.save(appUser);
    }

    public void updateUser(AppUser appUser) {
        AppUser existingUser = userRepository.findById(appUser.getId()).orElse(null);
        if (existingUser != null &&
                appUser.getPassword() != null &&
                !appUser.getPassword().trim().isEmpty() &&
                !checkPasswordMatch(existingUser.getPassword(), appUser.getPassword())) {
            appUser.setPassword(encodePassword(appUser.getPassword()));
        }
        userRepository.save(appUser);
    }
}