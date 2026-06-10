package com.spiritlane.controller.customer;

import com.spiritlane.dto.OrderRequest;
import com.spiritlane.dto.PaymentVerificationDto;
import com.spiritlane.exception.BusinessException;
import com.spiritlane.entity.Cart;
import com.spiritlane.entity.Order;
import com.spiritlane.security.CustomUserDetails;
import com.spiritlane.service.CartService;
import com.spiritlane.service.OrderService;
import com.spiritlane.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserService userService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    public CheckoutController(CartService cartService,
                               OrderService orderService,
                               UserService userService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.userService = userService;
    }

    /* ─── Checkout Page ─── */
    @GetMapping
    public String checkoutPage(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Cart cart = cartService.getOrCreateCart(principal.getId());
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }
        model.addAttribute("cart",         cart);
        model.addAttribute("addresses",    userService.getUserAddresses(principal.getId()));
        model.addAttribute("orderRequest", new OrderRequest());
        model.addAttribute("razorpayKey",  razorpayKeyId);
        return "customer/checkout";
    }

    /* ─── Place Order (AJAX) ─── */
    @PostMapping("/place-order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> placeOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody OrderRequest request) {

        Map<String, Object> response = new HashMap<>();
        try {
            Order order = orderService.placeOrder(principal.getId(), request);
            Map<String, Object> paymentData = orderService.createRazorpayOrder(order.getId());
            paymentData.put("orderId", order.getId());
            response.put("success", true);
            response.put("payment", paymentData);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Payment gateway error. Please try again.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /* ─── Verify Payment (AJAX) ─── */
    @PostMapping("/verify-payment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @RequestBody PaymentVerificationDto dto) {

        Map<String, Object> response = new HashMap<>();
        try {
            Order order = orderService.verifyPayment(dto);
            response.put("success",     true);
            response.put("orderNumber", order.getOrderNumber());
            response.put("message",     "Payment successful!");
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* ─── Payment Success Page ─── */
    @GetMapping("/success/{orderNumber}")
    public String success(@PathVariable String orderNumber, Model model) {
        model.addAttribute("order", orderService.findByOrderNumber(orderNumber));
        return "customer/payment-success";
    }

    /* ─── Payment Failure Page ─── */
    @GetMapping("/failed")
    public String failed() {
        return "customer/payment-failed";
    }
}
