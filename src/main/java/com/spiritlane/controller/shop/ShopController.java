package com.spiritlane.controller.shop;

import com.spiritlane.exception.BusinessException;
import com.spiritlane.entity.*;
import com.spiritlane.security.CustomUserDetails;
import com.spiritlane.service.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/shop")
public class ShopController {

    private final ShopService shopService;
    private final ProductService productService;
    private final OrderService orderService;
    private final UserService userService;

    public ShopController(ShopService shopService,
                          ProductService productService,
                          OrderService orderService,
                          UserService userService) {
        this.shopService    = shopService;
        this.productService = productService;
        this.orderService   = orderService;
        this.userService    = userService;
    }

    /* ─── Dashboard ─── */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        try {
            Shop shop = shopService.findByOwnerId(principal.getId());
            model.addAttribute("shop", shop);
            model.addAttribute("recentOrders",
                    orderService.getShopOrders(shop.getId(),
                            PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))));
            model.addAttribute("inventory", productService.getShopInventory(shop.getId()));
            return "shop/dashboard";
        } catch (Exception e) {
            model.addAttribute("noShop", true);
            return "shop/register-shop";
        }
    }

    /* ─── Register Shop ─── */
    @GetMapping("/register")
    public String registerShopForm(Model model) {
        model.addAttribute("shop", new Shop());
        return "shop/register-shop";
    }

    @PostMapping("/register")
    public String registerShop(@AuthenticationPrincipal CustomUserDetails principal,
                               @ModelAttribute Shop shop,
                               RedirectAttributes ra) {
        try {
            shop.setOwner(userService.findById(principal.getId()));
            shop.setIsApproved(false);
            shop.setIsActive(true);
            shopService.save(shop);
            ra.addFlashAttribute("successMsg",
                    "Shop registered! Awaiting admin approval.");
            return "redirect:/shop/dashboard";
        } catch (BusinessException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/shop/register";
        }
    }

    /* ─── Inventory ─── */
    @GetMapping("/inventory")
    public String inventory(@AuthenticationPrincipal CustomUserDetails principal,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        Shop shop = shopService.findByOwnerId(principal.getId());
        model.addAttribute("shop", shop);
        model.addAttribute("inventory",
                productService.getShopInventory(shop.getId()));
        model.addAttribute("allProducts", productService.getLatestProducts(200));
        return "shop/inventory";
    }

    @PostMapping("/inventory/add")
    public String addInventory(@AuthenticationPrincipal CustomUserDetails principal,
                               @ModelAttribute ShopInventory inventory,
                               RedirectAttributes ra) {
        try {
            Shop shop = shopService.findByOwnerId(principal.getId());
            inventory.setShop(shop);
            productService.saveInventory(inventory);
            ra.addFlashAttribute("successMsg", "Product added to inventory.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/shop/inventory";
    }

    @PostMapping("/inventory/{id}/update-stock")
    public String updateStock(@PathVariable Long id,
                              @RequestParam int stock,
                              RedirectAttributes ra) {
        ShopInventory inv = productService.findInventoryById(id);
        inv.setStockQuantity(stock);
        productService.saveInventory(inv);
        ra.addFlashAttribute("successMsg", "Stock updated.");
        return "redirect:/shop/inventory";
    }

    @PostMapping("/inventory/{id}/toggle")
    public String toggleAvailability(@PathVariable Long id, RedirectAttributes ra) {
        ShopInventory inv = productService.findInventoryById(id);
        inv.setIsAvailable(!inv.getIsAvailable());
        productService.saveInventory(inv);
        ra.addFlashAttribute("successMsg", "Availability updated.");
        return "redirect:/shop/inventory";
    }

    /* ─── Orders ─── */
    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal CustomUserDetails principal,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        Shop shop = shopService.findByOwnerId(principal.getId());
        model.addAttribute("shop", shop);
        model.addAttribute("orders",
                orderService.getShopOrders(shop.getId(),
                        PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"))));
        return "shop/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.findById(id));
        return "shop/order-detail";
    }

    @PostMapping("/orders/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam Order.OrderStatus status,
                               RedirectAttributes ra) {
        orderService.updateOrderStatus(id, status);
        ra.addFlashAttribute("successMsg", "Order status updated to: " + status.name());
        return "redirect:/shop/orders/" + id;
    }

    /* ─── Toggle Shop Open/Close ─── */
    @PostMapping("/toggle-status")
    public String toggleStatus(@AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes ra) {
        Shop shop = shopService.findByOwnerId(principal.getId());
        shopService.toggleShopStatus(shop.getId());
        ra.addFlashAttribute("successMsg", "Shop status updated.");
        return "redirect:/shop/dashboard";
    }
}
