package com.spiritlane.service;

public interface MailService {

    void sendOtpEmail(String toEmail, String otp, String purposeLabel);
}