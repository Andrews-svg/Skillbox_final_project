package com.example.searchengine.dto.JwtDto;

import io.jsonwebtoken.Claims;

public class TokenValidationResult {
    private final boolean valid;
    private final boolean expired;
    private final String error;
    private final Claims claims;

    private TokenValidationResult(boolean valid, boolean expired,
                                  String error, Claims claims) {
        this.valid = valid;
        this.expired = expired;
        this.error = error;
        this.claims = claims;
    }

    public static TokenValidationResult valid(Claims claims) {
        return new TokenValidationResult(true, false, null, claims);
    }

    public static TokenValidationResult invalid(String error) {
        return new TokenValidationResult(false, false, error, null);
    }

    public static TokenValidationResult expired() {
        return new TokenValidationResult(false, true, "Token expired", null);
    }

    public static TokenValidationResult expired(Claims claims) {
        return new TokenValidationResult(false, true, "Token expired", claims);
    }


    public boolean isValid() { return valid; }
    public boolean isExpired() { return expired; }
    public String getError() { return error; }
    public Claims getClaims() { return claims; }
}
