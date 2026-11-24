package com.pizzaflow.kitchen.service;

import com.pizzaflow.common.exception.InsufficientStockException;
import com.pizzaflow.common.exception.ResourceNotFoundException;
import com.pizzaflow.kitchen.dto.IngredientResponse;
import com.pizzaflow.kitchen.dto.StockMovementRequest;
import com.pizzaflow.kitchen.dto.StockMovementResponse;
import com.pizzaflow.kitchen.entity.Ingredient;
import com.pizzaflow.kitchen.entity.MovementType;
import com.pizzaflow.kitchen.entity.StockMovement;
import com.pizzaflow.kitchen.repository.IngredientRepository;
import com.pizzaflow.kitchen.repository.StockMovementRepository;
import com.pizzaflow.report.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final IngredientRepository ingredientRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AuditService auditService;

    public InventoryService(IngredientRepository ingredientRepository,
                            StockMovementRepository stockMovementRepository,
                            AuditService auditService) {
        this.ingredientRepository = ingredientRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<IngredientResponse> getAllIngredients() {
        return ingredientRepository.findAll().stream()
                .map(IngredientResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IngredientResponse> getLowStockIngredients() {
        return ingredientRepository.findLowStockIngredients().stream()
                .map(IngredientResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public StockMovementResponse createMovement(StockMovementRequest req, String actorUsername) {
        Ingredient ingredient = ingredientRepository.findById(req.getIngredientId())
                .orElseThrow(() -> new ResourceNotFoundException("Ингредиент не найден: ID " + req.getIngredientId()));

        BigDecimal currentStock = ingredient.getStockQuantity();
        BigDecimal newStock;

        if (req.getMovementType() == MovementType.RECEIPT) {
            newStock = currentStock.add(req.getQuantity());
        } else if (req.getMovementType() == MovementType.CONSUMPTION || req.getMovementType() == MovementType.WASTE) {
            if (currentStock.compareTo(req.getQuantity()) < 0) {
                throw new InsufficientStockException(String.format(
                        "Недостаточно ингредиента '%s' на складе для списания: требуется %.2f %s, доступно %.2f %s",
                        ingredient.getName(), req.getQuantity(), ingredient.getUnit(), currentStock, ingredient.getUnit()));
            }
            newStock = currentStock.subtract(req.getQuantity());
        } else if (req.getMovementType() == MovementType.ADJUSTMENT) {
            newStock = req.getQuantity();
        } else {
            throw new IllegalArgumentException("Неизвестный тип движения: " + req.getMovementType());
        }

        ingredient.setStockQuantity(newStock);
        Ingredient savedIngredient = ingredientRepository.save(ingredient);

        StockMovement movement = new StockMovement(
                savedIngredient,
                req.getMovementType(),
                req.getQuantity(),
                newStock,
                req.getReferenceType() != null ? req.getReferenceType() : "MANUAL",
                req.getReferenceId(),
                req.getReason(),
                actorUsername
        );

        StockMovement savedMovement = stockMovementRepository.save(movement);

        auditService.log(actorUsername, "MANAGER", "STOCK_MOVEMENT",
                String.format("Складская операция [%s] для '%s': %.2f %s -> остаток: %.2f %s",
                        req.getMovementType(), ingredient.getName(), req.getQuantity(), ingredient.getUnit(), newStock, ingredient.getUnit()));

        return StockMovementResponse.fromEntity(savedMovement);
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovements(Long ingredientId, MovementType type) {
        List<StockMovement> list;
        if (ingredientId != null) {
            list = stockMovementRepository.findByIngredientIdOrderByCreatedAtDesc(ingredientId);
        } else if (type != null) {
            list = stockMovementRepository.findByMovementTypeOrderByCreatedAtDesc(type);
        } else {
            list = stockMovementRepository.findAllByOrderByCreatedAtDesc();
        }
        return list.stream()
                .map(StockMovementResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
