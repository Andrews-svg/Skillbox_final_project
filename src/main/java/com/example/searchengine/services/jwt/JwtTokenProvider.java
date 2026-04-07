package com.example.searchengine.services.jwt;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Optional;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret:}")
    private String secretKeyBase64;

    @Value("${jwt.issuer:" + JwtConstants.DEFAULT_ISSUER + "}")
    private String issuer;

    @Value("${jwt.audience:" + JwtConstants.DEFAULT_AUDIENCE + "}")
    private String audience;

    @Value("${jwt.access-token.expiration:" + JwtConstants.DEFAULT_ACCESS_TOKEN_EXPIRATION_MS + "}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token.expiration:" + JwtConstants.DEFAULT_REFRESH_TOKEN_EXPIRATION_MS + "}")
    private long refreshTokenExpirationMs;


    @Value("${jwt.cookie.name:" + JwtConstants.DEFAULT_COOKIE_NAME + "}")
    private String authCookieName;

    @Value("${jwt.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${jwt.cookie.http-only:true}")
    private boolean cookieHttpOnly;

    @Value("${jwt.cookie.same-site:strict}")
    private String cookieSameSite;

    @Value("${jwt.cookie.max-age:#{null}}")
    private Integer cookieMaxAge;

    @Value("${jwt.cookie.domain:#{null}}")
    private String cookieDomain;

    @Value("${jwt.cookie.path:/}")
    private String cookiePath;


    @Value("${jwt.allowed-clock-skew-seconds:30}")
    private int allowedClockSkewSeconds;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        validateConfiguration();
        initializeSecretKey();
        logConfiguration();
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(secretKeyBase64)) {
            throw new IllegalStateException(
                    "JWT secret key is not configured. Set 'jwt.secret' property or JWT_SECRET_* environment variable."
            );
        }

        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKeyBase64);
            if (keyBytes.length < 32) {
                logger.warn("JWT secret key is only {} bytes. Minimum recommended: 32 bytes", keyBytes.length);
            }
        } catch (Exception e) {
            throw new IllegalStateException("JWT secret key is not valid Base64", e);
        }

        if (accessTokenExpirationMs < 300000) {
            logger.warn("Access token expiration time is very short: {} ms", accessTokenExpirationMs);
        }

        if (refreshTokenExpirationMs < accessTokenExpirationMs) {
            logger.warn("Refresh token expiration ({}) is shorter than access token expiration ({})",
                    refreshTokenExpirationMs, accessTokenExpirationMs);
        }
    }

    private void initializeSecretKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKeyBase64);
            this.secretKey = Keys.hmacShaKeyFor(keyBytes);
            logger.debug("JWT secret key initialized ({} bytes)", keyBytes.length);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decode JWT secret key", e);
        }
    }


    private void logConfiguration() {
        if (logger.isInfoEnabled()) {
            logger.info("JWT Configuration:");
            logger.info("  Issuer: {}", issuer);
            logger.info("  Audience: {}", audience);
            logger.info("  Access Token Expiration: {} ms ({} minutes)",
                    accessTokenExpirationMs, accessTokenExpirationMs / 60000);
            logger.info("  Refresh Token Expiration: {} ms ({} days)",
                    refreshTokenExpirationMs, refreshTokenExpirationMs / 86400000);
            logger.info("  Cookie Name: {}", authCookieName);
            logger.info("  Cookie Secure: {}", cookieSecure);
            logger.info("  Cookie HttpOnly: {}", cookieHttpOnly);
            logger.info("  Cookie SameSite: {}", cookieSameSite);
            logger.info("  Clock Skew Allowed: {} seconds", allowedClockSkewSeconds);
        }
    }



    public SecretKey getSecretKey() {
        return secretKey;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getAudience() {
        return audience;
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public String getAuthCookieName() {
        return authCookieName;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public boolean isCookieHttpOnly() {
        return cookieHttpOnly;
    }

    public String getCookieSameSite() {
        return cookieSameSite;
    }

    public Optional<Integer> getCookieMaxAge() {
        return Optional.ofNullable(cookieMaxAge);
    }

    public Optional<String> getCookieDomain() {
        return Optional.ofNullable(cookieDomain);
    }

    public String getCookiePath() {
        return cookiePath;
    }

    public int getAllowedClockSkewSeconds() {
        return allowedClockSkewSeconds;
    }

    public String getSecretKeyBase64() {
        return secretKeyBase64;
    }


    public jakarta.servlet.http.Cookie createTokenCookie(String token) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(authCookieName, token);
        cookie.setHttpOnly(cookieHttpOnly);
        cookie.setSecure(cookieSecure);
        cookie.setPath(cookiePath);

        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            cookie.setDomain(cookieDomain);
        }

        if (cookieMaxAge != null) {
            cookie.setMaxAge(cookieMaxAge);
        }

        return cookie;
    }


    public jakarta.servlet.http.Cookie createLogoutCookie() {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(authCookieName, "");
        cookie.setHttpOnly(cookieHttpOnly);
        cookie.setSecure(cookieSecure);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(0);

        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            cookie.setDomain(cookieDomain);
        }

        return cookie;
    }


    public String getSameSiteHeaderValue() {
        return "SameSite=" + cookieSameSite;
    }
}