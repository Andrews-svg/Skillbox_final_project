package com.example.searchengine.services;


import com.example.searchengine.models.AccountStatus;
import com.example.searchengine.models.AppUser;
import com.example.searchengine.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("customUserDetailsService")
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LOGGER.info("Загрузка пользователя '{}' для Spring Security", username);
        return userRepository.findByUsername(username)
                .map(this::convertToUserDetails)
                .orElseThrow(() -> {
                    LOGGER.error("Пользователь '{}' не найден", username);
                    return new UsernameNotFoundException("Пользователь не найден: " + username);
                });
    }


    private UserDetails convertToUserDetails(AppUser appUser) {
        if (!appUser.getStatus().equals(AccountStatus.CONFIRMED)) {
            LOGGER.warn("Аккаунт пользователя {} не активирован", appUser.getUsername());
            throw new DisabledException("Аккаунт не активирован");
        }
        List<SimpleGrantedAuthority> authorities = appUser.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                .collect(Collectors.toList());
        return new org.springframework.security.core.userdetails.User(
                appUser.getUsername(),
                appUser.getPassword(),
                appUser.isEnabled(),
                appUser.isAccountNonExpired(),
                appUser.isCredentialsNonExpired(),
                appUser.isAccountNonLocked(),
                authorities
        );
    }
}