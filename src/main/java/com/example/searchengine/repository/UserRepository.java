package com.example.searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.searchengine.models.AppUser;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
}