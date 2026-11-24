package com.pizzaflow.order.repository;

import com.pizzaflow.order.entity.Order;
import com.pizzaflow.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findByUserId(Long userId, Pageable pageable);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    List<Order> findByCreatedAtBetween(Instant from, Instant to);

    @Query("SELECT o FROM Order o WHERE o.createdAt >= :from AND o.createdAt <= :to AND o.status = :status")
    List<Order> findByPeriodAndStatus(@Param("from") Instant from, @Param("to") Instant to, @Param("status") OrderStatus status);

    long countByStatus(OrderStatus status);
}
