package com.spiritlane.repository;

import com.spiritlane.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndInventoryId(Long cartId, Long inventoryId);
    void deleteByCartId(Long cartId);
}
