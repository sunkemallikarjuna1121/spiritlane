package com.spiritlane.service;

import com.spiritlane.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ShopService {
    Shop save(Shop shop);
    Shop findById(Long id);
    Shop findByOwnerId(Long ownerId);
    List<Shop> getPendingApprovals();
    void approveShop(Long shopId);
    void rejectShop(Long shopId);
    void toggleShopStatus(Long shopId);
    Page<Shop> getAllApprovedShops(Pageable pageable);
    List<Shop> searchShopsByPincode(String pincode);
    long countAll();
}
