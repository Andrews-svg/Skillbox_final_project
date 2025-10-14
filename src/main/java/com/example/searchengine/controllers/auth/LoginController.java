package com.example.searchengine.controllers.auth;

import com.example.searchengine.models.LoginForm;
import com.example.searchengine.services.LoginService;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
public class LoginController {

    private final LoginService loginService;
    private final MessageSource messageSource;

    public LoginController(LoginService loginService,
                           MessageSource messageSource) {
        this.loginService = loginService;
        this.messageSource = messageSource;
    }

    @PostMapping("/login")
    public String handleLogin(LoginForm form, Model model) {
        try {
            Authentication authentication = loginService.authenticate(form);
            return "redirect:/dashboard";
        } catch(Exception ex) {
            String errorMsg = messageSource.getMessage(ex.getMessage(),
                    null, LocaleContextHolder.getLocale());
            model.addAttribute("errorMessage", errorMsg);
            return "login";
        }
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
