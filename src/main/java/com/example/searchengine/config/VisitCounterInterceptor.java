package com.example.searchengine.config;

import com.example.searchengine.services.VisitCounterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class VisitCounterInterceptor implements HandlerInterceptor {

    private final VisitCounterService visitCounterService;

    public VisitCounterInterceptor(VisitCounterService visitCounterService) {
        this.visitCounterService = visitCounterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if ("/".equals(uri) || "/index.html".equals(uri)) {
            visitCounterService.increment();
        }
        return true;
    }
}
