package com.spiritlane.controller.delivery;

import com.spiritlane.entity.Order;
import com.spiritlane.security.CustomUserDetails;
import com.spiritlane.service.OrderService;
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

    private final OrderService orderService;

    public DeliveryController(OrderService orderService) {
        this.orderService = orderService;
    }

    /* ─── Dashboard ─── */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("user", principal.getUser());
        model.addAttribute("activeOrders",
                orderService.getAgentOrders(principal.getId(),
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))));
        return "delivery/dashboard";
    }

    /* ─── My Deliveries ─── */
    @GetMapping("/orders")
    public String myOrders(@AuthenticationPrincipal CustomUserDetails principal,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        model.addAttribute("orders",
                orderService.getAgentOrders(principal.getId(),
                        PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"))));
        return "delivery/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.findById(id));
        return "delivery/order-detail";
    }

    /* ─── Update Delivery Status ─── */
    @PostMapping("/orders/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam Order.OrderStatus status,
                               RedirectAttributes ra) {
        orderService.updateOrderStatus(id, status);
        ra.addFlashAttribute("successMsg", "Status updated to: " + status.name());
        return "redirect:/delivery/orders/" + id;
    }
}
