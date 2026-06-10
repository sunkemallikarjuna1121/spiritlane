package com.spiritlane.service;

import com.spiritlane.dto.OrderRequest;
import com.spiritlane.dto.PaymentVerificationDto;
import com.spiritlane.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Map;

public interface OrderService {
    Order placeOrder(Long userId, OrderRequest request);
    Map<String, Object> createRazorpayOrder(Long orderId) throws Exception;
    Order verifyPayment(PaymentVerificationDto dto);
    Order findById(Long id);
    Order findByOrderNumber(String orderNumber);
    Page<Order> getCustomerOrders(Long customerId, Pageable pageable);
    Page<Order> getShopOrders(Long shopId, Pageable pageable);
    Page<Order> getAgentOrders(Long agentId, Pageable pageable);
    Order updateOrderStatus(Long orderId, Order.OrderStatus status);
    Order assignDeliveryAgent(Long orderId, Long agentId);
    void cancelOrder(Long orderId, String reason);
}
