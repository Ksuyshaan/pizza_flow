package com.pizzaflow.order.dto;

import com.pizzaflow.delivery.entity.PaymentMethod;
import com.pizzaflow.order.entity.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CreateOrderRequest {

    @NotNull(message = "Тип заказа обязателен (DELIVERY или PICKUP)")
    private OrderType orderType;

    @NotEmpty(message = "Заказ должен содержать хотя бы одну позицию")
    @Valid
    private List<OrderItemRequest> items;

    private Long addressId;
    private String customAddress;
    private Long deliveryZoneId;
    private String promoCode;

    @NotNull(message = "Способ оплаты обязателен (CASH, CARD, ONLINE)")
    private PaymentMethod paymentMethod;

    private String notes;

    public CreateOrderRequest() {
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public String getCustomAddress() {
        return customAddress;
    }

    public void setCustomAddress(String customAddress) {
        this.customAddress = customAddress;
    }

    public Long getDeliveryZoneId() {
        return deliveryZoneId;
    }

    public void setDeliveryZoneId(Long deliveryZoneId) {
        this.deliveryZoneId = deliveryZoneId;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
