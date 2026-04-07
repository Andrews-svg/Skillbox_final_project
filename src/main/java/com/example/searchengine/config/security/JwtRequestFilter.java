package com.example.searchengine.config.security;


import com.example.searchengine.services.jwt.JwtTokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenService jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);


    private static final List<String> EXCLUDE_PATHS = Arrays.asList(

            "/",
            "/layout",
            "/home",


            "/login",
            "/register",
            "/forgot-password",
            "/reset-password",
            "/password/forgot",
            "/password/reset",


            "/tab/",
            "/fragments/",


            "/dashboard",
            "/management",
            "/search",


            "/assets/",
            "/css/",
            "/js/",
            "/images/",
            "/webjars/",
            "/favicon.ico",
            "/static/",

            "/error",
            "/csrf-token",
            "/csp-reports",


            "/api/auth/",
            "/api/public/"
    );


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        logger.debug("JWT фильтр: проверка пути: {}", path);
        for (String excludePath : EXCLUDE_PATHS) {
            if (path.startsWith(excludePath)) {
                logger.debug("JWT фильтр: исключаем путь (начинается с {}): {}", excludePath, path);
                return true;
            }
        }
        if (path.startsWith("/auth/activate/")) {
            return true;
        }
        if (path.startsWith("/api/")) {
            logger.debug("JWT фильтр: API путь требует проверки токена: {}", path);
            return false;
        }
        logger.debug("JWT фильтр: обычный путь, пропускаем без токена: {}", path);
        return true;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        final String requestPath = request.getServletPath();
        logger.debug("JWT фильтр: обработка защищенного API запроса: {}", requestPath);
        String token = extractToken(request);
        if (token == null) {
            logger.warn("JWT фильтр: отсутствует токен для API маршрута {}", requestPath);
            sendUnauthorizedError(response, "Требуется JWT токен для доступа к API");
            return;
        }
        try {
            String username = jwtUtil.extractUsername(token);
            if (username == null) {
                logger.warn("JWT фильтр: не удалось извлечь имя пользователя из токена");
                sendUnauthorizedError(response, "Недействительный JWT токен");
                return;
            }
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.info("JWT фильтр: успешная аутентификация пользователя: {}", username);
                } else {
                    logger.warn("JWT фильтр: недействительный токен для пользователя: {}", username);
                    sendUnauthorizedError(response, "Недействительный JWT токен");
                    return;
                }
            }
        } catch (JwtException ex) {
            logger.error("JWT фильтр: ошибка проверки токена: {}", ex.getMessage());
            sendUnauthorizedError(response, "Ошибка проверки JWT токена: " + ex.getMessage());
            return;
        } catch (Exception ex) {
            logger.error("JWT фильтр: неожиданная ошибка: {}", ex.getMessage(), ex);
            sendInternalError(response);
            return;
        }
        chain.doFilter(request, response);
    }


    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            logger.debug("JWT фильтр: токен из Authorization header (длина: {})", token.length());
            return token;
        }
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isEmpty()) {
            logger.debug("JWT фильтр: токен из параметра запроса (длина: {})", tokenParam.length());
            return tokenParam;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt_token".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    logger.debug("JWT фильтр: токен из cookie (длина: {})", token.length());
                    return token;
                }
            }
        }
        logger.debug("JWT фильтр: токен не найден в запросе");
        return null;
    }


    private void sendUnauthorizedError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = String.format(
                "{\"result\": false, \"error\": \"%s\", \"code\": 401}",
                message.replace("\"", "\\\"")
        );

        response.getWriter().write(jsonResponse);
    }


    private void sendInternalError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = String.format(
                "{\"result\": false, \"error\": \"%s\", \"code\": 500}",
                "Внутренняя ошибка сервера".replace("\"", "\\\"")
        );

        response.getWriter().write(jsonResponse);
    }
}