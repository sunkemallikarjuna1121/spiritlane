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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Controller
@RequestMapping("/customer")
public class CustomerController {

	private static final Logger logger =
	        LoggerFactory.getLogger(AuthController.class);
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
        logger.info("[CustomerController] customerHome() : START");
        model.addAttribute("user", principal.getUser());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(principal.getId()));
        logger.info("[CustomerController] customerHome() : END");
        return "redirect:/home";
    }

    /* ─── Profile ─── */
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
    	logger.info("[CustomerController] profile() : START");
        model.addAttribute("user",      principal.getUser());
        model.addAttribute("addresses", userService.getUserAddresses(principal.getId()));
        logger.info("[CustomerController] profile() : END");
        return "customer/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails principal,
                                @RequestParam String fullName,
                                @RequestParam String phone,
                                RedirectAttributes redirectAttrs) {
    	logger.info("[CustomerController] updateProfile() : START");
        try {
            userService.updateProfile(principal.getId(), fullName, phone);
            logger.info("[CustomerController] updateProfile() : END");
            redirectAttrs.addFlashAttribute("successMsg", "Profile updated successfully.");
        } catch (BusinessException e) {
        	logger.info("[CustomerController] updateProfile() : END");
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        logger.info("[CustomerController] updateProfile() : END");
        return "redirect:/customer/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@AuthenticationPrincipal CustomUserDetails principal,
                                 @RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttrs) {
        logger.info("[CustomerController] changePassword() : START");
        if (!newPassword.equals(confirmPassword)) {
            redirectAttrs.addFlashAttribute("errorMsg", "New passwords do not match.");
            logger.info("[CustomerController] changePassword() : END");
            return "redirect:/customer/profile";
        }
        try {
            userService.changePassword(principal.getId(), oldPassword, newPassword);
            logger.info("[CustomerController] changePassword() : END");
            redirectAttrs.addFlashAttribute("successMsg", "Password changed successfully.");
        } catch (BusinessException e) {
            logger.info("[CustomerController] changePassword() : END");
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        logger.info("[CustomerController] changePassword() : END");
        return "redirect:/customer/profile";
    }

    /* ─── Addresses ─── */
    @GetMapping("/addresses")
    public String addresses(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        logger.info("[CustomerController] addresses() : START");
        model.addAttribute("addresses", userService.getUserAddresses(principal.getId()));
        model.addAttribute("addressDto", new AddressDto());
        logger.info("[CustomerController] addresses() : END");
        return "customer/addresses";
    }

    @PostMapping("/addresses/add")
    public String addAddress(@AuthenticationPrincipal CustomUserDetails principal,
                             @Valid @ModelAttribute("addressDto") AddressDto dto,
                             BindingResult result,
                             RedirectAttributes redirectAttrs,
                             Model model) {
        logger.info("[CustomerController] addAddress() : START");
        if (result.hasErrors()) {
            model.addAttribute("addresses", userService.getUserAddresses(principal.getId()));
            logger.info("[CustomerController] addAddress() : END");
            return "customer/addresses";
        }
        try {
            userService.addAddress(principal.getId(), dto);
            logger.info("[CustomerController] addAddress() : END");
            redirectAttrs.addFlashAttribute("successMsg", "Address added successfully.");
        } catch (BusinessException e) {
            logger.info("[CustomerController] addAddress() : END");
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        logger.info("[CustomerController] addAddress() : END");
        return "redirect:/customer/addresses";
    }

    @PostMapping("/addresses/{id}/delete")
    public String deleteAddress(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        logger.info("[CustomerController] deleteAddress() : START");
        userService.deleteAddress(id);
        redirectAttrs.addFlashAttribute("successMsg", "Address removed.");
        logger.info("[CustomerController] deleteAddress() : END");
        return "redirect:/customer/addresses";
    }

    @PostMapping("/addresses/{id}/set-default")
    public String setDefault(@AuthenticationPrincipal CustomUserDetails principal,
                             @PathVariable Long id, RedirectAttributes redirectAttrs) {
        logger.info("[CustomerController] setDefault() : START");
        userService.setDefaultAddress(principal.getId(), id);
        redirectAttrs.addFlashAttribute("successMsg", "Default address updated.");
        logger.info("[CustomerController] setDefault() : END");
        return "redirect:/customer/addresses";
    }

    /* ─── Orders ─── */
    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal CustomUserDetails principal,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        logger.info("[CustomerController] orders() : START");
        PageRequest pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("orders", orderService.getCustomerOrders(principal.getId(), pageable));
        logger.info("[CustomerController] orders() : END");
        return "customer/orders";
    }

    @GetMapping("/orders/{orderNumber}")
    public String orderDetail(@PathVariable String orderNumber, Model model) {
        logger.info("[CustomerController] orderDetail() : START");
        model.addAttribute("order", orderService.findByOrderNumber(orderNumber));
        logger.info("[CustomerController] orderDetail() : END");
        return "customer/order-detail";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @RequestParam(defaultValue = "Cancelled by customer") String reason,
                              RedirectAttributes redirectAttrs) {
        logger.info("[CustomerController] cancelOrder() : START");
        try {
            orderService.cancelOrder(id, reason);
            redirectAttrs.addFlashAttribute("successMsg", "Order cancelled successfully.");
        } catch (BusinessException e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        logger.info("[CustomerController] cancelOrder() : END");
        return "redirect:/customer/orders";
    }

    /* ─── Notifications ─── */
    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        logger.info("[CustomerController] notifications() : START");
        model.addAttribute("notifications",
                notificationService.getRecentNotifications(principal.getId(), 50));
        notificationService.markAllRead(principal.getId());
        logger.info("[CustomerController] notifications() : END");
        return "customer/notifications";
    }
}
