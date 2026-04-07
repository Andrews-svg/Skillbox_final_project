package com.example.searchengine.services.user;


import com.example.searchengine.dto.UserSecurityInfo;
import com.example.searchengine.models.AppUser;
import com.example.searchengine.models.Role;
import com.example.searchengine.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {

    private static final Logger logger = LoggerFactory.getLogger(UserQueryServiceImpl.class);

    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;

    public UserQueryServiceImpl(UserRepository userRepository,
                                UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
    }


    @Override
    public Optional<AppUser> findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
        logger.debug("Поиск пользователя по username: {}", username);
        Optional<AppUser> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            logger.debug("Пользователь '{}' найден", username);
        } else {
            logger.debug("Пользователь '{}' не найден", username);
        }
        return user;
    }


    @Override
    public Optional<AppUser> findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID не может быть null");
        }
        if (id <= 0) {
            throw new IllegalArgumentException("ID должен быть положительным числом");
        }
        logger.debug("Поиск пользователя по ID: {}", id);
        return userRepository.findById(id);
    }


    @Override
    public AppUser findByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }
        logger.debug("Поиск пользователя по email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Пользователь с email '{}' не найден", email);
                    return new EntityNotFoundException("Пользователь не найден");
                });
    }


    @Override
    public AppUser findByResetToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Токен не может быть пустым");
        }
        logger.debug("Поиск пользователя по reset token");
        return userRepository.findByResetToken(token)
                .orElseThrow(() -> {
                    logger.warn("Пользователь с reset token не найден");
                    return new EntityNotFoundException("Неверный токен сброса пароля");
                });
    }


    @Override
    public Optional<AppUser> findByActivationToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Токен не может быть пустым");
        }
        logger.debug("Поиск пользователя по activation token");
        return userRepository.findByActivationToken(token);
    }


    @Override
    public boolean existsByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
        logger.debug("Проверка существования пользователя '{}'", username);
        boolean exists = userRepository.existsByUsername(username);
        logger.debug("Пользователь '{}' существует: {}", username, exists);
        return exists;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
        logger.debug("Делегирование загрузки UserDetails для пользователя: {}", username);
        try {
            return userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            logger.error("Пользователь '{}' не найден при загрузке UserDetails", username);
            throw e;
        } catch (DisabledException e) {
            logger.warn("Аккаунт пользователя '{}' не активен: {}", username, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Ошибка при загрузке UserDetails для '{}': {}", username, e.getMessage(), e);
            throw new UsernameNotFoundException("Ошибка при загрузке пользователя", e);
        }
    }

    @Override
    public boolean canUserAuthenticate(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
        logger.debug("Проверка возможности аутентификации для пользователя: {}", username);
        try {
            UserDetails userDetails = loadUserByUsername(username);
            return userDetails.isEnabled() &&
                    userDetails.isAccountNonLocked() &&
                    userDetails.isAccountNonExpired() &&
                    userDetails.isCredentialsNonExpired();
        } catch (UsernameNotFoundException | DisabledException e) {
            logger.debug("Пользователь '{}' не может аутентифицироваться: {}", username, e.getMessage());
            return false;
        }
    }



    @Override
    public UserSecurityInfo getUserSecurityInfo(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
        logger.debug("Получение информации о безопасности для пользователя: {}", username);
        AppUser user = findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Пользователь не найден: " + username));
        List<String> roles = user.getRoles().stream()
                .map(Role::getAuthority)
                .collect(Collectors.toList());
        return new UserSecurityInfo(
                user.getUsername(),
                user.getId(),
                user.getEmail(),
                roles,
                user.isEnabled(),
                user.isAccountNonLocked(),
                user.getStatus()
        );
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
