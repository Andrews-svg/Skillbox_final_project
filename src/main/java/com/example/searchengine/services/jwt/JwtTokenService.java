package com.example.searchengine.services.jwt;


import com.example.searchengine.dto.JwtDto.TokenPair;
import com.example.searchengine.dto.JwtDto.TokenValidationResult;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class JwtTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    private final JwtTokenProvider tokenProvider;
    private final JwtTokenValidator tokenValidator;
    private final JwtTokenExtractor tokenExtractor;

    public JwtTokenService(JwtTokenProvider tokenProvider,
                           JwtTokenValidator tokenValidator,
                           JwtTokenExtractor tokenExtractor) {
        this.tokenProvider = tokenProvider;
        this.tokenValidator = tokenValidator;
        this.tokenExtractor = tokenExtractor;
    }

    public String generateAccessToken(UserDetails userDetails, Long userId, String email) {
        validateInput(userDetails, userId, email);
        return generateToken(userDetails, userId, email,
                JwtConstants.TOKEN_TYPE_ACCESS,
                tokenProvider.getAccessTokenExpirationMs());
    }

    public String generateRefreshToken(UserDetails userDetails, Long userId, String email) {
        validateInput(userDetails, userId, email);
        return generateToken(userDetails, userId, email,
                JwtConstants.TOKEN_TYPE_REFRESH,
                tokenProvider.getRefreshTokenExpirationMs());
    }

    public TokenPair generateTokenPair(UserDetails userDetails, Long userId, String email) {
        validateInput(userDetails, userId, email);

        String accessToken = generateAccessToken(userDetails, userId, email);
        String refreshToken = generateRefreshToken(userDetails, userId, email);

        return new TokenPair(
                accessToken,
                refreshToken,
                tokenProvider.getAccessTokenExpirationMs() / 1000
        );
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        return tokenValidator.validateToken(token, userDetails);
    }

    public boolean validateRefreshToken(String token, UserDetails userDetails) {
        return tokenValidator.validateRefreshToken(token, userDetails);
    }

    public TokenValidationResult validateTokenWithDetails(String token, UserDetails userDetails) {
        return tokenValidator.validateTokenWithDetails(token, userDetails);
    }

    public boolean isTokenExpired(String token) {
        return tokenValidator.isTokenExpired(token);
    }

    public boolean isTokenAboutToExpire(String token, long thresholdMs) {
        return tokenValidator.isTokenAboutToExpire(token, thresholdMs);
    }

    public String extractUsername(String token) {
        return tokenExtractor.extractUsername(token);
    }

    public Long extractUserId(String token) {
        return tokenExtractor.extractUserId(token);
    }

    public String extractEmail(String token) {
        return tokenExtractor.extractEmail(token);
    }

    public List<String> extractRoles(String token) {
        return tokenExtractor.extractRoles(token);
    }

    public String extractTokenType(String token) {
        return tokenExtractor.extractTokenType(token);
    }

    public boolean isAccessToken(String token) {
        return tokenExtractor.isAccessToken(token);
    }

    public boolean isRefreshToken(String token) {
        return tokenExtractor.isRefreshToken(token);
    }

    public boolean hasRole(String token, String role) {
        return tokenExtractor.hasRole(token, role);
    }

    public boolean hasAnyRole(String token, String... roles) {
        return tokenExtractor.hasAnyRole(token, roles);
    }

    public Date extractExpiration(String token) {
        return tokenExtractor.extractExpiration(token);
    }

    public Date extractIssuedAt(String token) {
        return tokenExtractor.extractIssuedAt(token);
    }

    public String extractTokenFromRequest(HttpServletRequest request) {
        return tokenExtractor.extractTokenFromRequest(request);
    }

    public String extractTokenFromHeader(String authorizationHeader) {
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    public String extractTokenFromCookies(Cookie[] cookies) {
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (tokenProvider.getAuthCookieName().equals(cookie.getName()) &&
                    StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public Map<String, Object> getTokenInfo(String token) {
        return tokenExtractor.getTokenInfo(token);
    }

    public boolean isValidTokenStructure(String token) {
        return tokenExtractor.isValidTokenStructure(token);
    }

    public long getRemainingTimeMs(String token) {
        return tokenValidator.getRemainingTimeMs(token);
    }

    public long getRemainingTimeSeconds(String token) {
        return getRemainingTimeMs(token) / 1000;
    }

    public java.util.function.Predicate<String> hasRolePredicate(String token) {
        return role -> hasRole(token, role);
    }


    public Cookie createTokenCookie(String token) {
        return tokenProvider.createTokenCookie(token);
    }


    public Cookie createLogoutCookie() {
        return tokenProvider.createLogoutCookie();
    }


    public String getSameSiteHeaderValue() {
        return tokenProvider.getSameSiteHeaderValue();
    }


    public boolean shouldRefreshToken(String token) {
        if (!isValidTokenStructure(token)) return false;
        if (isTokenExpired(token)) return false;

        long remainingMs = getRemainingTimeMs(token);
        long threshold = tokenProvider.getAccessTokenExpirationMs() / 4;

        return remainingMs < threshold;
    }


    public Map<String, Object> getUserInfo(String token) {
        Map<String, Object> info = new HashMap<>();

        String username = extractUsername(token);
        Long userId = extractUserId(token);
        String email = extractEmail(token);
        List<String> roles = extractRoles(token);

        if (username != null) info.put("username", username);
        if (userId != null) info.put("userId", userId);
        if (email != null) info.put("email", email);
        info.put("roles", roles != null ? roles : Collections.emptyList());

        return info;
    }


    private String generateToken(UserDetails userDetails, Long userId, String email,
                                 String tokenType, long expirationMs) {

        Instant now = Instant.now();
        Instant expiration = now.plus(expirationMs, ChronoUnit.MILLIS);

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtConstants.CLAIM_TYPE, tokenType);
        claims.put(JwtConstants.CLAIM_USER_ID, userId);
        claims.put(JwtConstants.CLAIM_EMAIL, email);
        claims.put(JwtConstants.CLAIM_ROLES, extractRoles(userDetails));

        return Jwts.builder()
                .header().type("JWT").and()
                .issuer(tokenProvider.getIssuer())
                .audience().add(tokenProvider.getAudience()).and()
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claims(claims)
                .signWith(tokenProvider.getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    private List<String> extractRoles(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    private void validateInput(UserDetails userDetails, Long userId, String email) {
        Objects.requireNonNull(userDetails, "userDetails cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(email, "email cannot be null");

        if (!StringUtils.hasText(userDetails.getUsername())) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
    }
}