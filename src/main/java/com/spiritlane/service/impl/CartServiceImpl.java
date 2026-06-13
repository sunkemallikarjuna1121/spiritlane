package com.spiritlane.service.impl;

import com.spiritlane.exception.BusinessException;
import com.spiritlane.exception.ResourceNotFoundException;
import com.spiritlane.entity.*;
import com.spiritlane.repository.*;
import com.spiritlane.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Hibernate;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ShopInventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    public CartServiceImpl(CartRepository cartRepository, CartItemRepository cartItemRepository, ShopInventoryRepository inventoryRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.userRepository = userRepository;
    }


    @Override
    @Transactional
    public Cart getOrCreateCart(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));

            Cart newCart = new Cart();
            newCart.setUser(user);

            return cartRepository.save(newCart);
        });

        // Force load lazy collections
        Hibernate.initialize(cart.getItems());
        cart.getItems().forEach(item -> {
            Hibernate.initialize(item.getInventory());
            Hibernate.initialize(item.getInventory().getProduct());
            Hibernate.initialize(item.getInventory().getProduct().getBrand());
        });

        return cart;
    }

    @Override
    public Cart addItem(Long userId, Long inventoryId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        ShopInventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not available"));

        if (!inventory.isInStock()) {
            throw new BusinessException("Sorry, this product is currently out of stock.");
        }
        if (quantity < inventory.getMinOrderQty() || quantity > inventory.getMaxOrderQty()) {
            throw new BusinessException("Quantity must be between " + inventory.getMinOrderQty()
                    + " and " + inventory.getMaxOrderQty());
        }

        // Enforce single-shop cart
        if (cart.getShop() != null && !cart.getShop().getId().equals(inventory.getShop().getId())) {
            throw new BusinessException("Your cart already has items from another shop. " +
                    "Please clear your cart before adding items from a different shop.");
        }

        cart.setShop(inventory.getShop());

        // Check if item already in cart
        cartItemRepository.findByCartIdAndInventoryId(cart.getId(), inventoryId)
                .ifPresentOrElse(
                        existing -> {
                            int newQty = existing.getQuantity() + quantity;
                            if (newQty > inventory.getMaxOrderQty()) newQty = inventory.getMaxOrderQty();
                            existing.setQuantity(newQty);
                            cartItemRepository.save(existing);
                        },
                        () -> {
                        	CartItem item = new CartItem();

                        	item.setCart(cart);
                        	item.setInventory(inventory);
                        	item.setQuantity(quantity);
                        	item.setUnitPrice(inventory.getSellingPrice());

                        	cart.getItems().add(item);
                        }
                );

        return cartRepository.save(cart);
    }

    @Override
    public Cart updateItem(Long userId, Long inventoryId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByCartIdAndInventoryId(cart.getId(), inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        if (quantity <= 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
        if (cart.getItems().isEmpty()) cart.setShop(null);
        return cartRepository.save(cart);
    }

    @Override
    public Cart removeItem(Long userId, Long inventoryId) {
        return updateItem(userId, inventoryId, 0);
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        cart.setShop(null);
        cartRepository.save(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public int getCartItemCount(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(Cart::getTotalItems)
                .orElse(0);
    }
}
