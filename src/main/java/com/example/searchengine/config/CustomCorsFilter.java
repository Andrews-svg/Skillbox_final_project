package com.example.searchengine.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@WebFilter(urlPatterns = {"/*"})
public class CustomCorsFilter implements Filter {

    private static final Logger logger =
            LoggerFactory.getLogger(CustomCorsFilter.class);

    private List<String> allowedOrigins = Arrays.asList(
            "https://www.lenta.ru",
            "https://www.skillbox.ru",
            "https://www.playback.ru"
    );

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {

            String origin = request.getHeader("Origin");

            if (allowedOrigins.contains(origin)) {
                response.setHeader("Access-Control-Allow-Origin", origin);
            } else {
                response.setHeader("Access-Control-Allow-Origin", "");
            }

            response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Access-Control-Allow-Headers",
                    "x-requested-with, authorization, " +
                            "Content-Type, Authorization, " +
                            "credential, X-XSRF-TOKEN");

            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                filterChain.doFilter(request, response);
            }
        } catch (Exception e) {
            logger.error("Error occurred while processing CORS filter", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error has occurred.");
        }
    }

    @Override
    public void init(jakarta.servlet.FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}