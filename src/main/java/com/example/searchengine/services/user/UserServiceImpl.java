package com.example.searchengine.services.user;


import com.example.searchengine.config.security.PasswordValidator;
import com.example.searchengine.exceptions.UserRegistrationException;
import com.example.searchengine.exceptions.UsernameAlreadyExistsException;
import com.example.searchengine.models.AccountStatus;
import com.example.searchengine.models.AppUser;
import com.example.searchengine.models.Role;
import com.example.searchengine.repositories.RoleRepository;
import com.example.searchengine.repositories.UserRepository;
import com.example.searchengine.services.EmailService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String DEFAULT_USER_ROLE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordValidator passwordValidator;
    private final UserQueryService userQueryService;


    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            PasswordValidator passwordValidator,
            UserQueryService userQueryService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.passwordValidator = passwordValidator;
        this.userQueryService = userQueryService;
    }


    @Override
    @Transactional
    public void registerNewUser(AppUser newUser) {
        validateUserForRegistration(newUser);
        log.info("Регистрация пользователя: {}", newUser.getUsername());
        checkUsernameAvailability(newUser.getUsername());
        String rawPassword = newUser.getPassword();
        if (isPasswordAlreadyEncoded(rawPassword)) {
            log.error("Ошибка: получен уже закодированный пароль при регистрации пользователя '{}'",
                    newUser.getUsername());
            throw new UserRegistrationException(
                    "При регистрации должен использоваться незакодированный пароль"
            );
        }
        String encodedPassword = passwordEncoder.encode(rawPassword);
        newUser.setPassword(encodedPassword);
        assignDefaultRole(newUser);
        newUser.setStatus(AccountStatus.UNCONFIRMED);
        if (newUser.getActivationToken() == null) {
            newUser.setActivationToken(generateActivationToken());
        }
        try {
            userRepository.save(newUser);
            log.info("Пользователь '{}' успешно зарегистрирован. ID: {}",
                    newUser.getUsername(), newUser.getId());
            emailService.sendActivationEmail(newUser);
        } catch (Exception e) {
            log.error("Ошибка сохранения пользователя '{}': {}",
                    newUser.getUsername(), e.getMessage(), e);
            throw new UserRegistrationException("Ошибка при сохранении пользователя", e);
        }
    }


    private String generateActivationToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }


    @Override
    @Transactional
    public void activateAccount(String activationToken) {
        validateToken(activationToken);
        log.info("Активация учетной записи по токену: {}", activationToken);
        Optional<AppUser> userOptional = userRepository.findByActivationToken(activationToken);
        if (userOptional.isEmpty()) {
            log.error("Пользователь с токеном активации '{}' не найден", activationToken);
            throw new EntityNotFoundException("Неверный токен активации");
        }
        AppUser user = userOptional.get();
        if (user.getStatus() == AccountStatus.CONFIRMED) {
            log.warn("Аккаунт пользователя '{}' уже активирован", user.getUsername());
            return;
        }
        user.setStatus(AccountStatus.CONFIRMED);
        user.setActivationToken(null);
        userRepository.save(user);
        log.info("Аккаунт пользователя '{}' успешно активирован", user.getUsername());
    }


    @Override
    public void save(AppUser user) {
        if (user == null) throw new IllegalArgumentException("Пользователь не может быть null");
        log.debug("Сохранение пользователя: {}", user.getUsername());
        if (StringUtils.hasText(user.getPassword())) {
            if (!isPasswordAlreadyEncoded(user.getPassword())) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
        }
        userRepository.save(user);
    }


    @Override
    @Transactional
    public void update(AppUser updatedUser) {
        validateUserForUpdate(updatedUser);
        AppUser existingUser = findExistingUser(updatedUser.getId());
        updateUserFields(existingUser, updatedUser);
        handlePasswordUpdate(existingUser, updatedUser.getPassword());
        saveUpdatedUser(existingUser);
    }


    private void saveUpdatedUser(AppUser user) {
        save(user);
        log.info("Пользователь '{}' успешно обновлен", user.getUsername());
    }


    @Override
    @Transactional
    public AppUser updateProfile(AppUser updatedUser) {
        if (updatedUser == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        if (updatedUser.getId() == null) {
            throw new IllegalArgumentException("ID пользователя не может быть null при обновлении");
        }
        if (!StringUtils.hasText(updatedUser.getEmail())) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }
        log.debug("Обновление профиля пользователя с ID: {}", updatedUser.getId());
        AppUser existingUser = userRepository.findById(updatedUser.getId())
                .orElseThrow(() -> {
                    log.error("Ошибка: пользователь с ID {} не найден", updatedUser.getId());
                    return new EntityNotFoundException("Пользователь не найден");
                });
        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setLastName(updatedUser.getLastName());
        existingUser.setEmail(updatedUser.getEmail());
        AppUser savedUser = userRepository.save(existingUser);
        log.info("Профиль пользователя '{}' успешно обновлен", savedUser.getUsername());
        return savedUser;
    }


    @Override
    public boolean isPasswordAlreadyEncoded(String password) {
        if (!StringUtils.hasText(password)) {
            return false;
        }
        if (password.startsWith("$2a$") ||
                password.startsWith("$2b$") ||
                password.startsWith("$2y$")) {
            return true;
        }
        if (password.startsWith("{bcrypt}") ||
                password.startsWith("{pbkdf2}") ||
                password.startsWith("{scrypt}") ||
                password.startsWith("{sha256}")) {
            return true;
        }
        return password.length() > 50 && password.contains("$");
    }


    private void validateUserForUpdate(AppUser user) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        if (user.getId() == null) {
            throw new IllegalArgumentException("ID пользователя не может быть null при обновлении");
        }
    }


    private AppUser findExistingUser(Long userId) {
        log.debug("Поиск пользователя с ID: {}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с ID {} не найден", userId);
                    return new EntityNotFoundException("Пользователь не найден");
                });
    }


    private void updateUserFields(AppUser existingUser, AppUser updatedUser) {
        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setLastName(updatedUser.getLastName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setRoles(updatedUser.getRoles());
        existingUser.setStatus(updatedUser.getStatus());
    }


    private void handlePasswordUpdate(AppUser user, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            return;
        }
        log.debug("Обновление пароля для '{}': длина={}",
                user.getUsername(), newPassword.length());
        if (!isPasswordAlreadyEncoded(newPassword)) {
            String encodedPassword = passwordEncoder.encode(newPassword);
            user.setPassword(encodedPassword);
            log.debug("Пароль закодирован для пользователя '{}'", user.getUsername());
        } else {
            user.setPassword(newPassword);
            log.debug("Использован уже закодированный пароль для пользователя '{}'",
                    user.getUsername());
        }
    }


    private void validateUserForRegistration(AppUser user) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        if (!StringUtils.hasText(user.getUsername())) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            throw new IllegalArgumentException("Пароль не может быть пустым");
        }
        if (!passwordValidator.isValid(user.getPassword())) {
            throw new IllegalArgumentException("Пароль не удовлетворяет требованиям безопасности");
        }
        if (!StringUtils.hasText(user.getEmail())) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }
    }


    private void validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Токен не может быть пустым");
        }
    }


    private void checkUsernameAvailability(String username) {
        if (userQueryService.existsByUsername(username)) {
            log.error("Пользователь с именем '{}' уже существует", username);
            throw new UsernameAlreadyExistsException("Пользователь с таким именем уже существует");
        }
    }


    @Override
    public void assignDefaultRole(AppUser user) {
        Role userRole = roleRepository.findByName(DEFAULT_USER_ROLE)
                .orElseThrow(() -> {
                    log.error("Роль '{}' не найдена в базе данных", DEFAULT_USER_ROLE);
                    return new IllegalStateException("Роль USER отсутствует в базе данных");
                });
        user.setRoles(Collections.singletonList(userRole));
        log.debug("Роль '{}' назначена пользователю '{}'", DEFAULT_USER_ROLE, user.getUsername());
    }


    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void updateUserRoles(Long userId, List<String> roleNames) {
        AppUser user = userQueryService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
        List<Role> roles = roleRepository.findAllByNameIn(roleNames);
        if (roles.size() != roleNames.size()) {
            List<String> foundRoleNames = roles.stream()
                    .map(Role::getName)
                    .toList();
            List<String> missingRoles = roleNames.stream()
                    .filter(r -> !foundRoleNames.contains(r))
                    .toList();
            log.error("Роли не найдены: {}", missingRoles);
            throw new IllegalArgumentException("Некоторые роли не найдены: " + missingRoles);
        }
        user.setRoles(roles);
        userRepository.save(user);
        log.info("Admin updated roles for user {}: {}", user.getUsername(), roleNames);
    }
}