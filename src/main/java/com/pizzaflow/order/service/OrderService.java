package com.pizzaflow.order.service;

import com.pizzaflow.common.exception.BadRequestException;
import com.pizzaflow.common.exception.ConflictException;
import com.pizzaflow.common.exception.InvalidStatusTransitionException;
import com.pizzaflow.common.exception.ResourceNotFoundException;
import com.pizzaflow.delivery.entity.DeliveryZone;
import com.pizzaflow.delivery.entity.Payment;
import com.pizzaflow.delivery.repository.DeliveryZoneRepository;
import com.pizzaflow.delivery.repository.PaymentRepository;
import com.pizzaflow.delivery.service.DeliveryService;
import com.pizzaflow.delivery.service.PaymentService;
import com.pizzaflow.kitchen.entity.Product;
import com.pizzaflow.kitchen.repository.ProductRepository;
import com.pizzaflow.kitchen.service.KitchenService;
import com.pizzaflow.order.dto.CreateOrderRequest;
import com.pizzaflow.order.dto.OrderItemRequest;
import com.pizzaflow.order.dto.OrderResponse;
import com.pizzaflow.order.entity.*;
import com.pizzaflow.order.repository.AddressRepository;
import com.pizzaflow.order.repository.OrderRepository;
import com.pizzaflow.order.repository.UserRepository;
import com.pizzaflow.report.service.AuditService;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final DeliveryZoneRepository deliveryZoneRepository;
    private final PaymentRepository paymentRepository;
    private final PromoCodeService promoCodeService;
    private final AuditService auditService;
    private final KitchenService kitchenService;
    private final DeliveryService deliveryService;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        ProductRepository productRepository,
                        AddressRepository addressRepository,
                        DeliveryZoneRepository deliveryZoneRepository,
                        PaymentRepository paymentRepository,
                        PromoCodeService promoCodeService,
                        AuditService auditService,
                        @Lazy KitchenService kitchenService,
                        @Lazy DeliveryService deliveryService,
                        @Lazy PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.deliveryZoneRepository = deliveryZoneRepository;
        this.paymentRepository = paymentRepository;
        this.promoCodeService = promoCodeService;
        this.auditService = auditService;
        this.kitchenService = kitchenService;
        this.deliveryService = deliveryService;
        this.paymentService = paymentService;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest req, Long userId, String username) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BadRequestException("В заказе должна быть хотя бы одна позиция");
        }

        // 1. Calculate Subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemRequest itemReq : req.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Продукт с ID " + itemReq.getProductId() + " не найден"));
            if (!product.isActive()) {
                throw new BadRequestException("Продукт '" + product.getName() + "' временно недоступен для заказа");
            }
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(lineTotal);
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        // 2. Promo Code Calculation
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String appliedPromo = null;
        if (req.getPromoCode() != null && !req.getPromoCode().trim().isEmpty()) {
            discountAmount = promoCodeService.calculateDiscount(req.getPromoCode().trim(), subtotal);
            appliedPromo = req.getPromoCode().trim().toUpperCase();
        }

        // 3. Delivery Fee Calculation
        BigDecimal deliveryFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        DeliveryZone zone = null;
        String addressSnapshot = null;

        if (req.getOrderType() == OrderType.DELIVERY) {
            if (req.getAddressId() != null) {
                Address address = addressRepository.findById(req.getAddressId())
                        .orElseThrow(() -> new ResourceNotFoundException("Адрес не найден"));
                zone = address.getDeliveryZone();
                addressSnapshot = address.toFormattedSnapshot();
            } else if (req.getDeliveryZoneId() != null) {
                zone = deliveryZoneRepository.findById(req.getDeliveryZoneId())
                        .orElseThrow(() -> new ResourceNotFoundException("Зона доставки не найдена"));
                addressSnapshot = (req.getCustomAddress() != null && !req.getCustomAddress().isBlank())
                        ? req.getCustomAddress()
                        : "Адрес в зоне: " + zone.getName();
            } else {
                throw new BadRequestException("Для доставки необходимо указать сохраненный адрес или выбрать тарифную зону");
            }

            if (zone != null) {
                if (subtotal.compareTo(zone.getFreeDeliveryThreshold()) >= 0) {
                    deliveryFee = BigDecimal.ZERO; // Free delivery threshold met!
                } else {
                    deliveryFee = zone.getDeliveryFee();
                }
            }
        } else {
            addressSnapshot = "Самовывоз из ресторана (ул. Пиццерийная, 1)";
        }
        deliveryFee = deliveryFee.setScale(2, RoundingMode.HALF_UP);

        // 4. Total Amount
        BigDecimal totalAmount = subtotal.subtract(discountAmount).add(deliveryFee).max(BigDecimal.ZERO);
        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);

        // 5. Unique Order Number
        String orderNumber = "PF-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Order order = new Order(
                orderNumber,
                user,
                req.getOrderType(),
                subtotal,
                discountAmount,
                deliveryFee,
                totalAmount,
                appliedPromo,
                addressSnapshot,
                zone
        );

        // Add Order Items
        for (OrderItemRequest itemReq : req.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId()).get();
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            OrderItem orderItem = new OrderItem(
                    order,
                    product,
                    product.getName(),
                    product.getPrice(),
                    itemReq.getQuantity(),
                    lineTotal
            );
            order.addItem(orderItem);
        }

        // Add Initial Status History
        StatusHistory initialHistory = new StatusHistory(
                order,
                null,
                OrderStatus.NEW.name(),
                username,
                user.getRole().name(),
                "Заказ оформлен клиентом"
        );
        order.addStatusHistory(initialHistory);

        Order savedOrder = orderRepository.save(order);

        // Increment Promo usage
        if (appliedPromo != null) {
            promoCodeService.incrementUsage(appliedPromo);
        }

        // Create Payment record
        Payment payment = new Payment(savedOrder, req.getPaymentMethod(), totalAmount);
        paymentRepository.save(payment);

        auditService.log(username, user.getRole().name(), "ORDER_CREATED",
                "Создан заказ #" + orderNumber + " на сумму " + totalAmount + " руб.");

        return OrderResponse.fromEntity(savedOrder);
    }

    @Transactional
    public OrderResponse changeStatus(Long orderId, OrderStatus newStatus, String actorUsername, UserRole actorRole, String comment) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ с ID " + orderId + " не найден"));

        OrderStatus currentStatus = order.getStatus();

        // Idempotent check
        if (currentStatus == newStatus) {
            return OrderResponse.fromEntity(order);
        }

        // Validate Allowed Transitions State Machine
        validateStatusTransition(order, currentStatus, newStatus, actorRole);

        // Apply Transition
        order.setStatus(newStatus);
        Instant now = Instant.now();

        if (newStatus == OrderStatus.CONFIRMED) {
            order.setConfirmedAt(now);
        } else if (newStatus == OrderStatus.COMPLETED) {
            order.setCompletedAt(now);
        } else if (newStatus == OrderStatus.CANCELLED) {
            order.setCancelledAt(now);
            if (comment != null && !comment.isBlank()) {
                order.setCancellationReason(comment);
            }
        }

        // Record Status History
        String roleStr = actorRole != null ? actorRole.name() : "SYSTEM";
        StatusHistory history = new StatusHistory(
                order,
                currentStatus.name(),
                newStatus.name(),
                actorUsername,
                roleStr,
                comment != null ? comment : "Переход в статус " + newStatus
        );
        order.addStatusHistory(history);

        Order updatedOrder = orderRepository.save(order);

        // Business Triggers
        if (newStatus == OrderStatus.CONFIRMED) {
            kitchenService.createKitchenOrder(updatedOrder.getId());
        } else if (newStatus == OrderStatus.READY && updatedOrder.getOrderType() == OrderType.DELIVERY) {
            deliveryService.createDelivery(updatedOrder.getId());
        } else if (newStatus == OrderStatus.COMPLETED) {
            paymentService.markPaymentPaid(updatedOrder.getId());
        } else if (newStatus == OrderStatus.CANCELLED) {
            paymentService.refundPayment(updatedOrder.getId(), "Отмена заказа: " + comment);
        }

        auditService.log(actorUsername, roleStr, "ORDER_STATUS_CHANGED",
                String.format("Заказ #%s: %s -> %s (%s)", updatedOrder.getOrderNumber(), currentStatus, newStatus, comment));

        return OrderResponse.fromEntity(updatedOrder);
    }

    private void validateStatusTransition(Order order, OrderStatus from, OrderStatus to, UserRole actorRole) {
        // Cancellation check
        if (to == OrderStatus.CANCELLED) {
            if (from != OrderStatus.NEW && from != OrderStatus.CONFIRMED) {
                throw new InvalidStatusTransitionException(String.format(
                        "Невозможно отменить заказ #%s в статусе [%s]. Отмена разрешена только в статусах [NEW, CONFIRMED] (до начала приготовления).",
                        order.getOrderNumber(), from));
            }
            return;
        }

        switch (from) {
            case NEW:
                if (to == OrderStatus.CONFIRMED) return;
                break;
            case CONFIRMED:
                if (to == OrderStatus.COOKING) return;
                break;
            case COOKING:
                if (to == OrderStatus.READY) return;
                break;
            case READY:
                if (order.getOrderType() == OrderType.DELIVERY && to == OrderStatus.DELIVERING) return;
                if (order.getOrderType() == OrderType.PICKUP && to == OrderStatus.COMPLETED) return;
                break;
            case DELIVERING:
                if (order.getOrderType() == OrderType.DELIVERY && to == OrderStatus.COMPLETED) return;
                break;
            case COMPLETED:
            case CANCELLED:
                throw new InvalidStatusTransitionException(String.format(
                        "Заказ #%s уже находится в конечном статусе [%s] и не может быть изменен.",
                        order.getOrderNumber(), from));
        }

        throw new InvalidStatusTransitionException(String.format(
                "Недопустимый переход статуса для заказа #%s (тип: %s): [%s] -> [%s].",
                order.getOrderNumber(), order.getOrderType(), from, to));
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, String actorUsername, UserRole actorRole, String reason) {
        return changeStatus(orderId, OrderStatus.CANCELLED, actorUsername, actorRole, reason);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ не найден"));
        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(Long userId, OrderStatus status, Pageable pageable) {
        Page<Order> orders;
        if (userId != null && status != null) {
            orders = orderRepository.findByUserIdAndStatus(userId, status, pageable);
        } else if (userId != null) {
            orders = orderRepository.findByUserId(userId, pageable);
        } else if (status != null) {
            orders = orderRepository.findByStatus(status, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }
        return orders.map(OrderResponse::fromEntity);
    }
}
