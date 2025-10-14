package com.example.searchengine.services;

import com.example.searchengine.models.AppUser;
import com.example.searchengine.repository.UserRepository;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;

    public RegistrationServiceImpl(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   MessageSource messageSource) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.messageSource = messageSource;
    }

    @Override
    @Transactional
    public AppUser register(AppUser appUser) {
        Optional<AppUser> existingUser = userRepository.findByUsername(appUser.getUsername());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException(messageSource.getMessage("registration.duplicate_user",
                    null, LocaleContextHolder.getLocale()));
        }

        appUser.setPassword(passwordEncoder.encode(appUser.getPassword()));
        appUser.setEnabled(true);
        return userRepository.save(appUser);
    }
}
