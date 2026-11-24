package com.pizzaflow.delivery.repository;

import com.pizzaflow.delivery.entity.Courier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourierRepository extends JpaRepository<Courier, Long> {
    Optional<Courier> findByUserId(Long userId);

    // FIFO Queue: who has been available the longest comes first!
    List<Courier> findByIsAvailableTrueOrderByAvailableSinceAsc();

    long countByIsAvailableTrue();
}
