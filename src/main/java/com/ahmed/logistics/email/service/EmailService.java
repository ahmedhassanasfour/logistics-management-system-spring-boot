package com.ahmed.logistics.email.service;

public interface EmailService {

    void sendEmail(String recipient, String subject, String body);
}
