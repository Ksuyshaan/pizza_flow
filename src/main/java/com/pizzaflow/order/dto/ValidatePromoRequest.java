package com.pizzaflow.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ValidatePromoRequest {

    @NotBlank(message = "Код обязателен")
    private String code;

    @NotNull(message = "Сумма заказа обязательна")
    @DecimalMin(value = "0.0", message = "Сумма заказа не может быть отрицательной")
    private BigDecimal orderAmount;

    public ValidatePromoRequest() {
    }

    public ValidatePromoRequest(String code, BigDecimal orderAmount) {
        this.code = code;
        this.orderAmount = orderAmount;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }
}
