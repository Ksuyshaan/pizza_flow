package com.pizzaflow.kitchen.dto;

public class KitchenLoadResponse {
    private long pendingOrders;
    private long inProgressOrders;
    private long totalActiveOrders;
    private int estimatedWaitMinutes;
    private String loadLevel; // LOW, MEDIUM, HIGH

    public KitchenLoadResponse() {
    }

    public KitchenLoadResponse(long pendingOrders, long inProgressOrders, int estimatedWaitMinutes, String loadLevel) {
        this.pendingOrders = pendingOrders;
        this.inProgressOrders = inProgressOrders;
        this.totalActiveOrders = pendingOrders + inProgressOrders;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
        this.loadLevel = loadLevel;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public long getInProgressOrders() {
        return inProgressOrders;
    }

    public void setInProgressOrders(long inProgressOrders) {
        this.inProgressOrders = inProgressOrders;
    }

    public long getTotalActiveOrders() {
        return totalActiveOrders;
    }

    public void setTotalActiveOrders(long totalActiveOrders) {
        this.totalActiveOrders = totalActiveOrders;
    }

    public int getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public void setEstimatedWaitMinutes(int estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public String getLoadLevel() {
        return loadLevel;
    }

    public void setLoadLevel(String loadLevel) {
        this.loadLevel = loadLevel;
    }
}
