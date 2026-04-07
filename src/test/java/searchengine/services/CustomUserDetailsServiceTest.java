//package searchengine.services;
//
//import com.example.searchengine.models.AccountStatus;
//import com.example.searchengine.models.AppUser;
//import com.example.searchengine.models.Role;
//import com.example.searchengine.repository.UserRepository;
//import com.example.searchengine.services.CustomUserDetailsService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.datasource.DriverManagerDataSource;
//import org.springframework.security.authentication.DisabledException;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import javax.sql.DataSource;
//import java.util.Collections;
//import java.util.Optional;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.when;
//
//public class CustomUserDetailsServiceTest {
//
//    @Mock
//    private UserRepository userRepository;
//
//    @InjectMocks
//    private CustomUserDetailsService customUserDetailsService;
//
//    private JdbcTemplate jdbcTemplate;
//
//    @BeforeEach
//    public void setUp() {
//        MockitoAnnotations.openMocks(this);
//
//        DataSource dataSource = new DriverManagerDataSource(
//                "jdbc:mysql://localhost:3306/search_engine",
//                "root",
//                "password"
//        );
//        jdbcTemplate = new JdbcTemplate(dataSource);
//    }
//
//    @Test
//    public void testLoadUserByUsername_Success() {
//        String username = "testUser";
//        AppUser user = new AppUser();
//        user.setUsername(username);
//        user.setPassword("hashedPassword");
//        user.setStatus(AccountStatus.CONFIRMED);
//        Role role = new Role();
//        role.setAuthority("USER");
//        user.setRoles(Collections.singletonList(role));
//
//        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
//
//        org.springframework.security.core.userdetails.UserDetails userDetails =
//                customUserDetailsService.loadUserByUsername(username);
//
//        assertNotNull(userDetails);
//        assertEquals(username, userDetails.getUsername());
//        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("USER")));
//    }
//
//    @Test
//    public void testLoadUserByUsername_UserNotFound() {
//        String username = "nonExistingUser";
//        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
//
//        assertThrows(UsernameNotFoundException.class, () ->
//                customUserDetailsService.loadUserByUsername(username));
//    }
//
//    @Test
//    public void testLoadUserByUsername_AccountNotConfirmed() {
//        String username = "testUser";
//        AppUser user = new AppUser();
//        user.setUsername(username);
//        user.setPassword("hashedPassword");
//        user.setStatus(AccountStatus.UNCONFIRMED);
//        Role role = new Role();
//        role.setAuthority("USER");
//        user.setRoles(Collections.singletonList(role));
//
//        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
//
//        assertThrows(DisabledException.class, () ->
//                customUserDetailsService.loadUserByUsername(username));
//    }
//
//    // Тесты для реального пользователя Max
//
//    @Test
//    public void testLoadRealUserByUsername_Success() {
//        String username = "Max";
//        AppUser user = new AppUser();
//        user.setUsername(username);
//        user.setPassword("$2a$10$syqRh5ul7T1ihIZBmeh69eJgWmRfJ3PaMDi.Az41ij.fI.cALhvN.");
//        user.setStatus(AccountStatus.CONFIRMED);
//        Role role = new Role();
//        role.setAuthority("USER");
//        user.setRoles(Collections.singletonList(role));
//        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
//        org.springframework.security.core.userdetails.UserDetails userDetails =
//                customUserDetailsService.loadUserByUsername(username);
//        assertNotNull(userDetails);
//        assertEquals(username, userDetails.getUsername());
//        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("USER")));
//        System.out.println("Тест с реальным пользователем Max успешно пройден");
//    }
//
//    @Test
//    public void testLoadRealUserByUsername_UserNotFound() {
//        String username = "nonExistingUser";
//        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
//
//        assertThrows(UsernameNotFoundException.class, () ->
//                customUserDetailsService.loadUserByUsername(username));
//        System.out.println("Тест с реальным пользователем nonExistingUser успешно пройден");
//    }
//
//
//    @Test
//    public void testLoadRealUserByUsername_AccountNotConfirmed() {
//        String username = "testUser";
//        AppUser user = new AppUser();
//        user.setUsername(username);
//        user.setPassword("hashedPassword");
//        user.setStatus(AccountStatus.UNCONFIRMED);
//        Role role = new Role();
//        role.setAuthority("USER");
//        user.setRoles(Collections.singletonList(role));
//        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
//        assertThrows(DisabledException.class, () ->
//                customUserDetailsService.loadUserByUsername(username));
//        System.out.println("Тест с пользователем testUser успешно пройден");
//    }
//}