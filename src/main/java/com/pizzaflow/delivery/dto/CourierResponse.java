package com.pizzaflow.delivery.dto;

import com.pizzaflow.delivery.entity.Courier;

public class CourierResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String vehicleType;
    private boolean available;
    private String availableSince;

    public static CourierResponse fromEntity(Courier c) {
        CourierResponse r = new CourierResponse();
        r.id = c.getId();
        r.userId = c.getUser().getId();
        r.fullName = c.getUser().getFullName();
        r.email = c.getUser().getEmail();
        r.vehicleType = c.getVehicleType() != null ? c.getVehicleType().name() : null;
        r.available = c.isAvailable();
        r.availableSince = c.getAvailableSince() != null ? c.getAvailableSince().toString() : null;
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public String getAvailableSince() { return availableSince; }
    public void setAvailableSince(String availableSince) { this.availableSince = availableSince; }
}
