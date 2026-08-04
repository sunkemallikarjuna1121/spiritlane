package com.spiritlane.service.impl;

import com.spiritlane.exception.BusinessException;
import com.spiritlane.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public MailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp, String purposeLabel) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("SpiritLane - Your OTP for " + purposeLabel);
            message.setText(
                    "Hello,\n\n" +
                    "Your One-Time Password (OTP) for " + purposeLabel + " on SpiritLane is:\n\n" +
                    "        " + otp + "\n\n" +
                    "This OTP is valid for 10 minutes. Please do not share this OTP with anyone.\n\n" +
                    "If you did not request this, please ignore this email.\n\n" +
                    "- Team SpiritLane"
            );
            mailSender.send(message);
            log.info("[MailServiceImpl] OTP email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("[MailServiceImpl] Failed to send OTP email to {}", toEmail, e);
            throw new BusinessException("Failed to send OTP email. Please try again in a moment.");
        }
    }
}