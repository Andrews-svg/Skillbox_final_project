package com.example.searchengine.dto.JwtDto;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class TokenPair {

    private static final String TOKEN_TYPE = "Bearer";

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private Long expiresIn;

    @JsonProperty("expires_at")
    private Instant expiresAt;

    public TokenPair() {

    }

    public TokenPair(String accessToken, String refreshToken, Long expiresIn) {
        validateParameters(accessToken, refreshToken, expiresIn);

        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.expiresAt = Instant.now().plusSeconds(expiresIn);
    }


    public static TokenPair of(String accessToken, String refreshToken, Long expiresIn) {
        return new TokenPair(accessToken, refreshToken, expiresIn);
    }


    private void validateParameters(String accessToken, String refreshToken, Long expiresIn) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalArgumentException("accessToken cannot be null or empty");
        }
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new IllegalArgumentException("refreshToken cannot be null or empty");
        }
        if (expiresIn == null || expiresIn <= 0) {
            throw new IllegalArgumentException("expiresIn must be positive");
        }
    }


    public boolean isAccessTokenExpired() {
        return Instant.now().isAfter(expiresAt);
    }


    public boolean willAccessTokenExpireSoon(long secondsThreshold) {
        Instant now = Instant.now();
        Instant thresholdTime = expiresAt.minusSeconds(secondsThreshold);
        return now.isAfter(thresholdTime) && !isAccessTokenExpired();
    }


    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("accessToken", accessToken);
        map.put("refreshToken", refreshToken);
        map.put("tokenType", TOKEN_TYPE);
        map.put("expiresIn", expiresIn);
        map.put("expiresAt", expiresAt);
        return map;
    }


    public Map<String, Object> toSnakeCaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("access_token", accessToken);
        map.put("refresh_token", refreshToken);
        map.put("token_type", TOKEN_TYPE);
        map.put("expires_in", expiresIn);
        map.put("expires_at", expiresAt);
        return map;
    }


    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        if (expiresIn != null) {
            this.expiresIn = expiresIn;
            this.expiresAt = Instant.now().plusSeconds(expiresIn);
        }
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenPair tokenPair = (TokenPair) o;
        return Objects.equals(accessToken, tokenPair.accessToken) &&
                Objects.equals(refreshToken, tokenPair.refreshToken) &&
                Objects.equals(expiresIn, tokenPair.expiresIn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, refreshToken, expiresIn);
    }

    @Override
    public String toString() {
        return "TokenPair{" +
                "accessToken='[PROTECTED]'" +
                ", refreshToken='[PROTECTED]'" +
                ", expiresIn=" + expiresIn +
                ", expiresAt=" + expiresAt +
                '}';
    }
}