package com.spiritlane.controller.shop;

import com.spiritlane.exception.BusinessException;
import com.spiritlane.controller.customer.AuthController;
import com.spiritlane.entity.*;
import com.spiritlane.security.CustomUserDetails;
import com.spiritlane.service.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/shop")
public class ShopController {

	private static final Logger logger =
	        LoggerFactory.getLogger(AuthController.class);
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
        logger.info("[ShopController] dashboard() : START");
        try {
            Shop shop = shopService.findByOwnerId(principal.getId());
            model.addAttribute("shop", shop);
            model.addAttribute("recentOrders",
                    orderService.getShopOrders(shop.getId(),
                            PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))));
            model.addAttribute("inventory", productService.getShopInventory(shop.getId()));
            logger.info("[ShopController] dashboard() : END");
            return "shop/dashboard";
        } catch (Exception e) {
            model.addAttribute("noShop", true);
            model.addAttribute("shop", new Shop());
            logger.info("[ShopController] dashboard() : END");
            return "shop/register-shop";
        }
    }

    /* ─── Register Shop ─── */
    @GetMapping("/register")
    public String registerShopForm(Model model) {
    	logger.info("[ShopController] registerShopForm() : START");
        model.addAttribute("shop", new Shop());
        logger.info("[ShopController] registerShopForm() : END");
        return "shop/register-shop";
    }

    @PostMapping("/register")
    public String registerShop(@AuthenticationPrincipal CustomUserDetails principal,
                               @ModelAttribute Shop shop,
                               RedirectAttributes ra) {
        logger.info("[ShopController] registerShop() : START");
        try {
            shop.setOwner(userService.findById(principal.getId()));
            shop.setIsApproved(false);
            shop.setIsActive(true);
            shopService.save(shop);
            ra.addFlashAttribute("successMsg",
                    "Shop registered! Awaiting admin approval.");
            logger.info("[ShopController] registerShop() : END");
            return "redirect:/shop/dashboard";
        } catch (BusinessException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            logger.info("[ShopController] registerShop() : END");
            return "redirect:/shop/register";
        }
    }

    /* ─── Inventory ─── */
    @GetMapping("/inventory")
    public String inventory(@AuthenticationPrincipal CustomUserDetails principal,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        logger.info("[ShopController] inventory() : START");
        Shop shop = shopService.findByOwnerId(principal.getId());
        model.addAttribute("shop", shop);
        model.addAttribute("inventory",
                productService.getShopInventory(shop.getId()));
        model.addAttribute("allProducts", productService.getLatestProducts(200));
        logger.info("[ShopController] inventory() : END");
        return "shop/inventory";
    }

    @PostMapping("/inventory/add")
    public String addInventory(@AuthenticationPrincipal CustomUserDetails principal,
                               @ModelAttribute ShopInventory inventory,
                               RedirectAttributes ra) {
        logger.info("[ShopController] addInventory() : START");
        try {
            Shop shop = shopService.findByOwnerId(principal.getId());
            if (inventory.getProduct() == null || inventory.getProduct().getId() == null) {
                throw new BusinessException("Please select a product.");
            }
            if (productService.existsInInventory(shop.getId(), inventory.getProduct().getId())) {
                throw new BusinessException("This product is already in your inventory. Update its stock instead.");
            }
            inventory.setShop(shop);
            productService.saveInventory(inventory);
            ra.addFlashAttribute("successMsg", "Product added to inventory.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        logger.info("[ShopController] addInventory() : END");
        return "redirect:/shop/inventory";
    }

    /**
     * JSON version of addInventory used by the "Add Product" modal so the
     * shop owner can add several products back-to-back without the page
     * reloading each time. Returns the newly created inventory row's data
     * so the client can append it to the table in place.
     */
    @PostMapping("/inventory/add-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addInventoryAjax(@AuthenticationPrincipal CustomUserDetails principal,
                                                                 @ModelAttribute ShopInventory inventory) {
        logger.info("[ShopController] addInventoryAjax() : START");
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            Shop shop = shopService.findByOwnerId(principal.getId());

            if (inventory.getProduct() == null || inventory.getProduct().getId() == null) {
                body.put("success", false);
                body.put("message", "Please select a product.");
                return ResponseEntity.badRequest().body(body);
            }

            Long productId = inventory.getProduct().getId();
            if (productService.existsInInventory(shop.getId(), productId)) {
                body.put("success", false);
                body.put("message", "This product is already in your inventory.");
                body.put("duplicate", true);
                body.put("productId", productId);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
            }

            inventory.setShop(shop);
            ShopInventory saved = productService.saveInventory(inventory);
            // Re-fetch with images so the product relation is fully populated for the response
            saved = productService.findInventoryById(saved.getId());
            Product product = saved.getProduct();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", saved.getId());
            data.put("productId", product.getId());
            data.put("productName", product.getName());
            data.put("brand", product.getBrand() != null ? product.getBrand().getName() : "");
            data.put("category", product.getCategory() != null ? product.getCategory().getName() : "");
            data.put("volumeMl", product.getVolumeMl());
            data.put("imageUrl", product.getPrimaryImageUrl());
            data.put("mrp", saved.getMrp());
            data.put("sellingPrice", saved.getSellingPrice());
            data.put("discountPct", saved.getDiscountPct());
            data.put("stockQuantity", saved.getStockQuantity());
            data.put("isAvailable", saved.getIsAvailable());

            body.put("success", true);
            body.put("message", "Product added to inventory.");
            body.put("inventory", data);
            logger.info("[ShopController] addInventoryAjax() : END");
            return ResponseEntity.ok(body);
        } catch (BusinessException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        } catch (Exception e) {
            logger.error("[ShopController] addInventoryAjax() error", e);
            body.put("success", false);
            body.put("message", "Could not add product. Please check the values and try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @PostMapping("/inventory/{id}/update-stock")
    public String updateStock(@PathVariable Long id,
                              @RequestParam int stock,
                              RedirectAttributes ra) {
        logger.info("[ShopController] updateStock() : START");
        ShopInventory inv = productService.findInventoryById(id);
        inv.setStockQuantity(stock);
        productService.saveInventory(inv);
        ra.addFlashAttribute("successMsg", "Stock updated.");
        logger.info("[ShopController] updateStock() : END");
        return "redirect:/shop/inventory";
    }

    @PostMapping("/inventory/{id}/toggle")
    public String toggleAvailability(@PathVariable Long id, RedirectAttributes ra) {
        logger.info("[ShopController] toggleAvailability() : START");
        ShopInventory inv = productService.findInventoryById(id);
        inv.setIsAvailable(!inv.getIsAvailable());
        productService.saveInventory(inv);
        ra.addFlashAttribute("successMsg", "Availability updated.");
        logger.info("[ShopController] toggleAvailability() : END");
        return "redirect:/shop/inventory";
    }

    /* ─── Orders ─── */
    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal CustomUserDetails principal,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        logger.info("[ShopController] orders() : START");
        Shop shop = shopService.findByOwnerId(principal.getId());
        model.addAttribute("shop", shop);
        model.addAttribute("orders",
                orderService.getShopOrders(shop.getId(),
                        PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"))));
        logger.info("[ShopController] orders() : END");
        return "shop/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        logger.info("[ShopController] orderDetail() : START");
        model.addAttribute("order", orderService.findById(id));
        logger.info("[ShopController] orderDetail() : END");
        return "shop/order-detail";
    }

    @PostMapping("/orders/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam Order.OrderStatus status,
                               RedirectAttributes ra) {
        logger.info("[ShopController] updateStatus() : START");
        orderService.updateOrderStatus(id, status);
        ra.addFlashAttribute("successMsg", "Order status updated to: " + status.name());
        logger.info("[ShopController] updateStatus() : END");
        return "redirect:/shop/orders/" + id;
    }

    /* ─── Toggle Shop Open/Close ─── */
    @PostMapping("/toggle-status")
    public String toggleStatus(@AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes ra) {
        logger.info("[ShopController] toggleStatus() : START");
        Shop shop = shopService.findByOwnerId(principal.getId());
        shopService.toggleShopStatus(shop.getId());
        ra.addFlashAttribute("successMsg", "Shop status updated.");
        logger.info("[ShopController] toggleStatus() : END");
        return "redirect:/shop/dashboard";
    }
}