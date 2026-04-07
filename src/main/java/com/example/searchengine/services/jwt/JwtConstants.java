package com.example.searchengine.services.jwt;

public final class JwtConstants {

    private JwtConstants() {
        throw new UnsupportedOperationException("Utility class");
    }


    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";


    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_USER_ID = "user_id";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ISSUED_AT = "iat";
    public static final String CLAIM_EXPIRATION = "exp";


    public static final long DEFAULT_ACCESS_TOKEN_EXPIRATION_MS = 3600000L;
    public static final long DEFAULT_REFRESH_TOKEN_EXPIRATION_MS = 604800000L;
    public static final String DEFAULT_ISSUER = "search-engine";
    public static final String DEFAULT_AUDIENCE = "search-engine-client";
    public static final String DEFAULT_COOKIE_NAME = "auth_token";
    public static final String DEFAULT_COOKIE_SAME_SITE = "strict";
}