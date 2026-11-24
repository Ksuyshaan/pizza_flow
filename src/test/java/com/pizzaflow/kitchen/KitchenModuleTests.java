package com.pizzaflow.kitchen;

import com.pizzaflow.common.exception.InsufficientStockException;
import com.pizzaflow.delivery.entity.PaymentMethod;
import com.pizzaflow.kitchen.dto.KitchenLoadResponse;
import com.pizzaflow.kitchen.dto.KitchenOrderResponse;
import com.pizzaflow.kitchen.dto.StockMovementRequest;
import com.pizzaflow.kitchen.dto.StockMovementResponse;
import com.pizzaflow.kitchen.entity.*;
import com.pizzaflow.kitchen.repository.IngredientRepository;
import com.pizzaflow.kitchen.repository.KitchenOrderRepository;
import com.pizzaflow.kitchen.repository.ProductRepository;
import com.pizzaflow.kitchen.repository.StockMovementRepository;
import com.pizzaflow.kitchen.service.InventoryService;
import com.pizzaflow.kitchen.service.KitchenService;
import com.pizzaflow.order.dto.CreateOrderRequest;
import com.pizzaflow.order.dto.OrderItemRequest;
import com.pizzaflow.order.dto.OrderResponse;
import com.pizzaflow.order.entity.Order;
import com.pizzaflow.order.entity.OrderStatus;
import com.pizzaflow.order.entity.OrderType;
import com.pizzaflow.order.entity.User;
import com.pizzaflow.order.entity.UserRole;
import com.pizzaflow.order.repository.OrderRepository;
import com.pizzaflow.order.repository.UserRepository;
import com.pizzaflow.order.service.OrderService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class KitchenModuleTests {

    @Autowired
    private KitchenService kitchenService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KitchenOrderRepository kitchenOrderRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private EntityManager entityManager;

    private User testCustomer;
    private Product testProduct;
    private Ingredient testFlour;
    private Ingredient testCheese;

    @BeforeEach
    void setup() {
        testCustomer = userRepository.findByEmail("customer@pizzaflow.com").get();
        testProduct = productRepository.findByNameIgnoreCase("Пепперони 30см").get();
        testFlour = ingredientRepository.findByNameIgnoreCase("Мука пшеничная в/с").get();
        testCheese = ingredientRepository.findByNameIgnoreCase("Сыр Моцарелла").get();
    }

    @Test
    @DisplayName("P2-1: Списание ингредиентов по рецепту при старте приготовления (startCooking)")
    void testStartCooking_DeductsIngredientsAccurately() {
        BigDecimal initialFlour = testFlour.getStockQuantity();
        BigDecimal initialCheese = testCheese.getStockQuantity();

        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.PICKUP);
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setItems(List.of(new OrderItemRequest(testProduct.getId(), 2))); // 2 pizzas

        OrderResponse orderRes = orderService.createOrder(req, testCustomer.getId(), testCustomer.getEmail());
        orderService.changeStatus(orderRes.getId(), OrderStatus.CONFIRMED, "manager@pizzaflow.com", UserRole.MANAGER, "ОК");
        KitchenOrderResponse ko = kitchenService.createKitchenOrder(orderRes.getId());

        // Start cooking
        KitchenOrderResponse startedKo = kitchenService.startCooking(ko.getId(), "cook@pizzaflow.com");

        assertEquals(KitchenOrderStatus.IN_PROGRESS, startedKo.getStatus());

        // Check ingredients deducted: 2 pizzas * 250g flour = 500g, 2 pizzas * 180g cheese = 360g
        Ingredient flourAfter = ingredientRepository.findById(testFlour.getId()).get();
        Ingredient cheeseAfter = ingredientRepository.findById(testCheese.getId()).get();

        assertEquals(initialFlour.subtract(BigDecimal.valueOf(500.00)), flourAfter.getStockQuantity());
        assertEquals(initialCheese.subtract(BigDecimal.valueOf(360.00)), cheeseAfter.getStockQuantity());
    }

    @Test
    @DisplayName("P2-2: Нехватка сырья на складе (stock < required) — отказ и откат")
    void testStartCooking_InsufficientStock_Rollback() {
        // Reduce cheese stock to only 50g (Пепперони requires 180g)
        testCheese.setStockQuantity(BigDecimal.valueOf(50.00));
        ingredientRepository.save(testCheese);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.PICKUP);
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setItems(List.of(new OrderItemRequest(testProduct.getId(), 1)));

        OrderResponse orderRes = orderService.createOrder(req, testCustomer.getId(), testCustomer.getEmail());
        orderService.changeStatus(orderRes.getId(), OrderStatus.CONFIRMED, "manager@pizzaflow.com", UserRole.MANAGER, "ОК");
        KitchenOrderResponse ko = kitchenService.createKitchenOrder(orderRes.getId());

        assertThrows(InsufficientStockException.class, () ->
                kitchenService.startCooking(ko.getId(), "cook@pizzaflow.com"));
    }

    @Test
    @DisplayName("P2-3: Граничное условие — остаток ровно равен расходу (balance = 0) — успешно")
    void testStartCooking_ExactZeroBalance_Allowed() {
        // Set cheese to exactly 180g
        testCheese.setStockQuantity(BigDecimal.valueOf(180.00));
        ingredientRepository.save(testCheese);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.PICKUP);
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setItems(List.of(new OrderItemRequest(testProduct.getId(), 1)));

        OrderResponse orderRes = orderService.createOrder(req, testCustomer.getId(), testCustomer.getEmail());
        orderService.changeStatus(orderRes.getId(), OrderStatus.CONFIRMED, "manager@pizzaflow.com", UserRole.MANAGER, "ОК");
        KitchenOrderResponse ko = kitchenService.createKitchenOrder(orderRes.getId());

        KitchenOrderResponse started = kitchenService.startCooking(ko.getId(), "cook@pizzaflow.com");
        assertEquals(KitchenOrderStatus.IN_PROGRESS, started.getStatus());

        Ingredient cheeseAfter = ingredientRepository.findById(testCheese.getId()).get();
        assertEquals(BigDecimal.ZERO.setScale(2), cheeseAfter.getStockQuantity().setScale(2));
    }

    @Test
    @DisplayName("P2-4: Журнал движений (StockMovement) фиксирует списание с типом CONSUMPTION")
    void testStartCooking_RecordsStockMovementJournal() {
        long movementsBefore = stockMovementRepository.count();

        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.PICKUP);
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setItems(List.of(new OrderItemRequest(testProduct.getId(), 1)));

        OrderResponse orderRes = orderService.createOrder(req, testCustomer.getId(), testCustomer.getEmail());
        orderService.changeStatus(orderRes.getId(), OrderStatus.CONFIRMED, "manager@pizzaflow.com", UserRole.MANAGER, "ОК");
        KitchenOrderResponse ko = kitchenService.createKitchenOrder(orderRes.getId());
        kitchenService.startCooking(ko.getId(), "cook@pizzaflow.com");

        long movementsAfter = stockMovementRepository.count();
        // Pepperoni has 4 recipe items -> 4 new movement entries created!
        assertTrue(movementsAfter >= movementsBefore + 4);
    }

    @Test
    @DisplayName("P2-5: Оприходование ингредиента на склад (RECEIPT) увеличивает баланс")
    void testInventoryReceipt_IncreasesBalance() {
        BigDecimal before = testFlour.getStockQuantity();
        StockMovementRequest req = new StockMovementRequest(testFlour.getId(), MovementType.RECEIPT, BigDecimal.valueOf(10000.00), "Поставка №102");

        StockMovementResponse res = inventoryService.createMovement(req, "manager@pizzaflow.com");

        assertEquals(before.add(BigDecimal.valueOf(10000.00)), res.getBalanceAfter());
    }

    @Test
    @DisplayName("P2-6: Списание испорченных продуктов (WASTE) уменьшает остаток")
    void testInventoryWaste_DecreasesBalance() {
        BigDecimal before = testFlour.getStockQuantity();
        StockMovementRequest req = new StockMovementRequest(testFlour.getId(), MovementType.WASTE, BigDecimal.valueOf(500.00), "Брак упаковки");

        StockMovementResponse res = inventoryService.createMovement(req, "manager@pizzaflow.com");

        assertEquals(before.subtract(BigDecimal.valueOf(500.00)), res.getBalanceAfter());
    }

    @Test
    @DisplayName("P2-7: Завершение приготовления (completeCooking) переводит заказ в READY")
    void testCompleteCooking_SetsOrderToReady() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.PICKUP);
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setItems(List.of(new OrderItemRequest(testProduct.getId(), 1)));

        OrderResponse orderRes = orderService.createOrder(req, testCustomer.getId(), testCustomer.getEmail());
        orderService.changeStatus(orderRes.getId(), OrderStatus.CONFIRMED, "manager@pizzaflow.com", UserRole.MANAGER, "ОК");
        KitchenOrderResponse ko = kitchenService.createKitchenOrder(orderRes.getId());
        kitchenService.startCooking(ko.getId(), "cook@pizzaflow.com");

        KitchenOrderResponse completedKo = kitchenService.completeCooking(ko.getId(), "cook@pizzaflow.com");
        assertEquals(KitchenOrderStatus.COMPLETED, completedKo.getStatus());

        Order finalOrder = orderRepository.findById(orderRes.getId()).get();
        assertEquals(OrderStatus.READY, finalOrder.getStatus());
    }

    @Test
    @DisplayName("P2-8: Расчет нагрузки кухни (Kitchen Load Index)")
    void testKitchenLoadCalculation() {
        KitchenLoadResponse load = kitchenService.getKitchenLoad();
        assertNotNull(load);
        assertTrue(load.getEstimatedWaitMinutes() >= 15);
        assertNotNull(load.getLoadLevel());
    }

    @Test
    @DisplayName("P2-9: Оптимистическая блокировка (@Version) защищает Ingredient от параллельного конфликта")
    void testOptimisticLocking_IngredientVersionConflict() {
        // Read ingredient instance 1
        Ingredient ing1 = ingredientRepository.findById(testFlour.getId()).get();
        Long initialVersion = ing1.getVersion();

        // Update ingredient through repository (simulating first concurrent transaction)
        ing1.setStockQuantity(ing1.getStockQuantity().add(BigDecimal.valueOf(100.00)));
        ingredientRepository.saveAndFlush(ing1);

        // Try to update an old detached instance with outdated version
        Ingredient staleIng = new Ingredient();
        staleIng.setId(testFlour.getId());
        staleIng.setName(testFlour.getName());
        staleIng.setUnit(testFlour.getUnit());
        staleIng.setMinStockThreshold(testFlour.getMinStockThreshold());
        staleIng.setCostPerUnit(testFlour.getCostPerUnit());
        staleIng.setStockQuantity(BigDecimal.valueOf(1000.00));
        staleIng.setVersion(initialVersion); // Stale version!

        assertThrows(OptimisticLockException.class, () -> {
            entityManager.merge(staleIng);
            entityManager.flush();
        });
    }
}
