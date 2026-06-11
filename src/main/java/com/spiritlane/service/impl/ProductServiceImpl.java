package com.spiritlane.service.impl;

import com.spiritlane.exception.ResourceNotFoundException;
import com.spiritlane.entity.*;
import com.spiritlane.repository.*;
import com.spiritlane.service.ProductService;
import com.spiritlane.util.AppUtils;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ShopInventoryRepository inventoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository, BrandRepository brandRepository, ShopInventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.inventoryRepository = inventoryRepository;
    }


    @Override
    public Product save(Product product) {
        if (product.getSlug() == null || product.getSlug().isBlank()) {
            String base = AppUtils.generateSlug(product.getName());
            String slug = base;
            int i = 1;
            while (productRepository.existsBySlug(slug)) {
                slug = base + "-" + i++;
            }
            product.setSlug(slug);
        }
        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Product findBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String query, Long catId, Long brandId, String type, Pageable pageable) {
        if (query != null && !query.isBlank()) {
            return productRepository.searchProducts(query, pageable);
        }
        return productRepository.filterProducts(catId, brandId, type, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getLatestProducts(int limit) {
        return productRepository.findLatestProductsWithImages(PageRequest.of(0, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Brand> getAllBrands() {
        return brandRepository.findByIsActiveTrueOrderByNameAsc();
    }

    @Override
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    public ShopInventory saveInventory(ShopInventory inventory) {
        return inventoryRepository.save(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopInventory findInventoryById(Long id) {
        return inventoryRepository.findByIdWithImages(id)
        .orElseThrow(() -> new ResourceNotFoundException("Inventory item", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopInventory> getShopInventory(Long shopId) {
        return inventoryRepository.findByShopIdWithImages(shopId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopInventory> browseProducts(Long catId, Long brandId, String query, Pageable pageable) {
        String q = (query == null) ? "" : query.trim();
        
        // Get paginated IDs first
        Page<Long> idPage = inventoryRepository.browseProductIds(catId, brandId, q, pageable);
        
        if (idPage.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // Fetch full objects with images for those IDs
        List<ShopInventory> items = inventoryRepository.findByIdsWithImages(idPage.getContent());
        
        // Preserve the original sort order from idPage
        Map<Long, ShopInventory> itemMap = items.stream()
                .collect(java.util.stream.Collectors.toMap(ShopInventory::getId, i -> i));
        List<ShopInventory> sorted = idPage.getContent().stream()
                .map(itemMap::get)
                .filter(i -> i != null)
                .collect(java.util.stream.Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(sorted, pageable, idPage.getTotalElements());

    }
}
