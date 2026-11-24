package com.pizzaflow.kitchen.repository;

import com.pizzaflow.kitchen.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    Optional<Ingredient> findByNameIgnoreCase(String name);

    @Query("SELECT i FROM Ingredient i WHERE i.stockQuantity <= i.minStockThreshold")
    List<Ingredient> findLowStockIngredients();
}
