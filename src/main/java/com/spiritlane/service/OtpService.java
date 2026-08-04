package com.spiritlane.service;

import com.spiritlane.entity.OtpVerification;

public interface OtpService {

    /**
     * Generates a new OTP for the given email + purpose, invalidates any
     * previous unused OTPs for the same email + purpose, and emails it.
     */
    void generateAndSendOtp(String email, OtpVerification.Purpose purpose);

    /**
     * Verifies the OTP for the given email + purpose.
     * Throws BusinessException if invalid, expired, or attempts exceeded.
     */
    boolean verifyOtp(String email, String otp, OtpVerification.Purpose purpose);
}