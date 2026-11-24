package com.pizzaflow.kitchen.repository;

import com.pizzaflow.kitchen.entity.RecipeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeItemRepository extends JpaRepository<RecipeItem, Long> {
    List<RecipeItem> findByProductId(Long productId);
    void deleteByProductId(Long productId);
}
