package com.spiritlane.controller.customer;

import com.spiritlane.dto.RegistrationDto;
import com.spiritlane.entity.OtpVerification;
import com.spiritlane.exception.BusinessException;
import com.spiritlane.service.OtpService;
import com.spiritlane.service.UserService;
import com.spiritlane.util.AppUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger =
        LoggerFactory.getLogger(AuthController.class);

    private static final String SESSION_PENDING_REGISTRATION = "pendingRegistrationDto";
    private static final String SESSION_OTP_EMAIL            = "otpEmail";
    private static final String SESSION_OTP_PURPOSE          = "otpPurpose";
    private static final String SESSION_RESET_VERIFIED_EMAIL = "resetPasswordVerifiedEmail";

    private final UserService userService;
    private final OtpService otpService;

    public AuthController(UserService userService, OtpService otpService) {
        this.userService = userService;
        this.otpService = otpService;
    }

    /* ─── Login ─── */
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        logger.info("[AuthController] loginPage() : START");
        if (error != null) {
            model.addAttribute("errorMsg", "Invalid email or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("successMsg", "You have been logged out successfully.");
        }
        logger.info("[AuthController] loginPage() : END");
        return "auth/login";
    }

    /* ─── Register (Customer) ─── */
    @GetMapping("/register")
    public String registerPage(Model model) {
        logger.info("[AuthController] registerPage() : START");
        model.addAttribute("registrationDto", new RegistrationDto());
        logger.info("[AuthController] registerPage() : END");
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationDto") RegistrationDto dto,
                           BindingResult result,
                           HttpSession session,
                           Model model) {
        logger.info("[AuthController] register() : START");
        if (result.hasErrors()) {
            return "auth/register";
        }
        if (!dto.isPasswordMatch()) {
            model.addAttribute("errorMsg", "Passwords do not match.");
            return "auth/register";
        }
        dto.setRole("ROLE_CUSTOMER");
        String view = initiateRegistrationOtp(dto, "auth/register", session, model);
        logger.info("[AuthController] register() : END");
        return view;
    }

    /* ─── Register as Shop Owner ─── */
    @GetMapping("/register-shop")
    public String registerShopPage(Model model) {
        logger.info("[AuthController] registerShopPage() : START");
        RegistrationDto dto = new RegistrationDto();
        dto.setRole("ROLE_SHOP_OWNER");
        model.addAttribute("registrationDto", dto);
        logger.info("[AuthController] registerShopPage() : END");
        return "auth/register-shop";
    }

    @PostMapping("/register-shop")
    public String registerShop(@Valid @ModelAttribute("registrationDto") RegistrationDto dto,
                               BindingResult result,
                               HttpSession session,
                               Model model) {
        logger.info("[AuthController] registerShop() : START");
        if (result.hasErrors()) return "auth/register-shop";
        if (!dto.isPasswordMatch()) {
            model.addAttribute("errorMsg", "Passwords do not match.");
            return "auth/register-shop";
        }
        dto.setRole("ROLE_SHOP_OWNER");
        String view = initiateRegistrationOtp(dto, "auth/register-shop", session, model);
        logger.info("[AuthController] registerShop() : END");
        return view;
    }

    /* ─── Register as Delivery Agent ─── */
    @GetMapping("/register-agent")
    public String registerAgentPage(Model model) {
        logger.info("[AuthController] registerAgentPage() : START");
        RegistrationDto dto = new RegistrationDto();
        dto.setRole("ROLE_DELIVERY_AGENT");
        model.addAttribute("registrationDto", dto);
        logger.info("[AuthController] registerAgentPage() : END");
        return "auth/register-agent";
    }

    @PostMapping("/register-agent")
    public String registerAgent(@Valid @ModelAttribute("registrationDto") RegistrationDto dto,
                                BindingResult result,
                                HttpSession session,
                                Model model) {
        logger.info("[AuthController] registerAgent() : START");
        if (result.hasErrors()) return "auth/register-agent";
        if (!dto.isPasswordMatch()) {
            model.addAttribute("errorMsg", "Passwords do not match.");
            return "auth/register-agent";
        }
        dto.setRole("ROLE_DELIVERY_AGENT");
        String view = initiateRegistrationOtp(dto, "auth/register-agent", session, model);
        logger.info("[AuthController] registerAgent() : END");
        return view;
    }

    /**
     * Shared step for all registration forms: validates uniqueness/age,
     * stashes the pending registration in session, sends an OTP to the
     * given email, and sends the user to the OTP verification page.
     */
    private String initiateRegistrationOtp(RegistrationDto dto, String viewOnError,
                                           HttpSession session, Model model) {
        try {
            if (userService.emailExists(dto.getEmail())) {
                throw new BusinessException("Email already registered. Please login.");
            }
            if (userService.phoneExists(dto.getPhone())) {
                throw new BusinessException("Phone number already registered.");
            }
            if (!AppUtils.isAdult(dto.getDateOfBirth())) {
                throw new BusinessException("You must be 18+ years old to register on SpiritLane.");
            }

            String email = dto.getEmail().toLowerCase().trim();
            session.setAttribute(SESSION_PENDING_REGISTRATION, dto);
            session.setAttribute(SESSION_OTP_EMAIL, email);
            session.setAttribute(SESSION_OTP_PURPOSE, OtpVerification.Purpose.REGISTRATION.name());

            otpService.generateAndSendOtp(email, OtpVerification.Purpose.REGISTRATION);
            return "redirect:/auth/verify-otp";
        } catch (BusinessException e) {
            model.addAttribute("errorMsg", e.getMessage());
            return viewOnError;
        }
    }

    /* ─── OTP Verification (shared by Registration & Forgot Password) ─── */
    @GetMapping("/verify-otp")
    public String verifyOtpPage(HttpSession session, Model model, RedirectAttributes redirectAttrs) {
        logger.info("[AuthController] verifyOtpPage() : START");
        String email = (String) session.getAttribute(SESSION_OTP_EMAIL);
        String purpose = (String) session.getAttribute(SESSION_OTP_PURPOSE);
        if (email == null || purpose == null) {
            redirectAttrs.addFlashAttribute("errorMsg", "Your session has expired. Please start again.");
            logger.info("[AuthController] verifyOtpPage() : END - session expired");
            return "redirect:/auth/login";
        }
        model.addAttribute("maskedEmail", AppUtils.maskEmail(email));
        model.addAttribute("purpose", purpose);
        logger.info("[AuthController] verifyOtpPage() : END");
        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtpSubmit(@RequestParam String otp,
                                  HttpSession session,
                                  RedirectAttributes redirectAttrs,
                                  Model model) {
        logger.info("[AuthController] verifyOtpSubmit() : START");
        String email = (String) session.getAttribute(SESSION_OTP_EMAIL);
        String purposeStr = (String) session.getAttribute(SESSION_OTP_PURPOSE);

        if (email == null || purposeStr == null) {
            redirectAttrs.addFlashAttribute("errorMsg", "Your session has expired. Please start again.");
            logger.info("[AuthController] verifyOtpSubmit() : END - session expired");
            return "redirect:/auth/login";
        }

        OtpVerification.Purpose purpose = OtpVerification.Purpose.valueOf(purposeStr);

        try {
            otpService.verifyOtp(email, otp, purpose);

            if (purpose == OtpVerification.Purpose.REGISTRATION) {
                RegistrationDto dto = (RegistrationDto) session.getAttribute(SESSION_PENDING_REGISTRATION);
                if (dto == null) {
                    redirectAttrs.addFlashAttribute("errorMsg", "Your session has expired. Please register again.");
                    logger.info("[AuthController] verifyOtpSubmit() : END - no pending registration");
                    return "redirect:/auth/register";
                }
                userService.register(dto);
                session.removeAttribute(SESSION_PENDING_REGISTRATION);
                session.removeAttribute(SESSION_OTP_EMAIL);
                session.removeAttribute(SESSION_OTP_PURPOSE);
                redirectAttrs.addFlashAttribute("successMsg",
                        "Email verified! Your account has been created successfully. Please login.");
                logger.info("[AuthController] verifyOtpSubmit() : END - registration complete");
                return "redirect:/auth/login";
            } else {
                // FORGOT_PASSWORD
                session.setAttribute(SESSION_RESET_VERIFIED_EMAIL, email);
                session.removeAttribute(SESSION_OTP_EMAIL);
                session.removeAttribute(SESSION_OTP_PURPOSE);
                logger.info("[AuthController] verifyOtpSubmit() : END - password reset OTP verified");
                return "redirect:/auth/reset-password";
            }
        } catch (BusinessException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("maskedEmail", AppUtils.maskEmail(email));
            model.addAttribute("purpose", purposeStr);
            logger.info("[AuthController] verifyOtpSubmit() : END - invalid otp");
            return "auth/verify-otp";
        }
    }

    @PostMapping("/verify-otp/resend")
    public String resendOtp(HttpSession session, RedirectAttributes redirectAttrs) {
        logger.info("[AuthController] resendOtp() : START");
        String email = (String) session.getAttribute(SESSION_OTP_EMAIL);
        String purposeStr = (String) session.getAttribute(SESSION_OTP_PURPOSE);

        if (email == null || purposeStr == null) {
            redirectAttrs.addFlashAttribute("errorMsg", "Your session has expired. Please start again.");
            logger.info("[AuthController] resendOtp() : END - session expired");
            return "redirect:/auth/login";
        }

        otpService.generateAndSendOtp(email, OtpVerification.Purpose.valueOf(purposeStr));
        redirectAttrs.addFlashAttribute("successMsg", "A new OTP has been sent to your email.");
        logger.info("[AuthController] resendOtp() : END");
        return "redirect:/auth/verify-otp";
    }

    /* ─── Forgot Password ─── */
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        logger.info("[AuthController] forgotPasswordPage() : START");
        logger.info("[AuthController] forgotPasswordPage() : END");
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(@RequestParam String email,
                                       HttpSession session,
                                       Model model) {
        logger.info("[AuthController] forgotPasswordSubmit() : START");
        try {
            if (!userService.emailExists(email)) {
                throw new BusinessException("No account found with this email address.");
            }
            String normalizedEmail = email.toLowerCase().trim();
            session.setAttribute(SESSION_OTP_EMAIL, normalizedEmail);
            session.setAttribute(SESSION_OTP_PURPOSE, OtpVerification.Purpose.FORGOT_PASSWORD.name());

            otpService.generateAndSendOtp(normalizedEmail, OtpVerification.Purpose.FORGOT_PASSWORD);
            logger.info("[AuthController] forgotPasswordSubmit() : END");
            return "redirect:/auth/verify-otp";
        } catch (BusinessException e) {
            model.addAttribute("errorMsg", e.getMessage());
            logger.info("[AuthController] forgotPasswordSubmit() : END - error");
            return "auth/forgot-password";
        }
    }

    /* ─── Reset Password (after OTP verified) ─── */
    @GetMapping("/reset-password")
    public String resetPasswordPage(HttpSession session, RedirectAttributes redirectAttrs) {
        logger.info("[AuthController] resetPasswordPage() : START");
        String email = (String) session.getAttribute(SESSION_RESET_VERIFIED_EMAIL);
        if (email == null) {
            redirectAttrs.addFlashAttribute("errorMsg", "Please verify the OTP before resetting your password.");
            logger.info("[AuthController] resetPasswordPage() : END - not verified");
            return "redirect:/auth/forgot-password";
        }
        logger.info("[AuthController] resetPasswordPage() : END");
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPasswordSubmit(@RequestParam String newPassword,
                                      @RequestParam String confirmPassword,
                                      HttpSession session,
                                      RedirectAttributes redirectAttrs,
                                      Model model) {
        logger.info("[AuthController] resetPasswordSubmit() : START");
        String email = (String) session.getAttribute(SESSION_RESET_VERIFIED_EMAIL);
        if (email == null) {
            redirectAttrs.addFlashAttribute("errorMsg", "Please verify the OTP before resetting your password.");
            logger.info("[AuthController] resetPasswordSubmit() : END - not verified");
            return "redirect:/auth/forgot-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMsg", "Passwords do not match.");
            logger.info("[AuthController] resetPasswordSubmit() : END - mismatch");
            return "auth/reset-password";
        }
        if (newPassword.length() < 8) {
            model.addAttribute("errorMsg", "Password must be at least 8 characters.");
            logger.info("[AuthController] resetPasswordSubmit() : END - too short");
            return "auth/reset-password";
        }
        userService.resetPasswordByEmail(email, newPassword);
        session.removeAttribute(SESSION_RESET_VERIFIED_EMAIL);
        redirectAttrs.addFlashAttribute("successMsg",
                "Password reset successfully! Please login with your new password.");
        logger.info("[AuthController] resetPasswordSubmit() : END");
        return "redirect:/auth/login";
    }

    /* ─── Age Verification (landing gate) ─── */
    @GetMapping("/age-verify")
    public String ageVerifyPage() {
        logger.info("[AuthController] ageVerifyPage() : START");
        logger.info("[AuthController] ageVerifyPage() : END");
        return "auth/age-verify";
    }
}
