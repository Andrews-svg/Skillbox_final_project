package com.example.searchengine.services.jwt;

import com.example.searchengine.dto.JwtDto.TokenValidationResult;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;

@Service
public class JwtTokenValidator {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenValidator.class);

    private final JwtTokenProvider tokenProvider;
    private final JwtTokenExtractor tokenExtractor;

    public JwtTokenValidator(JwtTokenProvider tokenProvider,
                             JwtTokenExtractor tokenExtractor) {
        this.tokenProvider = tokenProvider;
        this.tokenExtractor = tokenExtractor;
    }


    public boolean validateToken(String token, UserDetails userDetails) {
        if (!tokenExtractor.isValidTokenStructure(token)) {
            logger.debug("Token structure validation failed for access token");
            return false;
        }
        try {
            final String username = tokenExtractor.extractUsername(token);
            final String tokenType = tokenExtractor.extractTokenType(token);
            if (username == null || tokenType == null) {
                logger.debug("Failed to extract username or token type from access token");
                return false;
            }
            if (!JwtConstants.TOKEN_TYPE_ACCESS.equals(tokenType)) {
                logger.warn("Attempt to use non-access token as access token. Type: {}", tokenType);
                return false;
            }
            boolean isNotExpired = !isTokenExpired(token);
            boolean usernameMatches = username.equals(userDetails.getUsername());
            if (!usernameMatches) {
                logger.debug("Username mismatch in access token. Expected: {}, Actual: {}",
                        userDetails.getUsername(), username);
            }
            if (!isNotExpired) {
                logger.debug("Access token is expired");
            }
            return usernameMatches && isNotExpired;
        } catch (Exception e) {
            logger.debug("Access token validation failed with exception: {}", e.getMessage());
            return false;
        }
    }


    public boolean validateRefreshToken(String token, UserDetails userDetails) {
        if (!tokenExtractor.isValidTokenStructure(token)) {
            logger.debug("Token structure validation failed for refresh token");
            return false;
        }
        try {
            final String username = tokenExtractor.extractUsername(token);
            final String tokenType = tokenExtractor.extractTokenType(token);
            if (username == null || tokenType == null) {
                logger.debug("Failed to extract username or token type from refresh token");
                return false;
            }
            if (!JwtConstants.TOKEN_TYPE_REFRESH.equals(tokenType)) {
                logger.warn("Attempt to use non-refresh token as refresh token. Type: {}", tokenType);
                return false;
            }
            boolean isNotExpired = !isTokenExpired(token);
            boolean usernameMatches = username.equals(userDetails.getUsername());

            if (!usernameMatches) {
                logger.debug("Username mismatch in refresh token. Expected: {}, Actual: {}",
                        userDetails.getUsername(), username);
            }
            if (!isNotExpired) {
                logger.debug("Refresh token is expired");
            }
            return usernameMatches && isNotExpired;
        } catch (Exception e) {
            logger.debug("Refresh token validation failed with exception: {}", e.getMessage());
            return false;
        }
    }


    public TokenValidationResult validateTokenWithDetails(String token, UserDetails userDetails) {
        if (!tokenExtractor.isValidTokenStructure(token)) {
            logger.debug("Token structure validation failed for detailed validation");
            return TokenValidationResult.invalid("Invalid token structure");
        }
        try {
            Claims claims = tokenExtractor.extractAllClaims(token);
            String tokenType = claims.get(JwtConstants.CLAIM_TYPE, String.class);
            if (!JwtConstants.TOKEN_TYPE_ACCESS.equals(tokenType)) {
                logger.debug("Invalid token type in detailed validation. Expected: {}, Actual: {}",
                        JwtConstants.TOKEN_TYPE_ACCESS, tokenType);
                return TokenValidationResult.invalid(
                        String.format("Invalid token type. Expected: %s, Actual: %s",
                                JwtConstants.TOKEN_TYPE_ACCESS, tokenType)
                );
            }
            if (!tokenProvider.getIssuer().equals(claims.getIssuer())) {
                logger.debug("Issuer mismatch. Expected: {}, Actual: {}",
                        tokenProvider.getIssuer(), claims.getIssuer());
                return TokenValidationResult.invalid("Issuer mismatch");
            }
            Set<String> audienceSet = claims.getAudience();
            if (audienceSet == null || audienceSet.isEmpty() ||
                    !tokenProvider.getAudience().equals(audienceSet.iterator().next())) {
                logger.debug("Audience mismatch. Expected: {}, Actual: {}",
                        tokenProvider.getAudience(), audienceSet);
                return TokenValidationResult.invalid("Audience mismatch");
            }
            if (isTokenExpired(token)) {
                logger.debug("Token is expired in detailed validation");
                return TokenValidationResult.expired();
            }
            String username = claims.getSubject();
            if (!username.equals(userDetails.getUsername())) {
                logger.debug("User mismatch in detailed validation. Expected: {}, Actual: {}",
                        userDetails.getUsername(), username);
                return TokenValidationResult.invalid("User mismatch");
            }
            logger.debug("Token validation successful for user: {}", username);
            return TokenValidationResult.valid(claims);
        } catch (ExpiredJwtException e) {
            logger.debug("Token expired exception in detailed validation: {}", e.getMessage());
            return TokenValidationResult.expired(e.getClaims());
        } catch (Exception e) {
            logger.debug("Detailed token validation failed with exception: {}", e.getMessage());
            return TokenValidationResult.invalid(e.getMessage());
        }
    }


    public boolean isTokenExpired(String token) {
        try {
            Date expiration = tokenExtractor.extractExpiration(token);
            if (expiration == null) {
                logger.debug("Cannot extract expiration date from token");
                return true;
            }
            boolean expired = expiration.before(new Date());
            if (expired) {
                logger.debug("Token expired at: {}", expiration);
            }
            return expired;
        } catch (Exception e) {
            logger.debug("Failed to check token expiration: {}", e.getMessage());
            return true;
        }
    }


    public boolean isTokenAboutToExpire(String token, long thresholdMs) {
        long remainingTime = getRemainingTimeMs(token);
        boolean aboutToExpire = remainingTime > 0 && remainingTime < thresholdMs;
        if (aboutToExpire) {
            logger.debug("Token is about to expire. Remaining time: {}ms, Threshold: {}ms",
                    remainingTime, thresholdMs);
        }
        return aboutToExpire;
    }


    public long getRemainingTimeMs(String token) {
        try {
            Date expiration = tokenExtractor.extractExpiration(token);
            if (expiration == null) {
                logger.debug("Cannot calculate remaining time - expiration date is null");
                return 0;
            }
            Date now = new Date();
            long remainingTime = Math.max(0, expiration.getTime() - now.getTime());
            logger.debug("Token remaining time: {}ms", remainingTime);
            return remainingTime;
        } catch (Exception e) {
            logger.debug("Failed to calculate remaining time: {}", e.getMessage());
            return 0;
        }
    }
}