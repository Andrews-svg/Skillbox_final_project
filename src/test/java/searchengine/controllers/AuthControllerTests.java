//package searchengine.controllers;
//
//import com.example.searchengine.Application;
//import com.example.searchengine.controllers.api.AuthController;
//import com.example.searchengine.dto.ChangePasswordDto;
//import com.example.searchengine.dto.registration.RegistrationDTO;
//import com.example.searchengine.models.AppUser;
//import com.example.searchengine.services.EmailService;
//import com.example.searchengine.services.user.UserService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.persistence.EntityNotFoundException;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.MockitoAnnotations;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.springframework.web.context.WebApplicationContext;
//
//import java.util.Optional;
//
//import static org.hamcrest.Matchers.containsString;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//
//@SpringBootTest(classes = Application.class)
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//public class AuthControllerTests {
//
//    @Autowired
//    private WebApplicationContext wac;
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @InjectMocks
//    private AuthController controller;
//
//    @MockBean
//    private UserService userService;
//
//    @MockBean
//    private EmailService emailService;
//
//    @MockBean
//    private AuthenticationManager authenticationManager;
//
//    @MockBean
//    private BCryptPasswordEncoder bCryptPasswordEncoder;
//
//    @BeforeEach
//    public void setup() {
//        MockitoAnnotations.openMocks(this);
//        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
//    }
//
//
//    @Test
//    public void testShowLoginPage() throws Exception {
//        mockMvc.perform(get("/login"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(view().name("login"));
//    }
//
//
//    @Test
//    public void testRegisterNewUser_Successful() throws Exception {
//        RegistrationDTO registrationForm = new RegistrationDTO();
//        registrationForm.setUsername("newUser");
//        registrationForm.setFirstName("John");
//        registrationForm.setLastName("Doe");
//        registrationForm.setEmail("john@example.com");
//        registrationForm.setPassword("securepass");
//        registrationForm.setConfirmPassword("securepass");
//
//        mockMvc.perform(post("/register").
//                        flashAttr("registrationDTO", registrationForm))
//                .andDo(print())
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/login?message=activationSent"));
//    }
//
//
//    @Test
//    public void testRegisterNewUser_DuplicateUser() throws Exception {
//        RegistrationDTO registrationForm = new RegistrationDTO();
//        registrationForm.setUsername("existingUser");
//        registrationForm.setFirstName("Jane");
//        registrationForm.setLastName("Smith");
//        registrationForm.setEmail("jane@example.com");
//        registrationForm.setPassword("securepass");
//        registrationForm.setConfirmPassword("securepass");
//
//        when(userService.existsByUsername("existingUser")).thenReturn(true);
//
//        mockMvc.perform(post("/register").
//                        flashAttr("registrationDTO", registrationForm))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(view().name("registration"))
//                .andExpect(model().attribute("errorMessage",
//                        containsString("Имя пользователя занято")));
//    }
//
//
//    @Test
//    public void testChangePassword_Successful() throws Exception {
//        long userId = 1L;
//        ChangePasswordDto dto = new ChangePasswordDto();
//        dto.setOldPassword("oldSecurePass");
//        dto.setNewPassword("newSecurePass");
//        String dbHashedPassword = "$2a$10$gMJ1ugynsVDRclP5O2JD/uZvr36ZbtK.FmMXfCID8eZO8ZAo4U8qi";
//        AppUser existingUser = new AppUser();
//        existingUser.setPassword(dbHashedPassword);
//        existingUser.setId(userId);
//        when(userService.findById(userId)).thenReturn(Optional.of(existingUser));
//        when(bCryptPasswordEncoder.matches(eq(dto.getOldPassword()),
//                eq(dbHashedPassword))).thenReturn(true);
//
//        mockMvc.perform(put("/{userId}/change-password", userId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(asJsonString(dto)))
//                .andDo(print())
//                .andExpect(status().isNoContent());
//    }
//
//
//    @Test
//    public void testChangePassword_Failure_OldPasswordMismatch() throws Exception {
//        long userId = 1L;
//        ChangePasswordDto dto = new ChangePasswordDto();
//        dto.setOldPassword("wrongOldPass");
//        dto.setNewPassword("newSecurePass");
//
//        when(bCryptPasswordEncoder.matches(eq(dto.getOldPassword()),
//                any(String.class))).thenReturn(false);
//        when(userService.findById(userId)).thenReturn(Optional.of(new AppUser()));
//
//        mockMvc.perform(put("/{userId}/change-password", userId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(asJsonString(dto)))
//                .andDo(print())
//                .andExpect(status().isForbidden());
//    }
//
//
//    @Test
//    public void testActivateAccount_Successful() throws Exception {
//        String validToken = "valid-token";
//        doNothing().when(userService).activateAccount(validToken);
//
//        mockMvc.perform(get("/auth/activate/" + validToken))
//                .andDo(print())
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/login?message=accountActivated"));
//    }
//
//
//    @Test
//    public void testActivateAccount_InvalidToken() throws Exception {
//        String invalidToken = "invalid-token";
//        doThrow(EntityNotFoundException.class).when(userService).activateAccount(invalidToken);
//
//        mockMvc.perform(get("/auth/activate/" + invalidToken))
//                .andDo(print())
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/login?error=invalidToken"));
//    }
//
//
//    protected String asJsonString(final Object obj) {
//        try {
//            return new ObjectMapper().writeValueAsString(obj);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
//    @Test
//    public void testPasswordEncoding() {
//        String plainPassword = "oldSecurePass";
//        String dbHashedPassword = "$2a$10$gMJ1ugynsVDRclP5O2JD/uZvr36ZbtK.FmMXfCID8eZO8ZAo4U8qi";
//        boolean matches = bCryptPasswordEncoder.matches(plainPassword, dbHashedPassword);
//        System.out.println("Is password correct? " + matches);
//        assertTrue(matches, "The password does not match!");
//    }
//}