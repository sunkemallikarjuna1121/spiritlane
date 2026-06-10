package com.spiritlane.controller.admin;

import com.spiritlane.exception.BusinessException;
import com.spiritlane.entity.*;
import com.spiritlane.repository.*;
import com.spiritlane.service.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final ShopService shopService;
    private final OrderService orderService;
    private final ProductService productService;
    private final OrderRepository orderRepository;

    public AdminController(UserService userService,
                           ShopService shopService,
                           OrderService orderService,
                           ProductService productService,
                           OrderRepository orderRepository) {
        this.userService     = userService;
        this.shopService     = shopService;
        this.orderService    = orderService;
        this.productService  = productService;
        this.orderRepository = orderRepository;
    }

    /* ─── Dashboard ─── */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalCustomers",  userService.countByRole("ROLE_CUSTOMER"));
        model.addAttribute("totalShops",      shopService.countAll());
        model.addAttribute("totalAgents",     userService.countByRole("ROLE_DELIVERY_AGENT"));
        model.addAttribute("pendingOrders",   orderRepository.countByStatus(Order.OrderStatus.PENDING));
        model.addAttribute("pendingShops",    shopService.getPendingApprovals().size());
        model.addAttribute("recentOrders",
                orderService.getShopOrders(0L,
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))));
        return "admin/dashboard";
    }

    /* ─── Customers ─── */
    @GetMapping("/customers")
    public String customers(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("customers",
                userService.getAllCustomers(PageRequest.of(page, 15, Sort.by("createdAt").descending())));
        return "admin/customers";
    }

    @PostMapping("/customers/{id}/block")
    public String blockCustomer(@PathVariable Long id, RedirectAttributes ra) {
        userService.blockUser(id);
        ra.addFlashAttribute("successMsg", "Customer blocked.");
        return "redirect:/admin/customers";
    }

    @PostMapping("/customers/{id}/unblock")
    public String unblockCustomer(@PathVariable Long id, RedirectAttributes ra) {
        userService.unblockUser(id);
        ra.addFlashAttribute("successMsg", "Customer unblocked.");
        return "redirect:/admin/customers";
    }

    /* ─── Shops ─── */
    @GetMapping("/shops")
    public String shops(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("shops",        shopService.getAllApprovedShops(PageRequest.of(page, 15)));
        model.addAttribute("pendingShops", shopService.getPendingApprovals());
        return "admin/shops";
    }

    @PostMapping("/shops/{id}/approve")
    public String approveShop(@PathVariable Long id, RedirectAttributes ra) {
        shopService.approveShop(id);
        ra.addFlashAttribute("successMsg", "Shop approved successfully.");
        return "redirect:/admin/shops";
    }

    @PostMapping("/shops/{id}/reject")
    public String rejectShop(@PathVariable Long id, RedirectAttributes ra) {
        shopService.rejectShop(id);
        ra.addFlashAttribute("errorMsg", "Shop rejected.");
        return "redirect:/admin/shops";
    }

    /* ─── Delivery Agents ─── */
    @GetMapping("/agents")
    public String agents(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("agents",
                userService.getAllDeliveryAgents(PageRequest.of(page, 15)));
        return "admin/agents";
    }

    @PostMapping("/agents/{id}/block")
    public String blockAgent(@PathVariable Long id, RedirectAttributes ra) {
        userService.blockUser(id);
        ra.addFlashAttribute("successMsg", "Agent blocked.");
        return "redirect:/admin/agents";
    }

    /* ─── Orders ─── */
    @GetMapping("/orders")
    public String orders(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("orders",
                orderRepository.findAll(
                        PageRequest.of(page, 15, Sort.by(Sort.Direction.DESC, "createdAt"))));
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.findById(id));
        return "admin/order-detail";
    }

    /* ─── Products ─── */
    @GetMapping("/products")
    public String products(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("products",
                productService.searchProducts(null, null, null, null,
                        PageRequest.of(page, 15, Sort.by(Sort.Direction.DESC, "createdAt"))));
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("brands",     productService.getAllBrands());
        return "admin/products";
    }

    @GetMapping("/products/add")
    public String addProductForm(Model model) {
        model.addAttribute("product",    new Product());
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("brands",     productService.getAllBrands());
        return "admin/product-form";
    }

    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute Product product, RedirectAttributes ra) {
        try {
            productService.save(product);
            ra.addFlashAttribute("successMsg", "Product saved successfully.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes ra) {
        productService.deleteProduct(id);
        ra.addFlashAttribute("successMsg", "Product deleted.");
        return "redirect:/admin/products";
    }
}
