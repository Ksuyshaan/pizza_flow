package com.pizzaflow.kitchen.repository;

import com.pizzaflow.kitchen.entity.MovementType;
import com.pizzaflow.kitchen.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByIngredientIdOrderByCreatedAtDesc(Long ingredientId);
    List<StockMovement> findByMovementTypeOrderByCreatedAtDesc(MovementType movementType);
    List<StockMovement> findAllByOrderByCreatedAtDesc();
}
