package com.example.searchengine.repository;

import com.example.searchengine.models.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    Optional<Token> findByToken(String token);

    void deleteByToken(String token);

    boolean existsByToken(String token);

    Optional<Token> findByTokenAndExpirationDateAfter(String token, LocalDateTime expirationDate);
}