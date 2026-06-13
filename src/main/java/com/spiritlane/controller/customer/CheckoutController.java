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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

	private static final Logger logger =
	        LoggerFactory.getLogger(CheckoutController.class);
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
        logger.info("[CheckoutController] checkoutPage() : START");
        Cart cart = cartService.getOrCreateCart(principal.getId());
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }
        boolean freeDelivery = cart.getSubtotal().compareTo(java.math.BigDecimal.valueOf(500)) >= 0;

        model.addAttribute("cart", cart);
        model.addAttribute("freeDelivery", freeDelivery);
        model.addAttribute("addresses", userService.getUserAddresses(principal.getId()));
        model.addAttribute("orderRequest", new OrderRequest());
        model.addAttribute("razorpayKey", razorpayKeyId);

        logger.info("[CheckoutController] checkoutPage() : END");

        return "customer/checkout";
    }

    /* ─── Place Order (AJAX) ─── */
    @PostMapping("/place-order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> placeOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody OrderRequest request) {

    	logger.info("[CheckoutController] ResponseEntity() : START");
        Map<String, Object> response = new HashMap<>();
        try {
            Order order = orderService.placeOrder(principal.getId(), request);
            Map<String, Object> paymentData = orderService.createRazorpayOrder(order.getId());
            paymentData.put("orderId", order.getId());
            response.put("success", true);
            response.put("payment", paymentData);
            logger.info("[CheckoutController] ResponseEntity() : END");
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            response.put("success", false);
            logger.info("[CheckoutController] ResponseEntity() : END");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Payment gateway error. Please try again.");
            logger.info("[CheckoutController] ResponseEntity() : END");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /* ─── Verify Payment (AJAX) ─── */
    @PostMapping("/verify-payment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @RequestBody PaymentVerificationDto dto) {

    	logger.info("[CheckoutController] ResponseEntity() : START");
        Map<String, Object> response = new HashMap<>();
        try {
            Order order = orderService.verifyPayment(dto);
            response.put("success",     true);
            response.put("orderNumber", order.getOrderNumber());
            response.put("message",     "Payment successful!");
            logger.info("[CheckoutController] ResponseEntity() : END");
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            logger.info("[CheckoutController] ResponseEntity() : END");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* ─── Payment Success Page ─── */
    @GetMapping("/success/{orderNumber}")
    public String success(@PathVariable String orderNumber, Model model) {
        logger.info("[CheckoutController] success() : START");
        model.addAttribute("order", orderService.findByOrderNumber(orderNumber));
        logger.info("[CheckoutController] success() : END");
        return "customer/payment-success";
    }

    /* ─── Payment Failure Page ─── */
    @GetMapping("/failed")
    public String failed() {
    	logger.info("[CheckoutController] failed() : START");
    	logger.info("[CheckoutController] failed() : END");
        return "customer/payment-failed";
    }
}
