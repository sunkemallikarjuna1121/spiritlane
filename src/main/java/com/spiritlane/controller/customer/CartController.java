package com.spiritlane.controller.customer;

import com.spiritlane.exception.BusinessException;
import com.spiritlane.entity.Cart;
import com.spiritlane.security.CustomUserDetails;
import com.spiritlane.service.CartService;
import com.spiritlane.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    public CartController(CartService cartService, UserService userService) {
        this.cartService = cartService;
        this.userService = userService;
    }

    /* ─── View Cart ─── */
    @GetMapping
    public String viewCart(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Cart cart = cartService.getOrCreateCart(principal.getId());
        model.addAttribute("cart", cart);
        model.addAttribute("addresses", userService.getUserAddresses(principal.getId()));
        return "customer/cart";
    }

    /* ─── Add Item (AJAX + fallback) ─── */
    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addItem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam Long inventoryId,
            @RequestParam(defaultValue = "1") int quantity) {

        Map<String, Object> response = new HashMap<>();
        try {
            Cart cart = cartService.addItem(principal.getId(), inventoryId, quantity);
            response.put("success", true);
            response.put("cartCount", cart.getTotalItems());
            response.put("message", "Item added to cart successfully!");
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* ─── Update Quantity (AJAX) ─── */
    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateItem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam Long inventoryId,
            @RequestParam int quantity) {

        Map<String, Object> response = new HashMap<>();
        try {
            Cart cart = cartService.updateItem(principal.getId(), inventoryId, quantity);
            response.put("success", true);
            response.put("cartCount", cart.getTotalItems());
            response.put("subtotal",  cart.getSubtotal());
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* ─── Remove Item ─── */
    @PostMapping("/remove")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeItem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam Long inventoryId) {

        Map<String, Object> response = new HashMap<>();
        try {
            Cart cart = cartService.removeItem(principal.getId(), inventoryId);
            response.put("success", true);
            response.put("cartCount", cart.getTotalItems());
            response.put("subtotal",  cart.getSubtotal());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* ─── Clear Cart ─── */
    @PostMapping("/clear")
    public String clearCart(@AuthenticationPrincipal CustomUserDetails principal,
                            RedirectAttributes redirectAttrs) {
        cartService.clearCart(principal.getId());
        redirectAttrs.addFlashAttribute("successMsg", "Cart cleared.");
        return "redirect:/cart";
    }

    /* ─── Cart Count (AJAX for nav badge) ─── */
    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> getCount(
            @AuthenticationPrincipal CustomUserDetails principal) {
        Map<String, Integer> result = new HashMap<>();
        result.put("count", cartService.getCartItemCount(principal.getId()));
        return ResponseEntity.ok(result);
    }
}
