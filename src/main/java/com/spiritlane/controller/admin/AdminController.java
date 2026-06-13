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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger =
        LoggerFactory.getLogger(AdminController.class);
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
        logger.info("[AdminController] dashboard() : START");
        model.addAttribute("totalCustomers",  userService.countByRole("ROLE_CUSTOMER"));
        model.addAttribute("totalShops",      shopService.countAll());
        model.addAttribute("totalAgents",     userService.countByRole("ROLE_DELIVERY_AGENT"));
        model.addAttribute("pendingOrders",   orderRepository.countByStatus(Order.OrderStatus.PENDING));
        model.addAttribute("pendingShops",    shopService.getPendingApprovals().size());
        model.addAttribute("recentOrders",
                orderService.getShopOrders(0L,
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))));
        logger.info("[AdminController] dashboard() : END");
        return "admin/dashboard";
    }

    /* ─── Customers ─── */
    @GetMapping("/customers")
    public String customers(@RequestParam(defaultValue = "0") int page, Model model) {
        logger.info("[AdminController] dashboard() : START page=[{}]",page);
        model.addAttribute("customers",
                userService.getAllCustomers(PageRequest.of(page, 15, Sort.by("createdAt").descending())));
        logger.info("[AdminController] dashboard() : END");
        return "admin/customers";
    }

    @PostMapping("/customers/{id}/block")
    public String blockCustomer(@PathVariable Long id, RedirectAttributes ra) {
        logger.info("[AdminController] blockCustomer() : START id=[{}]",id);
        userService.blockUser(id);
        ra.addFlashAttribute("successMsg", "Customer blocked.");
        logger.info("[AdminController] blockCustomer() : END");
        return "redirect:/admin/customers";
    }

    @PostMapping("/customers/{id}/unblock")
    public String unblockCustomer(@PathVariable Long id, RedirectAttributes ra) {
        logger.info("[AdminController] unblockCustomer() : START");
        userService.unblockUser(id);
        ra.addFlashAttribute("successMsg", "Customer unblocked.");
        logger.info("[AdminController] unblockCustomer() : END");
        return "redirect:/admin/customers";
    }

    /* ─── Shops ─── */
    @GetMapping("/shops")
    public String shops(@RequestParam(defaultValue = "0") int page, Model model) {
        logger.info("[AdminController] shops() : START");
        model.addAttribute("shops",        shopService.getAllApprovedShops(PageRequest.of(page, 15)));
        model.addAttribute("pendingShops", shopService.getPendingApprovals());
        logger.info("[AdminController] shops() : END");
        return "admin/shops";
    }

    @PostMapping("/shops/{id}/approve")
    public String approveShop(@PathVariable Long id, RedirectAttributes ra) {
        logger.info("[AdminController] approveShop() : START");
        shopService.approveShop(id);
        ra.addFlashAttribute("successMsg", "Shop approved successfully.");
        logger.info("[AdminController] approveShop() : END");
        return "redirect:/admin/shops";
    }

    @PostMapping("/shops/{id}/reject")
    public String rejectShop(@PathVariable Long id, RedirectAttributes ra) {
        logger.info("[AdminController] rejectShop() : START");
        shopService.rejectShop(id);
        ra.addFlashAttribute("errorMsg", "Shop rejected.");
        logger.info("[AdminController] rejectShop() : END");
        return "redirect:/admin/shops";
    }

    /* ─── Delivery Agents ─── */
    @GetMapping("/agents")
    public String agents(@RequestParam(defaultValue = "0") int page, Model model) {
        logger.info("[AdminController] agents() : START");
        model.addAttribute("agents",
                userService.getAllDeliveryAgents(PageRequest.of(page, 15)));
        logger.info("[AdminController] agents() : END");
        return "admin/agents";
    }

    @PostMapping("/agents/{id}/block")
    public String blockAgent(@PathVariable Long id, RedirectAttributes ra) {
        logger.info("[AdminController] blockAgent() : START");
        userService.blockUser(id);
        ra.addFlashAttribute("successMsg", "Agent blocked.");
        logger.info("[AdminController] blockAgent() : END");
        return "redirect:/admin/agents";
    }

    /* ─── Orders ─── */
    @GetMapping("/orders")
    public String orders(@RequestParam(defaultValue = "0") int page, Model model) {
        logger.info("[AdminController] orders() : START");
        model.addAttribute("orders",
                orderRepository.findAll(
                        PageRequest.of(page, 15, Sort.by(Sort.Direction.DESC, "createdAt"))));
        logger.info("[AdminController] orders() : END");
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        logger.info("[AdminController] orderDetail() : START");
        model.addAttribute("order", orderService.findById(id));
        logger.info("[AdminController] orderDetail() : END");
        return "admin/order-detail";
    }

    /* ─── Products ─── */
    @GetMapping("/products")
    public String products(@RequestParam(defaultValue = "0") int page, Model model) {
        logger.info("[AdminController] products() : START");
        model.addAttribute("products",
                productService.searchProducts(null, null, null, null,
                        PageRequest.of(page, 15, Sort.by(Sort.Direction.DESC, "createdAt"))));
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("brands",     productService.getAllBrands());
        logger.info("[AdminController] products() : END");
        return "admin/products";
    }

    @GetMapping("/products/add")
    public String addProductForm(Model model) {
        logger.info("[AdminController] addProductForm() : START");
        model.addAttribute("product",    new Product());
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("brands",     productService.getAllBrands());
        logger.info("[AdminController] addProductForm() : END");
        return "admin/product-form";
    }

    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute Product product, RedirectAttributes ra) {
        logger.info("[AdminController] saveProduct() : START");
        try {
            productService.save(product);
            ra.addFlashAttribute("successMsg", "Product saved successfully.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        logger.info("[AdminController] saveProduct() : END");
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes ra) {
        logger.info("[AdminController] deleteProduct() : START");
        productService.deleteProduct(id);
        ra.addFlashAttribute("successMsg", "Product deleted.");
        logger.info("[AdminController] deleteProduct() : END");
        return "redirect:/admin/products";
    }
}
