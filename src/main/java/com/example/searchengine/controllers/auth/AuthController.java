package com.example.searchengine.controllers.admin;

import com.example.searchengine.dto.JwtDto.TokenPair;
import com.example.searchengine.dto.registration.RegistrationDTO;
import com.example.searchengine.dto.statistics.requests.LoginRequest;
import com.example.searchengine.models.AccountStatus;
import com.example.searchengine.models.AppUser;
import com.example.searchengine.services.EmailService;
import com.example.searchengine.services.jwt.JwtTokenService;
import com.example.searchengine.services.user.UserQueryServiceImpl;
import com.example.searchengine.services.user.UserServiceImpl;
import com.example.searchengine.services.UserActivityService;
import com.example.searchengine.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserServiceImpl userServiceImpl;
    private final UserQueryServiceImpl userQueryServiceImpl;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final EmailService emailService;
    private final UserActivityService userActivityService;

    public AuthController(UserServiceImpl userServiceImpl,
                          UserQueryServiceImpl userQueryServiceImpl,
                          AuthenticationManager authenticationManager,
                          JwtTokenService jwtTokenService,
                          EmailService emailService,
                          UserActivityService userActivityService) {
        this.userServiceImpl = userServiceImpl;
        this.userQueryServiceImpl = userQueryServiceImpl;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.emailService = emailService;
        this.userActivityService = userActivityService;
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                   HttpServletRequest request) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        log.info("Попытка входа: {} с IP: {}", username, ipAddress);
        try {
            Optional<AppUser> userOpt = userQueryServiceImpl.findByUsername(username);
            if (userOpt.isPresent()) {
                AppUser user = userOpt.get();
                if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
                    log.warn("Пользователь {} заблокирован до {} с IP: {}",
                            username, user.getLockedUntil(), ipAddress);
                    userActivityService.logLoginFailed(
                            user.getId(),
                            username,
                            ipAddress,
                            userAgent,
                            "ACCOUNT_LOCKED"
                    );
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of(
                                    "result", false,
                                    "error", "Аккаунт временно заблокирован. Попробуйте позже.",
                                    "lockedUntil", user.getLockedUntil().toString()
                            ));
                }
            }
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Optional<AppUser> appUserOptional = userQueryServiceImpl.findByUsername(username);
            if (appUserOptional.isEmpty()) {
                log.error("Пользователь не найден после аутентификации: {}", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("result", false, "error", "Ошибка аутентификации"));
            }
            AppUser appUser = appUserOptional.get();
            if (appUser.getStatus() == AccountStatus.UNCONFIRMED) {
                log.warn("Попытка входа в неактивированный аккаунт: {}", username);
                userActivityService.logLoginFailed(
                        appUser.getId(),
                        username,
                        ipAddress,
                        userAgent,
                        "ACCOUNT_UNCONFIRMED"
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("result", false, "error",
                                "Аккаунт не активирован. Проверьте email."));
            }
            appUser.setLastLoginAt(Instant.now());
            appUser.resetFailedAttempts();
            appUser.unlockAccount();
            userServiceImpl.update(appUser);
            TokenPair tokenPair = jwtTokenService.generateTokenPair(
                    userDetails,
                    appUser.getId(),
                    appUser.getEmail()
            );
            userActivityService.logLoginSuccess(
                    appUser.getId(),
                    username,
                    ipAddress,
                    userAgent
            );
            log.info("Пользователь {} успешно вошёл в систему с IP: {}", username, ipAddress);
            Map<String, Object> response = new HashMap<>();
            response.put("result", true);
            response.put("token", tokenPair.getAccessToken());
            response.put("refreshToken", tokenPair.getRefreshToken());
            response.put("expiresIn", tokenPair.getExpiresIn());
            Map<String, Object> user = new HashMap<>();
            user.put("id", appUser.getId());
            user.put("username", appUser.getUsername());
            user.put("email", appUser.getEmail());
            user.put("firstName", appUser.getFirstName());
            user.put("lastName", appUser.getLastName());
            user.put("lastLoginAt", appUser.getLastLoginAt());
            response.put("user", user);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            log.warn("Неверный пароль для пользователя: {} с IP: {}", username, ipAddress);
            userQueryServiceImpl.findByUsername(username).ifPresent(user -> {
                user.incrementFailedAttempts();
                if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                    Instant lockUntil = Instant.now().plusSeconds(900);
                    user.lockAccount(lockUntil);
                    log.warn("Аккаунт {} заблокирован до {} из-за множества неудачных попыток",
                            username, lockUntil);
                }
                userServiceImpl.update(user);
                userActivityService.logLoginFailed(
                        user.getId(),
                        username,
                        ipAddress,
                        userAgent,
                        "INVALID_PASSWORD"
                );
            });
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("result", false, "error", "Неверный логин или пароль"));
        } catch (DisabledException e) {
            log.warn("Учетная запись отключена: {} с IP: {}", username, ipAddress);
            userQueryServiceImpl.findByUsername(username).ifPresent(user -> {
                userActivityService.logLoginFailed(
                        user.getId(),
                        username,
                        ipAddress,
                        userAgent,
                        "ACCOUNT_DISABLED"
                );
            });
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("result", false, "error", "Учетная запись отключена"));
        } catch (AuthenticationException e) {
            log.error("Ошибка аутентификации для {} с IP: {}: {}", username, ipAddress, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("result", false, "error", "Ошибка аутентификации"));
        }
    }


    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationDTO regDTO,
                                      HttpServletRequest request) {
        String username = regDTO.getUsername();
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        log.info("Регистрация нового пользователя: {} с IP: {}", username, ipAddress);
        if (userQueryServiceImpl.existsByUsername(username)) {
            log.warn("Имя пользователя уже занято: {} с IP: {}", username, ipAddress);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("result", false, "error", "Имя пользователя занято!"));
        }
        if (!regDTO.getPassword().equals(regDTO.getConfirmPassword())) {
            log.warn("Пароли не совпадают для пользователя {} с IP: {}", username, ipAddress);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("result", false, "error", "Пароли не совпадают"));
        }
        try {
            AppUser appUser = new AppUser();
            appUser.setUsername(regDTO.getUsername());
            appUser.setFirstName(regDTO.getFirstName());
            appUser.setLastName(regDTO.getLastName());
            appUser.setEmail(regDTO.getEmail());
            appUser.setPassword(regDTO.getPassword());
            String activationToken = TokenUtils.generateActivationToken();
            appUser.setActivationToken(activationToken);
            appUser.setStatus(AccountStatus.UNCONFIRMED);
            userServiceImpl.registerNewUser(appUser);
            emailService.sendActivationEmail(appUser);
            userActivityService.logRegistration(
                    appUser.getId(),
                    username,
                    ipAddress,
                    userAgent
            );
            log.info("Пользователь {} успешно зарегистрирован с IP: {}", username, ipAddress);
            Map<String, Object> response = new HashMap<>();
            response.put("result", true);
            response.put("message", "Регистрация успешна. Проверьте email для активации аккаунта.");
            response.put("userId", appUser.getId());
            response.put("username", appUser.getUsername());
            response.put("email", appUser.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при регистрации пользователя {} с IP: {}: ",
                    username, ipAddress, e);
            userActivityService.logRegistrationFailed(
                    username,
                    ipAddress,
                    userAgent,
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("result", false, "error",
                            "Ошибка при регистрации. Попробуйте позже."));
        }
    }


    @GetMapping("/activate/{token}")
    public ResponseEntity<?> activateAccount(@PathVariable String token,
                                             HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        log.info("Активация аккаунта по токену: {} с IP: {}", token, ipAddress);
        try {
            Optional<AppUser> userOpt = userQueryServiceImpl.findByActivationToken(token);
            if (userOpt.isEmpty()) {
                log.warn("Токен активации не найден: {} с IP: {}", token, ipAddress);
                userActivityService.logActivationFailed(
                        null,
                        "UNKNOWN",
                        ipAddress,
                        "INVALID_TOKEN"
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("result", false, "error",
                                "Неверный или устаревший токен активации"));
            }
            AppUser user = userOpt.get();
            if (user.getStatus() == AccountStatus.CONFIRMED) {
                log.warn("Попытка повторной активации аккаунта: {} с IP: {}",
                        user.getUsername(), ipAddress);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("result", false, "error",
                                "Аккаунт уже активирован"));
            }
            user.setStatus(AccountStatus.CONFIRMED);
            user.setActivationToken(null);
            userServiceImpl.update(user);
            userActivityService.logActivation(
                    user.getId(),
                    user.getUsername(),
                    ipAddress
            );
            log.info("Аккаунт {} успешно активирован с IP: {}", user.getUsername(), ipAddress);
            return ResponseEntity.ok(Map.of(
                    "result", true,
                    "message", "Аккаунт успешно активирован. Теперь вы можете войти в систему."
            ));
        } catch (Exception e) {
            log.error("Ошибка активации аккаунта с IP: {}: {}", ipAddress, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("result", false, "error", "Ошибка активации аккаунта"));
        }
    }


    @GetMapping("/check-username/{username}")
    public ResponseEntity<?> checkUsernameAvailability(@PathVariable String username,
                                                       HttpServletRequest request) {
        boolean exists = userQueryServiceImpl.existsByUsername(username);
        log.debug("Проверка доступности имени {} с IP: {} - {}",
                username, request.getRemoteAddr(), exists ? "занято" : "свободно");
        Map<String, Object> response = new HashMap<>();
        response.put("result", true);
        response.put("username", username);
        response.put("available", !exists);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                    HttpServletRequest request,
                                    @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        String token = jwtTokenService.extractTokenFromHeader(authHeader);
        String username = "anonymous";
        Long userId = null;
        if (token != null && jwtTokenService.isValidTokenStructure(token)) {
            username = jwtTokenService.extractUsername(token);
            userId = jwtTokenService.extractUserId(token);
        }
        SecurityContextHolder.clearContext();
        userActivityService.logLogout(
                userId,
                username,
                request.getRemoteAddr(),
                userAgent
        );
        log.info("Пользователь {} вышел из системы с IP: {}", username, request.getRemoteAddr());
        return ResponseEntity.ok(Map.of(
                "result", true,
                "message", "Выход выполнен успешно"
        ));
    }


    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request,
                                          HttpServletRequest httpRequest) {
        String refreshToken = request.get("refreshToken");
        String ipAddress = httpRequest.getRemoteAddr();
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("result", false, "error", "Refresh token не предоставлен"));
        }
        try {
            String username = jwtTokenService.extractUsername(refreshToken);

            if (username == null) {
                throw new IllegalArgumentException("Недействительный refresh token");
            }
            Optional<AppUser> userOpt = userQueryServiceImpl.findByUsername(username);
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("Пользователь не найден");
            }
            AppUser user = userOpt.get();
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .authorities(user.getAuthorities())
                    .build();
            TokenPair tokenPair = jwtTokenService.generateTokenPair(
                    userDetails,
                    user.getId(),
                    user.getEmail()
            );
            userActivityService.logTokenRefresh(
                    user.getId(),
                    username,
                    ipAddress
            );
            log.info("Токен обновлен для пользователя {} с IP: {}", username, ipAddress);
            return ResponseEntity.ok(Map.of(
                    "result", true,
                    "token", tokenPair.getAccessToken(),
                    "refreshToken", tokenPair.getRefreshToken(),
                    "expiresIn", tokenPair.getExpiresIn()
            ));
        } catch (Exception e) {
            log.error("Ошибка обновления токена с IP: {}: {}", ipAddress, e.getMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("result", false, "error", "Недействительный refresh token"));
        }
    }
}