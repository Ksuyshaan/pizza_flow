package com.pizzaflow.kitchen.repository;

import com.pizzaflow.kitchen.entity.KitchenOrder;
import com.pizzaflow.kitchen.entity.KitchenOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KitchenOrderRepository extends JpaRepository<KitchenOrder, Long> {
    Optional<KitchenOrder> findByOrderId(Long orderId);
    List<KitchenOrder> findByStatusOrderByPriorityDescStartedAtAsc(KitchenOrderStatus status);
    List<KitchenOrder> findByStatusIn(List<KitchenOrderStatus> statuses);
    long countByStatus(KitchenOrderStatus status);
    long countByStatusIn(List<KitchenOrderStatus> statuses);
}
