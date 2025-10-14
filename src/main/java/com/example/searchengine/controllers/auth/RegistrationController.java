package com.example.searchengine.controllers.auth;

import com.example.searchengine.models.AppUser;
import com.example.searchengine.services.RegistrationService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.validation.Valid;


@Controller
@RequestMapping("/auth")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final MessageSource messageSource;

    public RegistrationController(RegistrationService registrationService,
                                  MessageSource messageSource) {
        this.registrationService = registrationService;
        this.messageSource = messageSource;
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("appUser") AppUser appUser,
                               BindingResult result,
                               Model model) {
        if (result.hasErrors()) {
            model.addAllAttributes(result.getModel());
            return "index";
        }

        try {
            registrationService.register(appUser);
            String successMessage = messageSource.getMessage("registration.successful",
                    null, LocaleContextHolder.getLocale());
            model.addAttribute("successMessage", successMessage);
            return "redirect:/login";
        } catch(IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "index";
        }
    }
}