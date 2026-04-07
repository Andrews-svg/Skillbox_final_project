package com.example.searchengine.repositories;

import com.example.searchengine.models.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<AppUser> findByActivationToken(String token);

    List<AppUser> findByUsernameContainingAndEmailContaining(String usernamePart, String emailPart);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByResetToken(String token);
}

