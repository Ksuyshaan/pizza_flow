package com.pizzaflow.delivery.repository;

import com.pizzaflow.delivery.entity.DeliveryZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, Long> {
    Optional<DeliveryZone> findByNameIgnoreCase(String name);
    List<DeliveryZone> findByActiveTrue();
}
