package com.pizzaflow.kitchen.service;

import com.pizzaflow.common.exception.ConflictException;
import com.pizzaflow.common.exception.InsufficientStockException;
import com.pizzaflow.common.exception.ResourceNotFoundException;
import com.pizzaflow.kitchen.dto.KitchenLoadResponse;
import com.pizzaflow.kitchen.dto.KitchenOrderResponse;
import com.pizzaflow.kitchen.entity.*;
import com.pizzaflow.kitchen.repository.IngredientRepository;
import com.pizzaflow.kitchen.repository.KitchenOrderRepository;
import com.pizzaflow.kitchen.repository.StockMovementRepository;
import com.pizzaflow.order.entity.Order;
import com.pizzaflow.order.entity.OrderItem;
import com.pizzaflow.order.entity.OrderStatus;
import com.pizzaflow.order.entity.UserRole;
import com.pizzaflow.order.repository.OrderRepository;
import com.pizzaflow.order.service.OrderService;
import com.pizzaflow.report.service.AuditService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KitchenService {

    private final KitchenOrderRepository kitchenOrderRepository;
    private final OrderRepository orderRepository;
    private final IngredientRepository ingredientRepository;
    private final StockMovementRepository stockMovementRepository;
    private final OrderService orderService;
    private final AuditService auditService;

    public KitchenService(KitchenOrderRepository kitchenOrderRepository,
                          OrderRepository orderRepository,
                          IngredientRepository ingredientRepository,
                          StockMovementRepository stockMovementRepository,
                          @Lazy OrderService orderService,
                          AuditService auditService) {
        this.kitchenOrderRepository = kitchenOrderRepository;
        this.orderRepository = orderRepository;
        this.ingredientRepository = ingredientRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.orderService = orderService;
        this.auditService = auditService;
    }

    @Transactional
    public KitchenOrderResponse createKitchenOrder(Long orderId) {
        if (kitchenOrderRepository.findByOrderId(orderId).isPresent()) {
            return KitchenOrderResponse.fromEntity(kitchenOrderRepository.findByOrderId(orderId).get());
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ не найден: ID " + orderId));

        int maxPrepMinutes = 15;
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null && item.getProduct().getPreparationMinutes() > maxPrepMinutes) {
                    maxPrepMinutes = item.getProduct().getPreparationMinutes();
                }
            }
        }

        KitchenOrder ko = new KitchenOrder(order, 1, maxPrepMinutes);
        KitchenOrder saved = kitchenOrderRepository.save(ko);

        auditService.log("SYSTEM", "KITCHEN", "CREATE_KITCHEN_ORDER",
                "Создан наряд кухни для заказа #" + order.getOrderNumber());

        return KitchenOrderResponse.fromEntity(saved);
    }

    @Transactional
    public KitchenOrderResponse startCooking(Long kitchenOrderId, String cookUsername) {
        KitchenOrder ko = kitchenOrderRepository.findById(kitchenOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Наряд кухни не найден: ID " + kitchenOrderId));

        if (ko.getStatus() != KitchenOrderStatus.PENDING) {
            throw new ConflictException("Наряд кухни уже находится в работе или выполнен (статус: " + ko.getStatus() + ")");
        }

        Order order = ko.getOrder();

        // 1. Calculate Required Ingredients from Recipes
        Map<Long, BigDecimal> requiredIngredients = new HashMap<>();

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                if (product != null && product.getRecipeItems() != null) {
                    for (RecipeItem recipeItem : product.getRecipeItems()) {
                        Long ingId = recipeItem.getIngredient().getId();
                        BigDecimal qtyForThisItem = recipeItem.getQuantityRequired()
                                .multiply(BigDecimal.valueOf(item.getQuantity()));
                        requiredIngredients.merge(ingId, qtyForThisItem, BigDecimal::add);
                    }
                }
            }
        }

        // 2. Validate Stock and Deduct with @Version check
        for (Map.Entry<Long, BigDecimal> entry : requiredIngredients.entrySet()) {
            Long ingId = entry.getKey();
            BigDecimal requiredQty = entry.getValue();

            Ingredient ingredient = ingredientRepository.findById(ingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ингредиент не найден: ID " + ingId));

            if (ingredient.getStockQuantity().compareTo(requiredQty) < 0) {
                throw new InsufficientStockException(String.format(
                        "Недостаточно ингредиента '%s' на складе: требуется %.2f %s, доступно %.2f %s. Приготовление невозможно.",
                        ingredient.getName(), requiredQty, ingredient.getUnit(), ingredient.getStockQuantity(), ingredient.getUnit()));
            }

            BigDecimal newBalance = ingredient.getStockQuantity().subtract(requiredQty);
            ingredient.setStockQuantity(newBalance);
            Ingredient savedIngredient = ingredientRepository.save(ingredient); // Optimistic locking (@Version) check happens here!

            // Log consumption movement
            StockMovement movement = new StockMovement(
                    savedIngredient,
                    MovementType.CONSUMPTION,
                    requiredQty,
                    newBalance,
                    "ORDER",
                    order.getOrderNumber(),
                    "Списание по рецепту для заказа #" + order.getOrderNumber(),
                    cookUsername
            );
            stockMovementRepository.save(movement);
        }

        // 3. Update KitchenOrder
        ko.setStatus(KitchenOrderStatus.IN_PROGRESS);
        ko.setStartedAt(Instant.now());
        ko.setCookUsername(cookUsername);
        KitchenOrder savedKo = kitchenOrderRepository.save(ko);

        // 4. Update Main Order Status to COOKING
        orderService.changeStatus(order.getId(), OrderStatus.COOKING, cookUsername, UserRole.COOK, "Начато приготовление блюд");

        return KitchenOrderResponse.fromEntity(savedKo);
    }

    @Transactional
    public KitchenOrderResponse completeCooking(Long kitchenOrderId, String cookUsername) {
        KitchenOrder ko = kitchenOrderRepository.findById(kitchenOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Наряд кухни не найден: ID " + kitchenOrderId));

        if (ko.getStatus() != KitchenOrderStatus.IN_PROGRESS) {
            throw new ConflictException("Наряд кухни не находится в процессе готовки (статус: " + ko.getStatus() + ")");
        }

        ko.setStatus(KitchenOrderStatus.COMPLETED);
        ko.setCompletedAt(Instant.now());
        KitchenOrder savedKo = kitchenOrderRepository.save(ko);

        // Update Main Order Status to READY
        orderService.changeStatus(ko.getOrder().getId(), OrderStatus.READY, cookUsername, UserRole.COOK, "Приготовление завершено");

        return KitchenOrderResponse.fromEntity(savedKo);
    }

    @Transactional(readOnly = true)
    public KitchenLoadResponse getKitchenLoad() {
        long pending = kitchenOrderRepository.countByStatus(KitchenOrderStatus.PENDING);
        long inProgress = kitchenOrderRepository.countByStatus(KitchenOrderStatus.IN_PROGRESS);
        long active = pending + inProgress;

        int waitMinutes = 15 + (int) (active * 5);
        String level = (active < 3) ? "LOW" : (active < 7) ? "MEDIUM" : "HIGH";

        return new KitchenLoadResponse(pending, inProgress, waitMinutes, level);
    }

    @Transactional(readOnly = true)
    public List<KitchenOrderResponse> getKitchenOrders(KitchenOrderStatus status) {
        List<KitchenOrder> list;
        if (status != null) {
            list = kitchenOrderRepository.findByStatusOrderByPriorityDescStartedAtAsc(status);
        } else {
            list = kitchenOrderRepository.findByStatusIn(List.of(KitchenOrderStatus.PENDING, KitchenOrderStatus.IN_PROGRESS));
        }
        return list.stream()
                .map(KitchenOrderResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
