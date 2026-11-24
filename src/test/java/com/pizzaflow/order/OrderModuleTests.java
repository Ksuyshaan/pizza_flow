package com.pizzaflow.order;

import com.pizzaflow.common.exception.InvalidPromoCodeException;
import com.pizzaflow.common.exception.InvalidStatusTransitionException;
import com.pizzaflow.delivery.entity.DeliveryZone;
import com.pizzaflow.delivery.entity.PaymentMethod;
import com.pizzaflow.delivery.repository.DeliveryZoneRepository;
import com.pizzaflow.kitchen.entity.Category;
import com.pizzaflow.kitchen.entity.Product;
import com.pizzaflow.kitchen.repository.CategoryRepository;
import com.pizzaflow.kitchen.repository.ProductRepository;
import com.pizzaflow.order.dto.CreateOrderRequest;
import com.pizzaflow.order.dto.OrderItemRequest;
import com.pizzaflow.order.dto.OrderResponse;
import com.pizzaflow.order.entity.*;
import com.pizzaflow.order.repository.OrderRepository;
import com.pizzaflow.order.repository.PromoCodeRepository;
import com.pizzaflow.order.repository.UserRepository;
import com.pizzaflow.order.service.OrderService;
import com.pizzaflow.order.service.PromoCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderModuleTests {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PromoCodeService promoCodeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DeliveryZoneRepository deliveryZoneRepository;

    @Autowired
    private PromoCodeRepository promoCodeRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User testCustomer;
    private Product testProduct;
    private DeliveryZone testZone;

    @BeforeEach
    void setup() {
        testCustomer = userRepository.findByEmail("customer@pizzaflow.com").orElseGet(() ->
                userRepository.save(new User("customer@pizzaflow.com", "hash", "Тестовый Клиент", "+79991112233", UserRole.CUSTOMER)));

        testProduct = productRepository.findByNameIgnoreCase("Пепперони 30см").orElseGet(() ->
                productRepository.save(new Product(null, "Пепперони 30см", "Тест", BigDecimal.valueOf(690.00), null, 15)));

        testZone = deliveryZoneRepository.findByNameIgnoreCase("Центральный район").orElseGet(() ->
                deliveryZoneRepository.save(new DeliveryZone("Центральный район", BigDecimal.valueOf(150.00), BigDecimal.valueOf(1000.00), 30)));
    }

    @Test
    @DisplayName("P1-1: Расчёт цены заказа без скидки с платной доставкой")
    void testOrderPricing_SubtotalUnderThreshold() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.DELIVERY);
        req.setDeliveryZoneId(testZone.getId());
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setItems(List.of(new OrderItemRequest(testProduct.getId(), 1))); // 1 * 690 = 690 (< 1000 threshold)

        OrderResponse res = orderService.createOrder(req, testCustomer.getId(), testCustomer.getEmail());

        assertEquals(BigDecimal.valueOf(690.00).setScale(2), res.getSubtotal());
        assertEquals(BigDecimal.ZERO.setScale(2), res.getDiscountAmount());
        assertEquals(BigDecimal.valueOf(150.00).setScale(2), res.getDeliveryFee());
        assertEquals(BigDecimal.valueOf(840.00).setScale(2), res.getTotalAmount()); // 690 + 150 = 840
        assertEquals(OrderStatus.NEW, res.getStatus());
    }

    @Test
    @DisplayName("P1-2: Расчёт цены с бесплатной доставкой при превышении порога")
    void testOrderPricing_SubtotalAboveThreshold_FreeDelivery() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.DELIVERY);
        req.setDeliveryZoneId(testZone.getId());
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setItems(List.of(new OrderItemRequest(testProduct.getId(), 2))); // 2 * 690 = 1380 (> 1000 threshold)

        OrderResponse res = orderService.createOrder(req, testCustomer.getId(), testCustomer.getEmail());

        assertEquals(BigDecimal.valueOf(1380.00).setScale(2), res.getSubtotal());
        assertEquals(BigDecimal.ZERO.setScale(2), res.getDeliveryFee()); // Free!
        assertEquals(BigDecimal.valueOf(1380.00).setScale(2), res.getTotalAmount());
    }

    @Test
    @DisplayName("P1-3: Расчёт цены для самовывоза (PICKUP) — доставка всегда 0")
    void testOrderPricing_Pickup_ZeroDeliveryFee() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.PICKUP);
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setItems(List.of(new OrderItemRequest(testProduct.getId(), 1)));

        OrderResponse res = orderService.createOrder(req, testCustomer.getId(), testCustomer.getEmail());

        assertEquals(BigDecimal.ZERO.setScale(2), res.getDeliveryFee());
        assertEquals(BigDecimal.valueOf(690.00).setScale(2), res.getTotalAmount());
    }

    @Test
    @DisplayName("P1-4: Промокод с процентной скидкой и ограничением максимальной суммы")
    void testPromoCode_PercentageWithMaxCap() {
        // PIZZA20: 20%, min 1200, max discount 500
        BigDecimal discount = promoCodeService.calculateDiscount("PIZZA20", BigDecimal.valueOf(3000.00));
        // 20% of 3000 = 600, capped at max 500
        assertEquals(BigDecimal.valueOf(500.00).setScale(2), discount);
    }

    @Test
    @DisplayName("P1-5: Граница промокода — ровно минимальная сумма заказа")
    void testPromoCode_ExactMinAmount_Allowed() {
        // PIZZA20 min amount is 1200
        BigDecimal discount = promoCodeService.calculateDiscount("PIZZA20", BigDecimal.valueOf(1200.00));
        // 20% of 1200 = 240.00
        assertEquals(BigDecimal.valueOf(240.00).setScale(2), discount);
    }

    @Test
    @DisplayName("P1-6: Граница промокода — сумма на копейку меньше минимальной — отказ")
    void testPromoCode_UnderMinAmount_ThrowsException() {
        assertThrows(InvalidPromoCodeException.class, () ->
                promoCodeService.calculateDiscount("PIZZA20", BigDecimal.valueOf(1199.99)));
    }

    @Test
    @DisplayName("P1-7: Промокод с истекшим сроком действия — отказ")
    void testPromoCode_Expired_ThrowsException() {
        promoCodeRepository.save(new PromoCode("EXPIRED", DiscountType.FIXED, BigDecimal.valueOf(100), BigDecimal.valueOf(500), null, null,
                Instant.now().minusSeconds(86400 * 10), Instant.now().minusSeconds(86400)));

        assertThrows(InvalidPromoCodeException.class, () ->
                promoCodeService.calculateDiscount("EXPIRED", BigDecimal.valueOf(1000)));
    }

    @Test
    @DisplayName("P1-8: Промокод с исчерпанным лимитом использований — отказ")
    void testPromoCode_LimitExceeded_ThrowsException() {
        PromoCode promo = new PromoCode("LIMIT1", DiscountType.FIXED, BigDecimal.valueOf(100), BigDecimal.valueOf(500), null, 1,
                Instant.now().minusSeconds(3600), Instant.now().plusSeconds(3600));
        promo.setTimesUsed(1); // Limit reached!
        promoCodeRepository.save(promo);

        assertThrows(InvalidPromoCodeException.class, () ->
                promoCodeService.calculateDiscount("LIMIT1", BigDecimal.valueOf(1000)));
    }

    @Test
    @DisplayName("P1-9: Валидный переход статусов NEW -> CONFIRMED с записью в StatusHistory")
    void testValidTransition_NewToConfirmed() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.PICKUP);
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setItems(List.of(new OrderItemRequest(testProduct.getId(), 1)));

        OrderResponse created = orderService.createOrder(req, testCustomer.getId(), testCustomer.getEmail());
        OrderResponse confirmed = orderService.changeStatus(created.getId(), OrderStatus.CONFIRMED, "manager@pizzaflow.com", UserRole.MANAGER, "Подтверждено");

        assertEquals(OrderStatus.CONFIRMED, confirmed.getStatus());
        assertNotNull(confirmed.getConfirmedAt());
        assertTrue(confirmed.getHistory().size() >= 2);
    }

    @Test
    @DisplayName("P1-10: Невалидный переход — отмена заказа после начала приготовления (COOKING -> CANCELLED)")
    void testInvalidTransition_CancelAfterCooking_StrictlyForbidden() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.PICKUP);
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setItems(List.of(new OrderItemRequest(testProduct.getId(), 1)));

        OrderResponse created = orderService.createOrder(req, testCustomer.getId(), testCustomer.getEmail());
        orderService.changeStatus(created.getId(), OrderStatus.CONFIRMED, "manager@pizzaflow.com", UserRole.MANAGER, "ОК");
        orderService.changeStatus(created.getId(), OrderStatus.COOKING, "cook@pizzaflow.com", UserRole.COOK, "Готовим");

        // Attempt to cancel after cooking started
        assertThrows(InvalidStatusTransitionException.class, () ->
                orderService.changeStatus(created.getId(), OrderStatus.CANCELLED, testCustomer.getEmail(), UserRole.CUSTOMER, "Клиент передумал"));
    }
}
