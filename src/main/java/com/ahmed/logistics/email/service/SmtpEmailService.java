package com.ahmed.logistics.email.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean enabled;

    public SmtpEmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${app.mail.from:noreply@logistics.com}") String fromAddress,
            @Value("${app.mail.enabled:true}") boolean enabled
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.enabled = enabled;
    }

    @Override
    public void sendEmail(String recipient, String subject, String body) {
        if (!enabled) {
            log.info("Email service is disabled. Skipping email to: {}, subject: '{}'", recipient, subject);
            return;
        }

        if (mailSender == null) {
            log.info("JavaMailSender is not configured. Skipping email to: {}, subject: '{}'", recipient, subject);
            return;
        }

        if (recipient == null || recipient.isBlank()) {
            log.warn("Cannot send email: recipient address is null or blank. Subject: '{}'", subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipient.trim());
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent successfully to: {}, subject: '{}'", recipient, subject);
        } catch (Exception ex) {
            log.error("Failed to send email to: {}, subject: '{}'. Reason: {}", recipient, subject, ex.getMessage());
            // Do NOT rethrow to protect core business transactions from external side-effect failures
        }
    }
}
