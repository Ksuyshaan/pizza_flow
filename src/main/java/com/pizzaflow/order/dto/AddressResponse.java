package com.pizzaflow.order.dto;

import com.pizzaflow.order.entity.Address;

public class AddressResponse {
    private Long id;
    private Long userId;
    private Long deliveryZoneId;
    private String deliveryZoneName;
    private String city;
    private String street;
    private String houseNumber;
    private String apartment;
    private String entrance;
    private String floor;
    private boolean isDefault;
    private String formatted;

    public AddressResponse() {
    }

    public static AddressResponse fromEntity(Address address) {
        AddressResponse res = new AddressResponse();
        res.setId(address.getId());
        res.setUserId(address.getUser().getId());
        if (address.getDeliveryZone() != null) {
            res.setDeliveryZoneId(address.getDeliveryZone().getId());
            res.setDeliveryZoneName(address.getDeliveryZone().getName());
        }
        res.setCity(address.getCity());
        res.setStreet(address.getStreet());
        res.setHouseNumber(address.getHouseNumber());
        res.setApartment(address.getApartment());
        res.setEntrance(address.getEntrance());
        res.setFloor(address.getFloor());
        res.setDefault(address.isDefault());
        res.setFormatted(address.toFormattedSnapshot());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDeliveryZoneId() {
        return deliveryZoneId;
    }

    public void setDeliveryZoneId(Long deliveryZoneId) {
        this.deliveryZoneId = deliveryZoneId;
    }

    public String getDeliveryZoneName() {
        return deliveryZoneName;
    }

    public void setDeliveryZoneName(String deliveryZoneName) {
        this.deliveryZoneName = deliveryZoneName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    public String getApartment() {
        return apartment;
    }

    public void setApartment(String apartment) {
        this.apartment = apartment;
    }

    public String getEntrance() {
        return entrance;
    }

    public void setEntrance(String entrance) {
        this.entrance = entrance;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public String getFormatted() {
        return formatted;
    }

    public void setFormatted(String formatted) {
        this.formatted = formatted;
    }
}
