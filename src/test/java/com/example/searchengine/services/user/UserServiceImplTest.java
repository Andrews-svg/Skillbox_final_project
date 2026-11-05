package com.example.searchengine.services.user;

import com.example.searchengine.models.AccountStatus;
import com.example.searchengine.models.AppUser;
import com.example.searchengine.models.Role;
import com.example.searchengine.repositories.RoleRepository;
import com.example.searchengine.repositories.UserRepository;
import com.example.searchengine.services.EmailService;
import com.example.searchengine.validation.PasswordValidator;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private PasswordValidator passwordValidator;
    @Mock private UserQueryService userQueryService;

    @InjectMocks
    private UserServiceImpl userService;

    private AppUser newUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        newUser = new AppUser();
        newUser.setUsername("testuser");
        newUser.setPassword("Password1!");
        newUser.setEmail("test@example.com");

        userRole = new Role();
        userRole.setName("USER");
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Nested
    @DisplayName("registerNewUser — без активной транзакции (fallback)")
    class RegisterWithoutTransaction {

        @BeforeEach
        void stubHappyPath() {
            when(passwordValidator.isValid("Password1!")).thenReturn(true);
            when(userQueryService.existsByUsername("testuser")).thenReturn(false);
            when(passwordEncoder.encode("Password1!")).thenReturn("encoded-pass");
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
            when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("email отправлен немедленно, синхронизация не регистрируется")
        void sendsEmailImmediately() {
            assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

            userService.registerNewUser(newUser);

            verify(userRepository, times(1)).save(newUser);
            verify(emailService, times(1)).sendActivationEmail(newUser);
        }
    }

    @Nested
    @DisplayName("registerNewUser — с активной транзакцией (afterCommit)")
    class RegisterWithTransaction {

        @BeforeEach
        void stubHappyPath() {
            when(passwordValidator.isValid("Password1!")).thenReturn(true);
            when(userQueryService.existsByUsername("testuser")).thenReturn(false);
            when(passwordEncoder.encode("Password1!")).thenReturn("encoded-pass");
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
            when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("email не отправлен до commit, отправлен ровно один раз после afterCommit")
        void sendsEmailOnlyAfterCommit() {
            TransactionSynchronizationManager.initSynchronization();
            assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();

            userService.registerNewUser(newUser);

            verify(userRepository, times(1)).save(newUser);
            verify(emailService, never()).sendActivationEmail(any());

            List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);

            synchronizations.forEach(TransactionSynchronization::afterCommit);

            verify(emailService, times(1)).sendActivationEmail(newUser);
        }
    }

    @Nested
    @DisplayName("activateAccount")
    class ActivateAccount {

        private static final String TOKEN = "activation-token-123";

        @Test
        @DisplayName("UNCONFIRMED → CONFIRMED, токен занулён, save один раз")
        void happyPath() {
            AppUser user = buildUser(AccountStatus.UNCONFIRMED, TOKEN);
            when(userRepository.findByActivationToken(TOKEN)).thenReturn(Optional.of(user));

            userService.activateAccount(TOKEN);

            assertThat(user.getStatus()).isEqualTo(AccountStatus.CONFIRMED);
            assertThat(user.getActivationToken()).isNull();
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("уже CONFIRMED с оставшимся токеном → токен очищен, save один раз")
        void alreadyConfirmedWithToken_clearsToken() {
            AppUser user = buildUser(AccountStatus.CONFIRMED, TOKEN);
            when(userRepository.findByActivationToken(TOKEN)).thenReturn(Optional.of(user));

            userService.activateAccount(TOKEN);

            assertThat(user.getActivationToken()).isNull();
            assertThat(user.getStatus()).isEqualTo(AccountStatus.CONFIRMED);
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("уже CONFIRMED без токена → save НЕ вызывается")
        void alreadyConfirmedWithoutToken_noSave() {
            AppUser user = buildUser(AccountStatus.CONFIRMED, null);
            when(userRepository.findByActivationToken(TOKEN)).thenReturn(Optional.of(user));

            userService.activateAccount(TOKEN);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("неизвестный токен → EntityNotFoundException, save не вызывается")
        void invalidToken_throws() {
            when(userRepository.findByActivationToken(TOKEN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.activateAccount(TOKEN))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Неверный токен активации");

            verify(userRepository, never()).save(any());
        }

        private AppUser buildUser(AccountStatus status, String token) {
            AppUser user = new AppUser();
            user.setUsername("activate-test");
            user.setStatus(status);
            user.setActivationToken(token);
            return user;
        }
    }
}