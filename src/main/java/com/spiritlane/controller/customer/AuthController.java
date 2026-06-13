package com.spiritlane.controller.customer;

import com.spiritlane.dto.RegistrationDto;
import com.spiritlane.exception.BusinessException;
import com.spiritlane.service.UserService;
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
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
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
                           RedirectAttributes redirectAttrs,
                           Model model) {
        logger.info("[AuthController] register() : START");
        if (result.hasErrors()) {
            return "auth/register";
        }
        if (!dto.isPasswordMatch()) {
            model.addAttribute("errorMsg", "Passwords do not match.");
            return "auth/register";
        }
        try {
            dto.setRole("ROLE_CUSTOMER");
            userService.register(dto);
            redirectAttrs.addFlashAttribute("successMsg",
                    "Account created successfully! Please login to continue.");
            logger.info("[AuthController] register() : END");
            return "redirect:/auth/login";
        } catch (BusinessException e) {
            model.addAttribute("errorMsg", e.getMessage());
            logger.info("[AuthController] register() : END");
            return "auth/register";
        }
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
                               RedirectAttributes redirectAttrs,
                               Model model) {
        logger.info("[AuthController] registerShop() : START");
        if (result.hasErrors()) return "auth/register-shop";
        if (!dto.isPasswordMatch()) {
            model.addAttribute("errorMsg", "Passwords do not match.");
            return "auth/register-shop";
        }
        try {
            dto.setRole("ROLE_SHOP_OWNER");
            userService.register(dto);
            redirectAttrs.addFlashAttribute("successMsg",
                    "Shop owner account created! Login and register your shop.");
            logger.info("[AuthController] registerShop() : END");
            return "redirect:/auth/login";
        } catch (BusinessException e) {
            model.addAttribute("errorMsg", e.getMessage());
            logger.info("[AuthController] registerShop() : END");
            return "auth/register-shop";
        }
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
                                RedirectAttributes redirectAttrs,
                                Model model) {
        logger.info("[AuthController] registerAgent() : START");
        if (result.hasErrors()) return "auth/register-agent";
        if (!dto.isPasswordMatch()) {
            model.addAttribute("errorMsg", "Passwords do not match.");
            return "auth/register-agent";
        }
        try {
            dto.setRole("ROLE_DELIVERY_AGENT");
            userService.register(dto);
            redirectAttrs.addFlashAttribute("successMsg",
                    "Delivery agent account created! Please login.");
        logger.info("[AuthController] registerAgent() : END");
            return "redirect:/auth/login";
        } catch (BusinessException e) {
            model.addAttribute("errorMsg", e.getMessage());
        logger.info("[AuthController] registerAgent() : END");
            return "auth/register-agent";
        }
    }

    /* ─── Age Verification (landing gate) ─── */
    @GetMapping("/age-verify")
    public String ageVerifyPage() {
        logger.info("[AuthController] ageVerifyPage() : START");
        logger.info("[AuthController] ageVerifyPage() : END");
        return "auth/age-verify";
    }
}
