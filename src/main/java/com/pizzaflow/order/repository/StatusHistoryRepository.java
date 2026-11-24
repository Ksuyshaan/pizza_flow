package com.pizzaflow.order.repository;

import com.pizzaflow.order.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByOrderIdOrderByChangedAtAsc(Long orderId);
}
