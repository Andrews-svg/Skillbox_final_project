package com.example.searchengine.services.jwt;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;

@Service
public class JwtTokenExtractor {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenExtractor.class);

    private final JwtTokenProvider tokenProvider;

    private final JwtParser jwtParser;

    public JwtTokenExtractor(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
        this.jwtParser = Jwts.parser()
                .verifyWith(tokenProvider.getSecretKey())
                .requireIssuer(tokenProvider.getIssuer())
                .requireAudience(tokenProvider.getAudience())
                .build();
    }


    public String extractUsername(String token) {
        if (!isValidTokenStructure(token)) return null;
        return extractClaim(token, Claims::getSubject);
    }


    public Long extractUserId(String token) {
        if (!isValidTokenStructure(token)) return null;
        return extractClaim(token, claims ->
                claims.get(JwtConstants.CLAIM_USER_ID, Long.class));
    }


    public String extractEmail(String token) {
        if (!isValidTokenStructure(token)) return null;
        return extractClaim(token, claims ->
                claims.get(JwtConstants.CLAIM_EMAIL, String.class));
    }


    public List<String> extractRoles(String token) {
        if (!isValidTokenStructure(token)) return Collections.emptyList();
        try {
            Claims claims = extractAllClaims(token);
            Object rolesObj = claims.get(JwtConstants.CLAIM_ROLES);

            if (rolesObj instanceof List<?> list) {
                return list.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .toList();
            }
            return Collections.emptyList();
        } catch (Exception e) {
            logger.debug("Failed to extract roles from token: {}", e.getMessage());
            return Collections.emptyList();
        }
    }


    public String extractTokenType(String token) {
        if (!isValidTokenStructure(token)) return null;
        return extractClaim(token, claims ->
                claims.get(JwtConstants.CLAIM_TYPE, String.class));
    }


    public Date extractExpiration(String token) {
        if (!isValidTokenStructure(token)) return null;
        return extractClaim(token, Claims::getExpiration);
    }


    public Date extractIssuedAt(String token) {
        if (!isValidTokenStructure(token)) return null;
        return extractClaim(token, Claims::getIssuedAt);
    }


    public boolean isAccessToken(String token) {
        return JwtConstants.TOKEN_TYPE_ACCESS.equals(extractTokenType(token));
    }


    public boolean isRefreshToken(String token) {
        return JwtConstants.TOKEN_TYPE_REFRESH.equals(extractTokenType(token));
    }


    public boolean hasRole(String token, String role) {
        List<String> roles = extractRoles(token);
        return roles.contains(role);
    }


    public boolean hasAnyRole(String token, String... roles) {
        List<String> tokenRoles = extractRoles(token);
        return Arrays.stream(roles).anyMatch(tokenRoles::contains);
    }


    public String extractTokenFromRequest(HttpServletRequest request) {
        String requestPath = request.getServletPath();
        logger.debug("Извлечение токена из запроса к: {}", requestPath);
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            logger.debug("Найден токен в заголовке Authorization");
            return token;
        }
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            logger.debug("Найден токен в параметре запроса");
            return tokenParam;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Set<String> possibleCookieNames = new HashSet<>(Arrays.asList(
                    tokenProvider.getAuthCookieName(),
                    "jwt_token",
                    "access_token",
                    "token"
            ));

            logger.debug("Проверяем {} cookies на наличие токена", cookies.length);

            for (Cookie cookie : cookies) {
                String cookieName = cookie.getName();
                String cookieValue = cookie.getValue();
                if (StringUtils.hasText(cookieValue) &&
                        possibleCookieNames.contains(cookieName)) {
                    logger.debug("Найден токен в cookie: {} (длина: {} символов)",
                            cookieName, cookieValue.length());
                    return cookieValue;
                }
            }
            List<String> foundCookieNames = Arrays.stream(cookies)
                    .map(Cookie::getName)
                    .collect(java.util.stream.Collectors.toList());
            logger.debug("Найдены cookies: {}", foundCookieNames);
        } else {
            logger.debug("Cookies не найдены в запросе");
        }
        logger.debug("Токен не найден в запросе");
        return null;
    }

    public Map<String, Object> getTokenInfo(String token) {
        Map<String, Object> info = new HashMap<>();

        try {
            if (isValidTokenStructure(token)) {
                Claims claims = extractAllClaims(token);
                info.put("subject", claims.getSubject());
                info.put("issuer", claims.getIssuer());
                info.put("audience", claims.getAudience());
                info.put("issuedAt", claims.getIssuedAt());
                info.put("expiration", claims.getExpiration());
                info.put("type", claims.get(JwtConstants.CLAIM_TYPE, String.class));
                info.put("userId", claims.get(JwtConstants.CLAIM_USER_ID, Long.class));
                info.put("email", claims.get(JwtConstants.CLAIM_EMAIL, String.class));
                info.put("valid", true);
            } else {
                info.put("valid", false);
                info.put("error", "Invalid token structure");
            }
        } catch (Exception e) {
            info.put("valid", false);
            info.put("error", e.getMessage());
        }

        return info;
    }

    public boolean isValidTokenStructure(String token) {
        if (!StringUtils.hasText(token)) {
            logger.debug("Пустой токен");
            return false;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            logger.debug("Токен должен содержать 3 части, найдено: {}", parts.length);
            return false;
        }
        try {
            boolean headerValid = isValidBase64Url(parts[0]);
            boolean payloadValid = isValidBase64Url(parts[1]);
            if (!headerValid || !payloadValid) {
                logger.debug("Невалидный Base64Url: header={}, payload={}", headerValid, payloadValid);
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.debug("Валидация структуры токена не удалась: {}", e.getMessage());
            return false;
        }
    }


    private boolean isValidBase64Url(String str) {
        if (!StringUtils.hasText(str)) {
            return false;
        }
        String base64UrlPattern = "^[A-Za-z0-9_-]+$";
        if (!str.matches(base64UrlPattern)) {
            return false;
        }
        int length = str.length();
        if (length % 4 != 0) {
            int padding = 4 - (length % 4);
            str = str + "=".repeat(padding);
        }
        try {
            Decoders.BASE64URL.decode(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            logger.debug("Не удалось извлечь claim из токена: {}", e.getMessage());
            return null;
        }
    }


    public Claims extractAllClaims(String token) {
        try {
            return jwtParser.parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            logger.debug("Не удалось извлечь claims из токена: {}", e.getMessage());
            throw new RuntimeException("Failed to parse JWT token", e);
        }
    }
}