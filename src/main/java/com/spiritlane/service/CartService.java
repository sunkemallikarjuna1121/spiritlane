package com.spiritlane.service;

import com.spiritlane.entity.Cart;

public interface CartService {
    Cart getOrCreateCart(Long userId);
    Cart addItem(Long userId, Long inventoryId, int quantity);
    Cart updateItem(Long userId, Long inventoryId, int quantity);
    Cart removeItem(Long userId, Long inventoryId);
    void clearCart(Long userId);
    int getCartItemCount(Long userId);
}
