package com.pizzaflow.delivery;

import com.pizzaflow.delivery.dto.CourierResponse;
import com.pizzaflow.delivery.dto.DeliveryResponse;
import com.pizzaflow.delivery.entity.*;
import com.pizzaflow.delivery.repository.CourierRepository;
import com.pizzaflow.delivery.repository.DeliveryRepository;
import com.pizzaflow.delivery.repository.DeliveryZoneRepository;
import com.pizzaflow.delivery.repository.PaymentRepository;
import com.pizzaflow.delivery.service.DeliveryService;
import com.pizzaflow.delivery.service.PaymentService;
import com.pizzaflow.kitchen.entity.Product;
import com.pizzaflow.kitchen.repository.ProductRepository;
import com.pizzaflow.order.dto.CreateOrderRequest;
import com.pizzaflow.order.dto.OrderItemRequest;
import com.pizzaflow.order.dto.OrderResponse;
import com.pizzaflow.order.entity.OrderStatus;
import com.pizzaflow.order.entity.OrderType;
import com.pizzaflow.order.entity.User;
import com.pizzaflow.order.entity.UserRole;
import com.pizzaflow.order.repository.OrderRepository;
import com.pizzaflow.order.repository.UserRepository;
import com.pizzaflow.order.service.OrderService;
import com.pizzaflow.report.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DeliveryModuleTests {

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CourierRepository courierRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private DeliveryZoneRepository deliveryZoneRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private User testCustomer;
    private Product testProduct;
    private DeliveryZone testZone;

    @BeforeEach
    void setup() {
        testCustomer = userRepository.findByEmail("customer@pizzaflow.com").get();
        testProduct = productRepository.findByNameIgnoreCase("Пепперони 30см").get();
        testZone = deliveryZoneRepository.findByNameIgnoreCase("Центральный район").get();
    }

    @Test
    @DisplayName("P3-1: Очередь курьеров FIFO — назначается курьер, ожидающий дольше всех (availableSince)")
    void testCourierQueue_FIFO_AssignsOldestAvailable() {
        // Ensure courier 1 has older availableSince than courier 2
        Courier c1 = courierRepository.findAll().get(0);
        c1.setAvailable(true);
        c1.setAvailableSince(Instant.now().minusSeconds(5000));
        courierRepository.save(c1);

        Courier c2 = courierRepository.findAll().get(1);
        c2.setAvailable(true);
        c2.setAvailableSince(Instant.now().minusSeconds(1000));
        courierRepository.save(c2);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.DELIVERY);
        req.setDeliveryZoneId(testZone.getId());
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setItems(List.of(new OrderItemRequest(testProduct.getId(), 1)));

        OrderResponse orderRes = orderService.createOrder(req, testCustomer.getId(), testCustomer.getEmail());
        DeliveryResponse deliveryRes = deliveryService.createDelivery(orderRes.getId());

        assertEquals(DeliveryStatus.ASSIGNED, deliveryRes.getStatus());
        assertEquals(c1.getId(), deliveryRes.getCourierId()); // C1 had older availableSince!

        // C1 is now busy
        Courier c1After = courierRepository.findById(c1.getId()).get();
        assertFalse(c1After.isAvailable());
    }

    @Test
    @DisplayName("P3-2: Ситуация отсутствия свободных курьеров — доставка остается в PENDING_ASSIGNMENT")
    void testCourierQueue_NoCouriersAvailable_PendingAssignment() {
        // Make all couriers busy
        for (Courier c : courierRepository.findAll()) {
            c.setAvailable(false);
            courierRepository.save(c);
        }

        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.DELIVERY);
        req.setDeliveryZoneId(testZone.getId());
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setItems(List.of(new OrderItemRequest(testProduct.getId(), 1)));

        OrderResponse orderRes = orderService.createOrder(req, testCustomer.getId(), testCustomer.getEmail());
        DeliveryResponse deliveryRes = deliveryService.createDelivery(orderRes.getId());

        assertEquals(DeliveryStatus.PENDING_ASSIGNMENT, deliveryRes.getStatus());
        assertNull(deliveryRes.getCourierId());
    }

    @Test
    @DisplayName("P3-3: Забор заказа курьером (pickup) переводит доставку в IN_TRANSIT и заказ в DELIVERING")
    void testPickupDelivery_SetsOrderToDelivering() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.DELIVERY);
        req.setDeliveryZoneId(testZone.getId());
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setItems(List.of(new OrderItemRequest(testProduct.getId(), 1)));

        OrderResponse orderRes = orderService.createOrder(req, testCustomer.getId(), testCustomer.getEmail());
        orderService.changeStatus(orderRes.getId(), OrderStatus.CONFIRMED, "manager@pizzaflow.com", UserRole.MANAGER, "ОК");
        orderService.changeStatus(orderRes.getId(), OrderStatus.COOKING, "cook@pizzaflow.com", UserRole.COOK, "ОК");
        orderService.changeStatus(orderRes.getId(), OrderStatus.READY, "cook@pizzaflow.com", UserRole.COOK, "ОК");

        DeliveryResponse delivery = deliveryService.createDelivery(orderRes.getId());
        DeliveryResponse pickedUp = deliveryService.pickupDelivery(delivery.getId(), "courier@pizzaflow.com");

        assertEquals(DeliveryStatus.IN_TRANSIT, pickedUp.getStatus());
        assertNotNull(pickedUp.getPickedUpAt());

        OrderResponse orderAfter = orderService.getOrderById(orderRes.getId());
        assertEquals(OrderStatus.DELIVERING, orderAfter.getStatus());
    }

    @Test
    @DisplayName("P3-4: Завершение доставки (complete) переводит заказ в COMPLETED и освобождает курьера")
    void testCompleteDelivery_FreesCourierAndCompletesOrder() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.DELIVERY);
        req.setDeliveryZoneId(testZone.getId());
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setItems(List.of(new OrderItemRequest(testProduct.getId(), 1)));

        OrderResponse orderRes = orderService.createOrder(req, testCustomer.getId(), testCustomer.getEmail());
        orderService.changeStatus(orderRes.getId(), OrderStatus.CONFIRMED, "manager@pizzaflow.com", UserRole.MANAGER, "ОК");
        orderService.changeStatus(orderRes.getId(), OrderStatus.COOKING, "cook@pizzaflow.com", UserRole.COOK, "ОК");
        orderService.changeStatus(orderRes.getId(), OrderStatus.READY, "cook@pizzaflow.com", UserRole.COOK, "ОК");

        DeliveryResponse delivery = deliveryService.createDelivery(orderRes.getId());
        deliveryService.pickupDelivery(delivery.getId(), "courier@pizzaflow.com");
        DeliveryResponse completed = deliveryService.completeDelivery(delivery.getId(), "courier@pizzaflow.com");

        assertEquals(DeliveryStatus.DELIVERED, completed.getStatus());
        assertNotNull(completed.getDeliveredAt());

        OrderResponse orderAfter = orderService.getOrderById(orderRes.getId());
        assertEquals(OrderStatus.COMPLETED, orderAfter.getStatus());

        // Courier is freed
        Courier courier = courierRepository.findById(completed.getCourierId()).get();
        assertTrue(courier.isAvailable());
    }

    @Test
    @DisplayName("P3-5: Платёж CASH закрывается в PAID при завершении доставки")
    void testCashPayment_MarkedPaidOnDelivery() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.DELIVERY);
        req.setDeliveryZoneId(testZone.getId());
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setItems(List.of(new OrderItemRequest(testProduct.getId(), 1)));

        OrderResponse orderRes = orderService.createOrder(req, testCustomer.getId(), testCustomer.getEmail());
        orderService.changeStatus(orderRes.getId(), OrderStatus.CONFIRMED, "manager@pizzaflow.com", UserRole.MANAGER, "ОК");
        orderService.changeStatus(orderRes.getId(), OrderStatus.COOKING, "cook@pizzaflow.com", UserRole.COOK, "ОК");
        orderService.changeStatus(orderRes.getId(), OrderStatus.READY, "cook@pizzaflow.com", UserRole.COOK, "ОК");

        DeliveryResponse delivery = deliveryService.createDelivery(orderRes.getId());
        deliveryService.pickupDelivery(delivery.getId(), "courier@pizzaflow.com");
        deliveryService.completeDelivery(delivery.getId(), "courier@pizzaflow.com");

        Payment payment = paymentRepository.findByOrderId(orderRes.getId()).get();
        assertEquals(PaymentStatus.PAID, payment.getPaymentStatus());
        assertNotNull(payment.getPaidAt());
    }

    @Test
    @DisplayName("P3-6: Возврат онлайн-платежа при отмене заказа (REFUNDED)")
    void testPaymentRefund_OnOrderCancel() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.PICKUP);
        req.setPaymentMethod(PaymentMethod.ONLINE); // Already PAID
        req.setItems(List.of(new OrderItemRequest(testProduct.getId(), 1)));

        OrderResponse orderRes = orderService.createOrder(req, testCustomer.getId(), testCustomer.getEmail());
        Payment paymentBefore = paymentRepository.findByOrderId(orderRes.getId()).get();
        assertEquals(PaymentStatus.PAID, paymentBefore.getPaymentStatus());

        // Cancel order while in NEW
        orderService.cancelOrder(orderRes.getId(), testCustomer.getEmail(), UserRole.CUSTOMER, "Передумал");

        Payment paymentAfter = paymentRepository.findByOrderId(orderRes.getId()).get();
        assertEquals(PaymentStatus.REFUNDED, paymentAfter.getPaymentStatus());
    }

    @Test
    @DisplayName("P3-7: Генерация Excel-отчёта по продажам через Apache POI (.xlsx)")
    void testGenerateSalesExcelReport_CreatesValidWorkbookBytes() throws IOException {
        byte[] excelBytes = reportService.generateSalesExcelReport(
                Instant.now().minusSeconds(86400 * 7),
                Instant.now()
        );

        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 1000, "Excel binary must contain valid workbook structure");
    }

    @Test
    @DisplayName("P3-8: Генерация Word-отчёта по смене через Apache POI (.docx)")
    void testGenerateShiftWordReport_CreatesValidDocxBytes() throws IOException {
        byte[] docxBytes = reportService.generateShiftWordReport(LocalDate.now());

        assertNotNull(docxBytes);
        assertTrue(docxBytes.length > 1000, "Word binary must contain valid docx document structure");
    }

    @Test
    @DisplayName("P3-9: Ручное переназначение курьера менеджером (manualAssign)")
    void testManualAssignCourier_ReplacesCourierCleanly() {
        Courier c1 = courierRepository.findAll().get(0);
        Courier c2 = courierRepository.findAll().get(1);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.DELIVERY);
        req.setDeliveryZoneId(testZone.getId());
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setItems(List.of(new OrderItemRequest(testProduct.getId(), 1)));

        OrderResponse orderRes = orderService.createOrder(req, testCustomer.getId(), testCustomer.getEmail());
        DeliveryResponse delivery = deliveryService.createDelivery(orderRes.getId());

        // Reassign to c2 manually
        DeliveryResponse reassigned = deliveryService.manualAssignCourier(delivery.getId(), c2.getId());

        assertEquals(c2.getId(), reassigned.getCourierId());
        assertFalse(courierRepository.findById(c2.getId()).get().isAvailable());
    }
}
