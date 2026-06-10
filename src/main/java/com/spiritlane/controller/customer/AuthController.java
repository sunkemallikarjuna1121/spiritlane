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

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /* ─── Login ─── */
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMsg", "Invalid email or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("successMsg", "You have been logged out successfully.");
        }
        return "auth/login";
    }

    /* ─── Register (Customer) ─── */
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationDto", new RegistrationDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationDto") RegistrationDto dto,
                           BindingResult result,
                           RedirectAttributes redirectAttrs,
                           Model model) {
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
            return "redirect:/auth/login";
        } catch (BusinessException e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "auth/register";
        }
    }

    /* ─── Register as Shop Owner ─── */
    @GetMapping("/register-shop")
    public String registerShopPage(Model model) {
        RegistrationDto dto = new RegistrationDto();
        dto.setRole("ROLE_SHOP_OWNER");
        model.addAttribute("registrationDto", dto);
        return "auth/register-shop";
    }

    @PostMapping("/register-shop")
    public String registerShop(@Valid @ModelAttribute("registrationDto") RegistrationDto dto,
                               BindingResult result,
                               RedirectAttributes redirectAttrs,
                               Model model) {
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
            return "redirect:/auth/login";
        } catch (BusinessException e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "auth/register-shop";
        }
    }

    /* ─── Register as Delivery Agent ─── */
    @GetMapping("/register-agent")
    public String registerAgentPage(Model model) {
        RegistrationDto dto = new RegistrationDto();
        dto.setRole("ROLE_DELIVERY_AGENT");
        model.addAttribute("registrationDto", dto);
        return "auth/register-agent";
    }

    @PostMapping("/register-agent")
    public String registerAgent(@Valid @ModelAttribute("registrationDto") RegistrationDto dto,
                                BindingResult result,
                                RedirectAttributes redirectAttrs,
                                Model model) {
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
            return "redirect:/auth/login";
        } catch (BusinessException e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "auth/register-agent";
        }
    }

    /* ─── Age Verification (landing gate) ─── */
    @GetMapping("/age-verify")
    public String ageVerifyPage() {
        return "auth/age-verify";
    }
}
