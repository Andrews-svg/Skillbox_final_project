package com.example.searchengine.config;

import io.micrometer.common.lang.NonNull;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.regex.Pattern;


@Component
public class SecurityHeadersConfig {

    @Bean
    public Filter addResponseHeaderFilter() {
        return new AddResponseHeaderFilter();
    }

    public static class AddResponseHeaderFilter extends OncePerRequestFilter {

        private static final Logger logger = LoggerFactory.getLogger(AddResponseHeaderFilter.class);
        private static final Pattern UNSAFE_CHARACTERS = Pattern.compile("[^\\w-]");


        @Override
        protected void doFilterInternal(
                @NonNull HttpServletRequest request,
                @NonNull HttpServletResponse response,
                @NonNull FilterChain filterChain)
                throws ServletException, IOException {


            Enumeration<String> headers = request.getHeaderNames();
            while (headers.hasMoreElements()) {
                String headerName = headers.nextElement();
                String cleanedValue = UNSAFE_CHARACTERS.matcher(
                        request.getHeader(headerName)).replaceAll("");
                request.setAttribute(headerName, cleanedValue);
            }


            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            response.setHeader("X-Frame-Options", "SAMEORIGIN");
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");


            String contentSecurityPolicy =
                    "default-src 'self'; " +
                            "script-src 'self'; " +
                            "style-src 'self'; " +
                            "img-src 'self' data:; " +
                            "font-src 'self';";
            response.setHeader("Content-Security-Policy", contentSecurityPolicy);

            logger.trace("Filtered request: {}", request.getRequestURL());

            filterChain.doFilter(request, response);
        }
    }
}
