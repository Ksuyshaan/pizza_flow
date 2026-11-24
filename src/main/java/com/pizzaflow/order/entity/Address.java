package com.pizzaflow.order.entity;

import com.pizzaflow.delivery.entity.DeliveryZone;
import jakarta.persistence.*;

@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "delivery_zone_id", nullable = false)
    private DeliveryZone deliveryZone;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String street;

    @Column(name = "house_number", nullable = false)
    private String houseNumber;

    private String apartment;
    private String entrance;
    private String floor;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    public Address() {
    }

    public Address(User user, DeliveryZone deliveryZone, String city, String street, String houseNumber, String apartment, String entrance, String floor, boolean isDefault) {
        this.user = user;
        this.deliveryZone = deliveryZone;
        this.city = city;
        this.street = street;
        this.houseNumber = houseNumber;
        this.apartment = apartment;
        this.entrance = entrance;
        this.floor = floor;
        this.isDefault = isDefault;
    }

    public String toFormattedSnapshot() {
        StringBuilder sb = new StringBuilder();
        sb.append(city).append(", ул. ").append(street).append(", д. ").append(houseNumber);
        if (apartment != null && !apartment.isBlank()) sb.append(", кв./оф. ").append(apartment);
        if (entrance != null && !entrance.isBlank()) sb.append(", под. ").append(entrance);
        if (floor != null && !floor.isBlank()) sb.append(", эт. ").append(floor);
        return sb.toString();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public DeliveryZone getDeliveryZone() {
        return deliveryZone;
    }

    public void setDeliveryZone(DeliveryZone deliveryZone) {
        this.deliveryZone = deliveryZone;
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
}
