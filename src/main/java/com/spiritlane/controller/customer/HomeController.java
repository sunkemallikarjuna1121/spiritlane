package com.spiritlane.controller.customer;

import com.spiritlane.entity.Banner;
import com.spiritlane.repository.BannerRepository;
import com.spiritlane.service.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class HomeController {

    private final ProductService productService;
    private final BannerRepository bannerRepository;

    public HomeController(ProductService productService,
                          BannerRepository bannerRepository) {
        this.productService = productService;
        this.bannerRepository = bannerRepository;
    }

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("categories",   productService.getAllCategories());
        model.addAttribute("brands",       productService.getAllBrands());
        model.addAttribute("latestProducts", productService.getLatestProducts(12));
        model.addAttribute("topBanners",
                bannerRepository.findByIsActiveTrueAndPositionOrderBySortOrderAsc(Banner.BannerPosition.HOME_TOP));
        model.addAttribute("midBanners",
                bannerRepository.findByIsActiveTrueAndPositionOrderBySortOrderAsc(Banner.BannerPosition.HOME_MID));
        return "customer/home";
    }

    @GetMapping("/products/browse")
    public String browse(@RequestParam(required = false) Long category,
                         @RequestParam(required = false) Long brand,
                         @RequestParam(required = false) String q,
                         @RequestParam(defaultValue = "0")  int page,
                         @RequestParam(defaultValue = "12") int size,
                         @RequestParam(defaultValue = "sellingPrice") String sortBy,
                         @RequestParam(defaultValue = "asc")          String sortDir,
                         Model model) {
        Sort.Direction dir = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        model.addAttribute("products",   productService.browseProducts(category, brand, q, pageable));
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("brands",     productService.getAllBrands());
        model.addAttribute("selectedCat",   category);
        model.addAttribute("selectedBrand", brand);
        model.addAttribute("query",         q);
        model.addAttribute("sortBy",        sortBy);
        model.addAttribute("sortDir",       sortDir);
        return "customer/browse";
    }

    @GetMapping("/products/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("inventory", productService.findInventoryById(id));
        return "customer/product-detail";
    }
}
