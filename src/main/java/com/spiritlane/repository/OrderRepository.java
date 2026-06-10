package com.spiritlane.repository;

import com.spiritlane.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    Page<Order> findByShopIdOrderByCreatedAtDesc(Long shopId, Pageable pageable);

    Page<Order> findByDeliveryAgentIdOrderByCreatedAtDesc(Long agentId, Pageable pageable);

    List<Order> findByShopIdAndOrderStatusIn(Long shopId, List<Order.OrderStatus> statuses);

    List<Order> findByDeliveryAgentIdAndOrderStatus(Long agentId, Order.OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.shop.id = :shopId AND o.createdAt >= :from")
    long countByShopIdAndCreatedAtAfter(@Param("shopId") Long shopId, @Param("from") LocalDateTime from);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.shop.id = :shopId AND o.paymentStatus = 'PAID'")
    java.math.BigDecimal sumRevenueByShop(@Param("shopId") Long shopId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus = :status")
    long countByStatus(@Param("status") Order.OrderStatus status);
}
