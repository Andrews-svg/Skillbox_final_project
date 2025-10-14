package com.example.searchengine.services;

import com.example.searchengine.models.LoginForm;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl implements LoginService {

    private final AuthenticationManager authenticationManager;
    private final AppUserDetailsService appUserDetailsService;
    private final MessageSource messageSource;

    public LoginServiceImpl(AuthenticationManager authenticationManager,
                            AppUserDetailsService appUserDetailsService,
                            MessageSource messageSource) {
        this.authenticationManager = authenticationManager;
        this.appUserDetailsService = appUserDetailsService;
        this.messageSource = messageSource;
    }

    @Override
    public Authentication authenticate(LoginForm loginForm) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginForm.getUsername(), loginForm.getPassword())
            );
            return authentication;
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException(messageSource.getMessage("login.bad_credentials",
                    null, LocaleContextHolder.getLocale()));
        }
    }
}