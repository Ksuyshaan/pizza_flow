package com.pizzaflow.delivery.entity;

import com.pizzaflow.order.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "couriers")
public class Courier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = true;

    @Column(name = "available_since", nullable = false)
    private Instant availableSince;

    @Column(name = "current_deliveries_count", nullable = false)
    private int currentDeliveriesCount = 0;

    public Courier() {
        this.availableSince = Instant.now();
        this.isAvailable = true;
    }

    public Courier(User user, VehicleType vehicleType) {
        this.user = user;
        this.vehicleType = vehicleType;
        this.isAvailable = true;
        this.availableSince = Instant.now();
        this.currentDeliveriesCount = 0;
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

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public Instant getAvailableSince() {
        return availableSince;
    }

    public void setAvailableSince(Instant availableSince) {
        this.availableSince = availableSince;
    }

    public int getCurrentDeliveriesCount() {
        return currentDeliveriesCount;
    }

    public void setCurrentDeliveriesCount(int currentDeliveriesCount) {
        this.currentDeliveriesCount = currentDeliveriesCount;
    }
}
