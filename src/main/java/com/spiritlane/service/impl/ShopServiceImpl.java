package com.spiritlane.service.impl;

import com.spiritlane.exception.BusinessException;
import com.spiritlane.exception.ResourceNotFoundException;
import com.spiritlane.entity.Shop;
import com.spiritlane.repository.ShopRepository;
import com.spiritlane.service.ShopService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;

    public ShopServiceImpl(ShopRepository shopRepository) {
        this.shopRepository = shopRepository;
    }

    @Override
    public Shop save(Shop shop) {
        if (shop.getId() == null && shopRepository.existsByLicenseNumber(shop.getLicenseNumber())) {
            throw new BusinessException("A shop with this license number already exists.");
        }
        return shopRepository.save(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public Shop findById(Long id) {
        return shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Shop findByOwnerId(Long ownerId) {
        return shopRepository.findByOwnerId(ownerId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No shop found for owner: " + ownerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shop> getPendingApprovals() {
        return shopRepository.findPendingApproval();
    }

    @Override
    public void approveShop(Long shopId) {
        Shop shop = findById(shopId);
        shop.setIsApproved(true);
        shop.setIsActive(true);
        shopRepository.save(shop);
    }

    @Override
    public void rejectShop(Long shopId) {
        Shop shop = findById(shopId);
        shop.setIsApproved(false);
        shop.setIsActive(false);
        shopRepository.save(shop);
    }

    @Override
    public void toggleShopStatus(Long shopId) {
        Shop shop = findById(shopId);
        shop.setIsOpen(!shop.getIsOpen());
        shopRepository.save(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Shop> getAllApprovedShops(Pageable pageable) {
        return shopRepository.findByIsApprovedAndIsActive(true, true, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shop> searchShopsByPincode(String pincode) {
        return shopRepository.findByPincodeAndIsApprovedAndIsActive(pincode, true, true);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return shopRepository.count();
    }
}
