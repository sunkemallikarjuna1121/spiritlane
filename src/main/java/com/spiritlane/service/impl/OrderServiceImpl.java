package com.spiritlane.service.impl;

import com.razorpay.*;
import com.spiritlane.dto.OrderRequest;
import com.spiritlane.dto.PaymentVerificationDto;
import com.spiritlane.exception.BusinessException;
import com.spiritlane.exception.ResourceNotFoundException;
import com.spiritlane.entity.*;
import com.spiritlane.entity.Order;
import com.spiritlane.entity.Payment;
import com.spiritlane.repository.*;
import com.spiritlane.service.CartService;
import com.spiritlane.service.NotificationService;
import com.spiritlane.service.OrderService;
import com.spiritlane.util.AppUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
	
	private static final Logger log =
			LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final UserRepository userRepository;
    private final UserAddressRepository addressRepository;
    private final CouponRepository couponRepository;
    private final ShopInventoryRepository inventoryRepository;
    private final NotificationService notificationService;
    private final RazorpayClient razorpayClient;

    public OrderServiceImpl(OrderRepository orderRepository, OrderItemRepository orderItemRepository, PaymentRepository paymentRepository, CartRepository cartRepository, CartService cartService, UserRepository userRepository, UserAddressRepository addressRepository, CouponRepository couponRepository, ShopInventoryRepository inventoryRepository, NotificationService notificationService, RazorpayClient razorpayClient) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.cartRepository = cartRepository;
        this.cartService = cartService;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.couponRepository = couponRepository;
        this.inventoryRepository = inventoryRepository;
        this.notificationService = notificationService;
        this.razorpayClient = razorpayClient;
    }


    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${app.delivery.charge:40.00}")
    private BigDecimal deliveryCharge;

    @Value("${app.delivery.free-above:500.00}")
    private BigDecimal freeDeliveryAbove;

    @Value("${app.gst.rate:18.0}")
    private double gstRate;

    @Override
    public Order placeOrder(Long userId, OrderRequest request) {
        User customer = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Your cart is empty."));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessException("Your cart is empty. Please add products before placing an order.");
        }

        UserAddress address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", request.getAddressId()));

        // Calculate totals
        BigDecimal subtotal = cart.getSubtotal();
        BigDecimal delivery = subtotal.compareTo(freeDeliveryAbove) >= 0
                ? BigDecimal.ZERO : deliveryCharge;

        BigDecimal discount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            appliedCoupon = couponRepository.findByCodeIgnoreCase(request.getCouponCode())
                    .orElseThrow(() -> new BusinessException("Invalid coupon code."));
            if (!appliedCoupon.isValid()) throw new BusinessException("This coupon has expired or is no longer valid.");
            if (subtotal.compareTo(appliedCoupon.getMinOrderAmount()) < 0)
                throw new BusinessException("Minimum order amount for this coupon is ₹" + appliedCoupon.getMinOrderAmount());

            if (appliedCoupon.getDiscountType() == Coupon.DiscountType.PERCENTAGE) {
                discount = subtotal.multiply(appliedCoupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                if (appliedCoupon.getMaxDiscount() != null && discount.compareTo(appliedCoupon.getMaxDiscount()) > 0) {
                    discount = appliedCoupon.getMaxDiscount();
                }
            } else {
                discount = appliedCoupon.getDiscountValue();
            }
            appliedCoupon.setUsedCount(appliedCoupon.getUsedCount() + 1);
            couponRepository.save(appliedCoupon);
        }

        BigDecimal taxableAmount = subtotal.subtract(discount);
        BigDecimal tax = taxableAmount.multiply(BigDecimal.valueOf(gstRate / 100)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = taxableAmount.add(delivery).add(tax);

        // Build order
        Order order = new Order();

        order.setOrderNumber(AppUtils.generateOrderNumber());
        order.setCustomer(customer);
        order.setShop(cart.getShop());
        order.setDeliveryAddress(address);
        order.setSubtotal(subtotal);
        order.setDeliveryCharge(delivery);
        order.setDiscountAmount(discount);
        order.setTaxAmount(tax);
        order.setTotalAmount(total);
        order.setNotes(request.getNotes());
        order.setOrderStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setEstimatedDelivery(LocalDateTime.now().plusHours(2));

        Order savedOrder = orderRepository.save(order);

        // Save order items + reduce stock
        for (CartItem cartItem : cart.getItems()) {
            ShopInventory inventory = cartItem.getInventory();
            if (inventory.getStockQuantity() < cartItem.getQuantity()) {
                throw new BusinessException("Insufficient stock for: " + inventory.getProduct().getName());
            }
            inventory.setStockQuantity(inventory.getStockQuantity() - cartItem.getQuantity());
            inventoryRepository.save(inventory);

            OrderItem oi = new OrderItem();

            oi.setOrder(savedOrder);
            oi.setInventory(inventory);
            oi.setProductName(inventory.getProduct().getName());
            oi.setQuantity(cartItem.getQuantity());
            oi.setUnitPrice(cartItem.getUnitPrice());
            oi.setTotalPrice(cartItem.getTotalPrice());
            orderItemRepository.save(oi);
        }

        notificationService.sendNotification(userId,
                "Order Placed!", "Your order #" + savedOrder.getOrderNumber() + " has been placed successfully.",
                Notification.NotificationType.ORDER);

        log.info("Order placed: {} by user {}", savedOrder.getOrderNumber(), userId);
        return savedOrder;
    }

    @Override
    public Map<String, Object> createRazorpayOrder(Long orderId) throws Exception {
        Order order = findById(orderId);
        long amountInPaise = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();

        JSONObject opts = new JSONObject();
        opts.put("amount", amountInPaise);
        opts.put("currency", "INR");
        opts.put("receipt", order.getOrderNumber());

        com.razorpay.Order razorpayOrder = razorpayClient.orders.create(opts);

        Payment payment = new Payment();

        payment.setOrder(order);
        payment.setRazorpayOrderId(razorpayOrder.get("id").toString());
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(Payment.PaymentStatus.CREATED);
        paymentRepository.save(payment);

        Map<String, Object> result = new HashMap<>();
        result.put("razorpayOrderId", razorpayOrder.get("id"));
        result.put("amount", amountInPaise);
        result.put("currency", "INR");
        result.put("keyId", razorpayKeyId);
        result.put("orderId", orderId);
        result.put("orderNumber", order.getOrderNumber());
        return result;
    }

    @Override
    public Order verifyPayment(PaymentVerificationDto dto) {
        // Verify HMAC-SHA256 signature
        try {
            String data = dto.getRazorpayOrderId() + "|" + dto.getRazorpayPaymentId();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) hexString.append(String.format("%02x", b));
            String computedSignature = hexString.toString();

            if (!computedSignature.equals(dto.getRazorpaySignature())) {
                throw new BusinessException("Payment verification failed. Invalid signature.");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Payment verification error: " + e.getMessage());
        }

        Order order = findById(dto.getOrderId());
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found"));

        payment.setRazorpayPaymentId(dto.getRazorpayPaymentId());
        payment.setRazorpaySignature(dto.getRazorpaySignature());
        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setOrderStatus(Order.OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);
        cartService.clearCart(order.getCustomer().getId());

        notificationService.sendNotification(order.getCustomer().getId(),
                "Payment Successful!", "Payment received for order #" + order.getOrderNumber(),
                Notification.NotificationType.PAYMENT);
        // Notify shop owner
        notificationService.sendNotification(order.getShop().getOwner().getId(),
                "New Order Received!", "New order #" + order.getOrderNumber() + " needs preparation.",
                Notification.NotificationType.ORDER);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Order findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getCustomerOrders(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getShopOrders(Long shopId, Pageable pageable) {
        return orderRepository.findByShopIdOrderByCreatedAtDesc(shopId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getAgentOrders(Long agentId, Pageable pageable) {
        return orderRepository.findByDeliveryAgentIdOrderByCreatedAtDesc(agentId, pageable);
    }

    @Override
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = findById(orderId);
        order.setOrderStatus(status);
        if (status == Order.OrderStatus.DELIVERED) {
            order.setActualDelivery(LocalDateTime.now());
        }
        return orderRepository.save(order);
    }

    @Override
    public Order assignDeliveryAgent(Long orderId, Long agentId) {
        Order order = findById(orderId);
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery agent", agentId));
        order.setDeliveryAgent(agent);
        order.setOrderStatus(Order.OrderStatus.DISPATCHED);
        Order saved = orderRepository.save(order);
        notificationService.sendNotification(order.getCustomer().getId(),
                "Order Dispatched!", "Your order #" + order.getOrderNumber() + " is on its way!",
                Notification.NotificationType.ORDER);
        return saved;
    }

    @Override
    public void cancelOrder(Long orderId, String reason) {
        Order order = findById(orderId);
        if (order.getOrderStatus() == Order.OrderStatus.DELIVERED) {
            throw new BusinessException("Delivered orders cannot be cancelled.");
        }
        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledReason(reason);
        // Restore stock
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                ShopInventory inv = item.getInventory();
                inv.setStockQuantity(inv.getStockQuantity() + item.getQuantity());
                inventoryRepository.save(inv);
            }
        }
        orderRepository.save(order);
    }
}
