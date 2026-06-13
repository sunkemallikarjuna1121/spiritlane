package com.spiritlane.controller.customer;

import com.spiritlane.config.MvcConfig;
import com.spiritlane.entity.Banner;
import com.spiritlane.repository.BannerRepository;
import com.spiritlane.service.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class HomeController {

	private static final Logger logger =
	        LoggerFactory.getLogger(MvcConfig.class);
    private final ProductService productService;
    private final BannerRepository bannerRepository;

    public HomeController(ProductService productService,
                          BannerRepository bannerRepository) {
        this.productService = productService;
        this.bannerRepository = bannerRepository;
    }

    @GetMapping("/home")
    public String home(Model model) {
        logger.info("[HomeController] home : START");
        model.addAttribute("categories",   productService.getAllCategories());
        model.addAttribute("brands",       productService.getAllBrands());
        model.addAttribute("latestProducts", productService.browseProducts(null, null, null, PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "id"))));
        model.addAttribute("topBanners",
                bannerRepository.findByIsActiveTrueAndPositionOrderBySortOrderAsc(Banner.BannerPosition.HOME_TOP));
        model.addAttribute("midBanners",
                bannerRepository.findByIsActiveTrueAndPositionOrderBySortOrderAsc(Banner.BannerPosition.HOME_MID));

        logger.info("[HomeController] home : End");
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
        logger.info("[HomeController] browse : START");
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
        logger.info("[HomeController] browse : END");
        return "customer/browse";
    }

    @GetMapping("/products/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        logger.info("[HomeController] detail : START");
        model.addAttribute("inventory", productService.findInventoryById(id));
        logger.info("[HomeController] detail : END");
        return "customer/product-detail";
    }
}
