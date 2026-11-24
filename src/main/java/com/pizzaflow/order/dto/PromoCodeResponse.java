package com.pizzaflow.order.dto;

import com.pizzaflow.order.entity.DiscountType;
import com.pizzaflow.order.entity.PromoCode;

import java.math.BigDecimal;
import java.time.Instant;

public class PromoCodeResponse {
    private Long id;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private Integer timesUsed;
    private Instant validFrom;
    private Instant validTo;
    private boolean active;

    public PromoCodeResponse() {
    }

    public static PromoCodeResponse fromEntity(PromoCode promo) {
        PromoCodeResponse res = new PromoCodeResponse();
        res.setId(promo.getId());
        res.setCode(promo.getCode());
        res.setDiscountType(promo.getDiscountType());
        res.setDiscountValue(promo.getDiscountValue());
        res.setMinOrderAmount(promo.getMinOrderAmount());
        res.setMaxDiscountAmount(promo.getMaxDiscountAmount());
        res.setUsageLimit(promo.getUsageLimit());
        res.setTimesUsed(promo.getTimesUsed());
        res.setValidFrom(promo.getValidFrom());
        res.setValidTo(promo.getValidTo());
        res.setActive(promo.isActive());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getTimesUsed() {
        return timesUsed;
    }

    public void setTimesUsed(Integer timesUsed) {
        this.timesUsed = timesUsed;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
