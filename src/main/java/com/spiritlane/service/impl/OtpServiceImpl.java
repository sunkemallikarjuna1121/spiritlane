package com.spiritlane.service.impl;

import com.spiritlane.entity.OtpVerification;
import com.spiritlane.exception.BusinessException;
import com.spiritlane.repository.OtpVerificationRepository;
import com.spiritlane.service.MailService;
import com.spiritlane.service.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

    private static final int OTP_VALIDITY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final OtpVerificationRepository otpRepository;
    private final MailService mailService;
    private final SecureRandom random = new SecureRandom();

    public OtpServiceImpl(OtpVerificationRepository otpRepository, MailService mailService) {
        this.otpRepository = otpRepository;
        this.mailService = mailService;
    }

    @Override
    @Transactional
    public void generateAndSendOtp(String email, OtpVerification.Purpose purpose) {
        String normalizedEmail = email.toLowerCase().trim();

        // Invalidate any previous unused OTPs for this email + purpose
        otpRepository.deleteByEmailAndPurpose(normalizedEmail, purpose);

        String otp = generateSixDigitOtp();

        OtpVerification entity = new OtpVerification();
        entity.setEmail(normalizedEmail);
        entity.setOtpCode(otp);
        entity.setPurpose(purpose);
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        entity.setIsUsed(false);
        entity.setAttemptCount(0);
        otpRepository.save(entity);

        String purposeLabel = purpose == OtpVerification.Purpose.REGISTRATION
                ? "Account Registration" : "Password Reset";
        mailService.sendOtpEmail(normalizedEmail, otp, purposeLabel);

        log.info("[OtpServiceImpl] Generated OTP for {} [{}]", normalizedEmail, purpose);
    }

    @Override
    @Transactional
    public boolean verifyOtp(String email, String otp, OtpVerification.Purpose purpose) {
        String normalizedEmail = email.toLowerCase().trim();

        OtpVerification entity = otpRepository
                .findTopByEmailAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(normalizedEmail, purpose)
                .orElseThrow(() -> new BusinessException("No OTP found. Please request a new one."));

        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("OTP has expired. Please request a new one.");
        }

        if (entity.getAttemptCount() != null && entity.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new BusinessException("Too many incorrect attempts. Please request a new OTP.");
        }

        if (!entity.getOtpCode().equals(otp)) {
            entity.setAttemptCount((entity.getAttemptCount() == null ? 0 : entity.getAttemptCount()) + 1);
            otpRepository.save(entity);
            throw new BusinessException("Invalid OTP. Please try again.");
        }

        entity.setIsUsed(true);
        otpRepository.save(entity);
        log.info("[OtpServiceImpl] OTP verified for {} [{}]", normalizedEmail, purpose);
        return true;
    }

    private String generateSixDigitOtp() {
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }
}