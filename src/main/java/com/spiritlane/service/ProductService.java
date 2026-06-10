package com.spiritlane.service;

import com.spiritlane.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ProductService {
    Product save(Product product);
    Product findById(Long id);
    Product findBySlug(String slug);
    Page<Product> searchProducts(String query, Long catId, Long brandId, String type, Pageable pageable);
    List<Product> getLatestProducts(int limit);
    List<Category> getAllCategories();
    List<Brand> getAllBrands();
    void deleteProduct(Long id);

    // Inventory
    ShopInventory saveInventory(ShopInventory inventory);
    ShopInventory findInventoryById(Long id);
    List<ShopInventory> getShopInventory(Long shopId);
    Page<ShopInventory> browseProducts(Long catId, Long brandId, String query, Pageable pageable);
}
