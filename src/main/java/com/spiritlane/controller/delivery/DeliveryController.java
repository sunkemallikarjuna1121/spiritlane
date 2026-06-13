package com.spiritlane.controller.delivery;

import com.spiritlane.config.MvcConfig;
import com.spiritlane.entity.Order;
import com.spiritlane.security.CustomUserDetails;
import com.spiritlane.service.OrderService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/delivery")
public class DeliveryController {
	
	private static final Logger logger =
	        LoggerFactory.getLogger(MvcConfig.class);
    private final OrderService orderService;

    public DeliveryController(OrderService orderService) {
        this.orderService = orderService;
    }

    /* ─── Dashboard ─── */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        logger.info("[DeliveryController] dashboard() : START");
        model.addAttribute("user", principal.getUser());
        model.addAttribute("activeOrders",
                orderService.getAgentOrders(principal.getId(),
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))));
        logger.info("[DeliveryController] dashboard() : END");
        return "delivery/dashboard";
    }

    /* ─── My Deliveries ─── */
    @GetMapping("/orders")
    public String myOrders(@AuthenticationPrincipal CustomUserDetails principal,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        logger.info("[DeliveryController] myOrders() : START");
        model.addAttribute("orders",
                orderService.getAgentOrders(principal.getId(),
                        PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"))));
        logger.info("[DeliveryController] myOrders() : END");
        return "delivery/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        logger.info("[DeliveryController] orderDetail() : START");
        model.addAttribute("order", orderService.findById(id));
        logger.info("[DeliveryController] orderDetail() : END");
        return "delivery/order-detail";
    }

    /* ─── Update Delivery Status ─── */
    @PostMapping("/orders/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam Order.OrderStatus status,
                               RedirectAttributes ra) {
        logger.info("[DeliveryController] updateStatus() : START");
        orderService.updateOrderStatus(id, status);
        ra.addFlashAttribute("successMsg", "Status updated to: " + status.name());
        logger.info("[DeliveryController] updateStatus() : END");
        return "redirect:/delivery/orders/" + id;
    }
}
