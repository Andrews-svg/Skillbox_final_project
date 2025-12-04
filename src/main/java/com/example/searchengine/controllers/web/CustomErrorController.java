package com.example.searchengine.controllers.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Locale;


@Controller
@RequestMapping("/error")
public class CustomErrorController implements ErrorController {

    @Autowired
    private MessageSource messageSource;

    @RequestMapping(value="/error")
    public String handleError(HttpServletRequest request, Model model, Locale locale) {

        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if(statusCode != null){
            int code = Integer.parseInt(statusCode.toString());

            switch(code){
                case 404:
                    model.addAttribute("errorMessage",
                            messageSource.getMessage("error.page.not.found", null, locale));
                    break;
                default:
                    model.addAttribute("errorMessage",
                            messageSource.getMessage("error.internal.server", null, locale));
            }
        }

        return "error";
    }
}