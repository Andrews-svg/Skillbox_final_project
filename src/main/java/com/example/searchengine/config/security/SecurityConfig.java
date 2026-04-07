package com.example.searchengine.config.security;


import com.example.searchengine.services.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(AbstractHttpConfigurer::disable)

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**", "/web/api/**", "/csp-reports")
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )


                .authorizeHttpRequests(authorization -> authorization


                        .requestMatchers("/", "/layout").permitAll()

                        .requestMatchers("/home").permitAll()
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/register").permitAll()

                        .requestMatchers("/tab/home").permitAll()

                        .requestMatchers("/fragments/home").permitAll()
                        .requestMatchers("/fragments/login-fragment").permitAll()
                        .requestMatchers("/fragments/registration-fragment").permitAll()
                        .requestMatchers("/fragments/login").permitAll()
                        .requestMatchers("/fragments/registration").permitAll()
                        .requestMatchers("/fragments/navbar").permitAll()

                        .requestMatchers("/api/auth/**", "/api/public/**").permitAll()

                        .requestMatchers(
                                "/assets/**",
                                "/favicon/**",
                                "/images/**",
                                "/css/**",
                                "/js/**",
                                "/webjars/**",
                                "/static/**",
                                "/error",
                                "/csrf-token",
                                "/csp-reports"
                        ).permitAll()


                        .requestMatchers(
                                "/forgot-password",
                                "/reset-password",
                                "/password/forgot",
                                "/password/reset",
                                "/auth/activate/**",
                                "/csp-reports"
                        ).permitAll()


                        .requestMatchers("/tab/dashboard", "/tab/management", "/tab/search")
                        .hasAnyAuthority("USER", "ADMIN")

                        .requestMatchers("/fragment/dashboard", "/fragment/management", "/fragment/search")
                        .hasAnyAuthority("USER", "ADMIN")

                        .requestMatchers("/dashboard", "/management", "/search")
                        .hasAnyAuthority("USER", "ADMIN")


                        .requestMatchers("/api/user/**").hasAnyAuthority("USER", "ADMIN")


                        .requestMatchers("/user/profile", "/user/settings")
                        .hasAnyAuthority("USER", "ADMIN")

                        .requestMatchers("/password/**").authenticated()

                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")

                        .requestMatchers("/fragments/**").permitAll()

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )


                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .deleteCookies("JSESSIONID", "jwt_token")
                        .invalidateHttpSession(true)
                        .permitAll()
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().migrateSession()
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .sessionRegistry(sessionRegistry())
                )


                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }
}