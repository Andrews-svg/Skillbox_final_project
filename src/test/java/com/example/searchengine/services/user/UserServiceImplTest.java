package com.example.searchengine.services.user;

import com.example.searchengine.models.AppUser;
import com.example.searchengine.models.Role;
import com.example.searchengine.repositories.RoleRepository;
import com.example.searchengine.repositories.UserRepository;
import com.example.searchengine.services.EmailService;
import com.example.searchengine.validation.PasswordValidator;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl.registerNewUser — afterCommit vs fallback")
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

        when(passwordValidator.isValid("Password1!")).thenReturn(true);
        when(userQueryService.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("encoded-pass");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Nested
    @DisplayName("Без активной транзакции (fallback)")
    class WithoutTransaction {

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
    @DisplayName("С активной транзакцией (afterCommit)")
    class WithTransaction {

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
}
