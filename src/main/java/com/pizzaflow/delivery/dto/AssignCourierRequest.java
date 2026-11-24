package com.pizzaflow.delivery.dto;

import jakarta.validation.constraints.NotNull;

public class AssignCourierRequest {

    @NotNull(message = "ID курьера обязателен")
    private Long courierId;

    public AssignCourierRequest() {
    }

    public AssignCourierRequest(Long courierId) {
        this.courierId = courierId;
    }

    public Long getCourierId() {
        return courierId;
    }

    public void setCourierId(Long courierId) {
        this.courierId = courierId;
    }
}
