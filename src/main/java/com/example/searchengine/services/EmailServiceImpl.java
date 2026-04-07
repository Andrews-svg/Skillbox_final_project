package com.example.searchengine.services;

import com.example.searchengine.models.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public EmailServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Async
    @Override
    public void sendActivationEmail(AppUser user) {
        if (user == null) {
            logger.error("Попытка отправки активационного письма для null пользователя");
            return;
        }
        String username = user.getUsername();
        String email = user.getEmail();
        String token = user.getActivationToken();
        if (!StringUtils.hasText(email)) {
            logger.error("Email пустой для пользователя {}", username);
            return;
        }
        if (!StringUtils.hasText(token)) {
            logger.error("ActivationToken пустой для пользователя {}", username);
            return;
        }
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Подготовка активационного письма для {}", username);
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Активируйте ваш аккаунт!");
            message.setText(
                    "Здравствуйте!\n\n" +
                            "Пожалуйста, подтвердите ваш аккаунт, перейдя по ссылке:\n" +
                            "http://localhost:8080/api/auth/activate/" + token
            );
            if (logger.isDebugEnabled()) {
                logger.debug("Отправка активационного письма для {}", username);
            }

            javaMailSender.send(message);
            logger.info("Активационное письмо отправлено для {}", username);

        } catch (MailSendException mse) {
            logger.error("Ошибка SMTP для {}: {}", username, mse.getMessage());
        } catch (MailException me) {
            logger.error("Ошибка почтового сервера для {}: {}", username, me.getMessage());
        } catch (Exception ex) {
            logger.error("Неожиданная ошибка для {}: {}", username, ex.getMessage(), ex);
        }
    }

    @Async
    @Override
    public void sendPasswordResetEmail(AppUser user) {
        if (user == null || !StringUtils.hasText(user.getEmail()) ||
                !StringUtils.hasText(user.getResetToken())) {
            logger.error("Невалидные данные для сброса пароля");
            return;
        }
        String resetLink = "http://localhost:8080/reset-password/" + user.getResetToken();
        String subject = "Сброс пароля";
        String body = "Перейдите по ссылке для сброса пароля: " + resetLink;

        sendEmail(user.getEmail(), subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            if (!StringUtils.hasText(to)) {
                logger.error("Пустой адрес для отправки письма");
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            javaMailSender.send(message);
        } catch (Exception e) {
            logger.error("Ошибка отправки письма на {}: {}", to, e.getMessage());
        }
    }
}