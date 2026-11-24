package com.pizzaflow.order.dto;

import java.math.BigDecimal;

public class PromoValidationResponse {
    private boolean valid;
    private String code;
    private String message;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    public PromoValidationResponse() {
    }

    public PromoValidationResponse(boolean valid, String code, String message, BigDecimal discountAmount, BigDecimal finalAmount) {
        this.valid = valid;
        this.code = code;
        this.message = message;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }
}
