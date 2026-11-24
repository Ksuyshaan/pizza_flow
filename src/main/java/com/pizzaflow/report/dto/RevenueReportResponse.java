package com.pizzaflow.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class RevenueReportResponse {
    private Instant periodStart;
    private Instant periodEnd;
    private long totalOrdersCount;
    private long completedOrdersCount;
    private long cancelledOrdersCount;
    private BigDecimal totalRevenue;
    private BigDecimal averageCheck;
    private BigDecimal totalDiscounts;
    private BigDecimal totalDeliveryFees;
    private List<CategorySalesDto> categoryBreakdown;

    public RevenueReportResponse() {
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(Instant periodStart) {
        this.periodStart = periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(Instant periodEnd) {
        this.periodEnd = periodEnd;
    }

    public long getTotalOrdersCount() {
        return totalOrdersCount;
    }

    public void setTotalOrdersCount(long totalOrdersCount) {
        this.totalOrdersCount = totalOrdersCount;
    }

    public long getCompletedOrdersCount() {
        return completedOrdersCount;
    }

    public void setCompletedOrdersCount(long completedOrdersCount) {
        this.completedOrdersCount = completedOrdersCount;
    }

    public long getCancelledOrdersCount() {
        return cancelledOrdersCount;
    }

    public void setCancelledOrdersCount(long cancelledOrdersCount) {
        this.cancelledOrdersCount = cancelledOrdersCount;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getAverageCheck() {
        return averageCheck;
    }

    public void setAverageCheck(BigDecimal averageCheck) {
        this.averageCheck = averageCheck;
    }

    public BigDecimal getTotalDiscounts() {
        return totalDiscounts;
    }

    public void setTotalDiscounts(BigDecimal totalDiscounts) {
        this.totalDiscounts = totalDiscounts;
    }

    public BigDecimal getTotalDeliveryFees() {
        return totalDeliveryFees;
    }

    public void setTotalDeliveryFees(BigDecimal totalDeliveryFees) {
        this.totalDeliveryFees = totalDeliveryFees;
    }

    public List<CategorySalesDto> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(List<CategorySalesDto> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }
}
