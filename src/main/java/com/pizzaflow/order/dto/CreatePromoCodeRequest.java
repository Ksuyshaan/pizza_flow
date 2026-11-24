package com.pizzaflow.order.dto;

import com.pizzaflow.order.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public class CreatePromoCodeRequest {

    @NotBlank(message = "Код обязателен")
    private String code;

    @NotNull(message = "Тип скидки обязателен")
    private DiscountType discountType;

    @NotNull(message = "Значение скидки обязательно")
    @DecimalMin(value = "0.01", message = "Значение скидки должно быть больше 0")
    private BigDecimal discountValue;

    @NotNull(message = "Минимальная сумма заказа обязательна")
    private BigDecimal minOrderAmount;

    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private Instant validFrom = Instant.now();
    private Instant validTo = Instant.now().plusSeconds(86400 * 30); // 30 days default

    public CreatePromoCodeRequest() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public BigDecimal getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public void setValidTo(Instant validTo) {
        this.validTo = validTo;
    }
}
