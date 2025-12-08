package com.example.searchengine.config;

import com.example.searchengine.services.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Начало настройки SecurityFilterChain");

        OrRequestMatcher orRequestMatcher = getOrRequestMatcher();

        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(orRequestMatcher))

                .authorizeHttpRequests(authz -> {
                    logger.debug("Настройка разрешений для URL-адресов");

                    authz
                            .requestMatchers("/resources/**").permitAll()
                            .requestMatchers("/static/**", "/assets/**", "/favicon/**").permitAll()
                            .requestMatchers("/", "/login", "/auth/custom-error",
                                    "/api/statistics", "/error").permitAll()
                            .requestMatchers("/user/profile",
                                    "/user/settings").hasAnyRole("USER", "ADMIN")
                            .requestMatchers("/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated();

                    logger.debug("Настройка разрешений завершена");
                })

                .formLogin(form -> {
                    logger.debug("Настройка формы входа");
                    form
                            .loginPage("/login")
                            .loginProcessingUrl("/login")
                            .failureUrl("/login?error=true")
                            .defaultSuccessUrl("/dashboard", true);
                    logger.debug("Форма входа успешно настроена");
                });

        logger.info("SecurityFilterChain успешно настроен");
        return http.build();
    }

    private static OrRequestMatcher getOrRequestMatcher() {
        RequestMatcher[] ignoredPaths = {
                new AntPathRequestMatcher("/resources/**"),
                new AntPathRequestMatcher("/static/**"),
                new AntPathRequestMatcher("/assets/**"),
                new AntPathRequestMatcher("/favicon/**"),
                new AntPathRequestMatcher("/api/public/**"),
                new AntPathRequestMatcher("/healthcheck"),
                new AntPathRequestMatcher("/login")
        };
        return new OrRequestMatcher(ignoredPaths);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        logger.info("Создание BCryptPasswordEncoder");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        logger.info("Начало настройки AuthenticationManager");
        return config.getAuthenticationManager();
    }
}
