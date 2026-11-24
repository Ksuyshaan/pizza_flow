package com.pizzaflow.delivery.dto;

import com.pizzaflow.delivery.entity.Delivery;

public class DeliveryResponse {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private String status;
    private Long courierId;
    private String courierName;
    private String assignedAt;
    private String pickedUpAt;
    private String deliveredAt;
    private String deliveryNotes;

    public static DeliveryResponse fromEntity(Delivery d) {
        DeliveryResponse r = new DeliveryResponse();
        r.id = d.getId();
        r.orderId = d.getOrder().getId();
        r.orderNumber = d.getOrder().getOrderNumber();
        r.status = d.getStatus().name();
        if (d.getCourier() != null) {
            r.courierId = d.getCourier().getId();
            r.courierName = d.getCourier().getUser().getFullName();
        }
        r.assignedAt = d.getAssignedAt() != null ? d.getAssignedAt().toString() : null;
        r.pickedUpAt = d.getPickedUpAt() != null ? d.getPickedUpAt().toString() : null;
        r.deliveredAt = d.getDeliveredAt() != null ? d.getDeliveredAt().toString() : null;
        r.deliveryNotes = d.getDeliveryNotes();
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCourierId() { return courierId; }
    public void setCourierId(Long courierId) { this.courierId = courierId; }
    public String getCourierName() { return courierName; }
    public void setCourierName(String courierName) { this.courierName = courierName; }
    public String getAssignedAt() { return assignedAt; }
    public void setAssignedAt(String assignedAt) { this.assignedAt = assignedAt; }
    public String getPickedUpAt() { return pickedUpAt; }
    public void setPickedUpAt(String pickedUpAt) { this.pickedUpAt = pickedUpAt; }
    public String getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(String deliveredAt) { this.deliveredAt = deliveredAt; }
    public String getDeliveryNotes() { return deliveryNotes; }
    public void setDeliveryNotes(String deliveryNotes) { this.deliveryNotes = deliveryNotes; }
}
