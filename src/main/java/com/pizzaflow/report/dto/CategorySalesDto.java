package com.pizzaflow.report.dto;

import java.math.BigDecimal;

public class CategorySalesDto {
    private String categoryName;
    private long itemsSold;
    private BigDecimal revenue;
    private BigDecimal percentageShare;

    public CategorySalesDto() {
    }

    public CategorySalesDto(String categoryName, long itemsSold, BigDecimal revenue, BigDecimal percentageShare) {
        this.categoryName = categoryName;
        this.itemsSold = itemsSold;
        this.revenue = revenue;
        this.percentageShare = percentageShare;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public long getItemsSold() {
        return itemsSold;
    }

    public void setItemsSold(long itemsSold) {
        this.itemsSold = itemsSold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getPercentageShare() {
        return percentageShare;
    }

    public void setPercentageShare(BigDecimal percentageShare) {
        this.percentageShare = percentageShare;
    }
}
