package com.pizzaflow.order.dto;

import com.pizzaflow.order.entity.Order;
import com.pizzaflow.order.entity.OrderStatus;
import com.pizzaflow.order.entity.OrderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class OrderResponse {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private OrderType orderType;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    private String promoCode;
    private String deliveryAddress;
    private Long deliveryZoneId;
    private String deliveryZoneName;
    private Instant createdAt;
    private Instant confirmedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private String cancellationReason;
    private List<OrderItemResponse> items;
    private List<StatusHistoryResponse> history;

    public OrderResponse() {
    }

    public static OrderResponse fromEntity(Order order) {
        OrderResponse res = new OrderResponse();
        res.setId(order.getId());
        res.setOrderNumber(order.getOrderNumber());
        if (order.getUser() != null) {
            res.setUserId(order.getUser().getId());
            res.setUserEmail(order.getUser().getEmail());
            res.setUserFullName(order.getUser().getFullName());
        }
        res.setOrderType(order.getOrderType());
        res.setStatus(order.getStatus());
        res.setSubtotal(order.getSubtotal());
        res.setDiscountAmount(order.getDiscountAmount());
        res.setDeliveryFee(order.getDeliveryFee());
        res.setTotalAmount(order.getTotalAmount());
        res.setPromoCode(order.getPromoCodeSnapshot());
        res.setDeliveryAddress(order.getDeliveryAddressSnapshot());
        if (order.getDeliveryZone() != null) {
            res.setDeliveryZoneId(order.getDeliveryZone().getId());
            res.setDeliveryZoneName(order.getDeliveryZone().getName());
        }
        res.setCreatedAt(order.getCreatedAt());
        res.setConfirmedAt(order.getConfirmedAt());
        res.setCompletedAt(order.getCompletedAt());
        res.setCancelledAt(order.getCancelledAt());
        res.setCancellationReason(order.getCancellationReason());

        if (order.getItems() != null) {
            res.setItems(order.getItems().stream()
                    .map(OrderItemResponse::fromEntity)
                    .collect(Collectors.toList()));
        }

        if (order.getStatusHistories() != null) {
            res.setHistory(order.getStatusHistories().stream()
                    .map(StatusHistoryResponse::fromEntity)
                    .collect(Collectors.toList()));
        }

        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public Long getDeliveryZoneId() {
        return deliveryZoneId;
    }

    public void setDeliveryZoneId(Long deliveryZoneId) {
        this.deliveryZoneId = deliveryZoneId;
    }

    public String getDeliveryZoneName() {
        return deliveryZoneName;
    }

    public void setDeliveryZoneName(String deliveryZoneName) {
        this.deliveryZoneName = deliveryZoneName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
    }

    public List<StatusHistoryResponse> getHistory() {
        return history;
    }

    public void setHistory(List<StatusHistoryResponse> history) {
        this.history = history;
    }
}
