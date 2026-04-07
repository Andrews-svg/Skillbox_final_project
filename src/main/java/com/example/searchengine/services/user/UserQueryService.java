package com.example.searchengine.services.user;


import com.example.searchengine.dto.UserSecurityInfo;
import com.example.searchengine.models.AppUser;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

public interface UserQueryService {

    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findById(Long id);
    AppUser findByEmail(String email);
    AppUser findByResetToken(String token);
    Optional<AppUser> findByActivationToken(String token);
    boolean existsByUsername(String username);
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
    boolean canUserAuthenticate(String username);
    UserSecurityInfo getUserSecurityInfo(String username);
    boolean existsByEmail(String email);
}