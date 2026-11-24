package com.pizzaflow.delivery.repository;

import com.pizzaflow.delivery.entity.Delivery;
import com.pizzaflow.delivery.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findByOrderId(Long orderId);
    List<Delivery> findByCourierId(Long courierId);
    List<Delivery> findByCourierIdAndStatusIn(Long courierId, List<DeliveryStatus> statuses);
    List<Delivery> findByStatus(DeliveryStatus status);
}
