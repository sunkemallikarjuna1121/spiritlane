package com.spiritlane.controller.customer;

import com.spiritlane.dto.AddressDto;
import com.spiritlane.exception.BusinessException;
import com.spiritlane.entity.Order;
import com.spiritlane.security.CustomUserDetails;
import com.spiritlane.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    private final UserService userService;
    private final OrderService orderService;
    private final NotificationService notificationService;

    public CustomerController(UserService userService,
                               OrderService orderService,
                               NotificationService notificationService) {
        this.userService = userService;
        this.orderService = orderService;
        this.notificationService = notificationService;
    }

    /* ─── Home ─── */
    @GetMapping("/home")
    public String customerHome(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("user", principal.getUser());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(principal.getId()));
        return "redirect:/home";
    }

    /* ─── Profile ─── */
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("user",      principal.getUser());
        model.addAttribute("addresses", userService.getUserAddresses(principal.getId()));
        return "customer/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails principal,
                                @RequestParam String fullName,
                                @RequestParam String phone,
                                RedirectAttributes redirectAttrs) {
        try {
            userService.updateProfile(principal.getId(), fullName, phone);
            redirectAttrs.addFlashAttribute("successMsg", "Profile updated successfully.");
        } catch (BusinessException e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/customer/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@AuthenticationPrincipal CustomUserDetails principal,
                                 @RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttrs) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttrs.addFlashAttribute("errorMsg", "New passwords do not match.");
            return "redirect:/customer/profile";
        }
        try {
            userService.changePassword(principal.getId(), oldPassword, newPassword);
            redirectAttrs.addFlashAttribute("successMsg", "Password changed successfully.");
        } catch (BusinessException e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/customer/profile";
    }

    /* ─── Addresses ─── */
    @GetMapping("/addresses")
    public String addresses(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("addresses", userService.getUserAddresses(principal.getId()));
        model.addAttribute("addressDto", new AddressDto());
        return "customer/addresses";
    }

    @PostMapping("/addresses/add")
    public String addAddress(@AuthenticationPrincipal CustomUserDetails principal,
                             @Valid @ModelAttribute("addressDto") AddressDto dto,
                             BindingResult result,
                             RedirectAttributes redirectAttrs,
                             Model model) {
        if (result.hasErrors()) {
            model.addAttribute("addresses", userService.getUserAddresses(principal.getId()));
            return "customer/addresses";
        }
        try {
            userService.addAddress(principal.getId(), dto);
            redirectAttrs.addFlashAttribute("successMsg", "Address added successfully.");
        } catch (BusinessException e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/customer/addresses";
    }

    @PostMapping("/addresses/{id}/delete")
    public String deleteAddress(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        userService.deleteAddress(id);
        redirectAttrs.addFlashAttribute("successMsg", "Address removed.");
        return "redirect:/customer/addresses";
    }

    @PostMapping("/addresses/{id}/set-default")
    public String setDefault(@AuthenticationPrincipal CustomUserDetails principal,
                             @PathVariable Long id, RedirectAttributes redirectAttrs) {
        userService.setDefaultAddress(principal.getId(), id);
        redirectAttrs.addFlashAttribute("successMsg", "Default address updated.");
        return "redirect:/customer/addresses";
    }

    /* ─── Orders ─── */
    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal CustomUserDetails principal,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        PageRequest pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("orders", orderService.getCustomerOrders(principal.getId(), pageable));
        return "customer/orders";
    }

    @GetMapping("/orders/{orderNumber}")
    public String orderDetail(@PathVariable String orderNumber, Model model) {
        model.addAttribute("order", orderService.findByOrderNumber(orderNumber));
        return "customer/order-detail";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @RequestParam(defaultValue = "Cancelled by customer") String reason,
                              RedirectAttributes redirectAttrs) {
        try {
            orderService.cancelOrder(id, reason);
            redirectAttrs.addFlashAttribute("successMsg", "Order cancelled successfully.");
        } catch (BusinessException e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/customer/orders";
    }

    /* ─── Notifications ─── */
    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("notifications",
                notificationService.getRecentNotifications(principal.getId(), 50));
        notificationService.markAllRead(principal.getId());
        return "customer/notifications";
    }
}
