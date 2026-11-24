package com.pizzaflow.delivery.service;

import com.pizzaflow.common.exception.BadRequestException;
import com.pizzaflow.common.exception.ConflictException;
import com.pizzaflow.common.exception.ResourceNotFoundException;
import com.pizzaflow.delivery.dto.CourierResponse;
import com.pizzaflow.delivery.dto.DeliveryResponse;
import com.pizzaflow.delivery.dto.DeliveryZoneRequest;
import com.pizzaflow.delivery.dto.DeliveryZoneResponse;
import com.pizzaflow.delivery.entity.*;
import com.pizzaflow.delivery.repository.CourierRepository;
import com.pizzaflow.delivery.repository.DeliveryRepository;
import com.pizzaflow.delivery.repository.DeliveryZoneRepository;
import com.pizzaflow.order.entity.Order;
import com.pizzaflow.order.entity.OrderStatus;
import com.pizzaflow.order.entity.UserRole;
import com.pizzaflow.order.repository.OrderRepository;
import com.pizzaflow.order.service.OrderService;
import com.pizzaflow.report.service.AuditService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final CourierRepository courierRepository;
    private final DeliveryZoneRepository deliveryZoneRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final AuditService auditService;

    public DeliveryService(DeliveryRepository deliveryRepository,
                           CourierRepository courierRepository,
                           DeliveryZoneRepository deliveryZoneRepository,
                           OrderRepository orderRepository,
                           @Lazy OrderService orderService,
                           AuditService auditService) {
        this.deliveryRepository = deliveryRepository;
        this.courierRepository = courierRepository;
        this.deliveryZoneRepository = deliveryZoneRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.auditService = auditService;
    }

    @Transactional
    public DeliveryResponse createDelivery(Long orderId) {
        if (deliveryRepository.findByOrderId(orderId).isPresent()) {
            return DeliveryResponse.fromEntity(deliveryRepository.findByOrderId(orderId).get());
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ не найден: ID " + orderId));

        Delivery delivery = new Delivery(order, "Доставка по адресу: " + order.getDeliveryAddressSnapshot());
        Delivery saved = deliveryRepository.save(delivery);

        // Try immediate auto-assignment from FIFO courier queue
        tryAutoAssignCourier(saved);

        return DeliveryResponse.fromEntity(saved);
    }

    @Transactional
    public DeliveryResponse autoAssignCourier(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Доставка не найдена: ID " + deliveryId));
        tryAutoAssignCourier(delivery);
        return DeliveryResponse.fromEntity(delivery);
    }

    private boolean tryAutoAssignCourier(Delivery delivery) {
        // FIFO queue: who has been available the longest comes first!
        List<Courier> availableCouriers = courierRepository.findByIsAvailableTrueOrderByAvailableSinceAsc();
        if (availableCouriers.isEmpty()) {
            auditService.log("SYSTEM", "DELIVERY", "COURIER_QUEUE_EMPTY",
                    "Нет свободных курьеров для заказа #" + delivery.getOrder().getOrderNumber() + ". Заказ ожидает в очереди.");
            return false;
        }

        Courier courier = availableCouriers.get(0);
        courier.setAvailable(false);
        courier.setCurrentDeliveriesCount(1);
        courierRepository.save(courier);

        delivery.setCourier(courier);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setAssignedAt(Instant.now());
        deliveryRepository.save(delivery);

        auditService.log("SYSTEM", "DELIVERY", "COURIER_ASSIGNED_FIFO",
                String.format("Курьер %s (FIFO) назначен на заказ #%s",
                        courier.getUser().getFullName(), delivery.getOrder().getOrderNumber()));
        return true;
    }

    @Transactional
    public DeliveryResponse manualAssignCourier(Long deliveryId, Long courierId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Доставка не найдена: ID " + deliveryId));
        Courier courier = courierRepository.findById(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Курьер не найден: ID " + courierId));

        // Free previous courier if assigned
        if (delivery.getCourier() != null && !delivery.getCourier().getId().equals(courierId)) {
            Courier prev = delivery.getCourier();
            prev.setAvailable(true);
            prev.setCurrentDeliveriesCount(0);
            prev.setAvailableSince(Instant.now());
            courierRepository.save(prev);
        }

        courier.setAvailable(false);
        courier.setCurrentDeliveriesCount(1);
        courierRepository.save(courier);

        delivery.setCourier(courier);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setAssignedAt(Instant.now());
        Delivery saved = deliveryRepository.save(delivery);

        auditService.log("MANAGER", "DELIVERY", "COURIER_ASSIGNED_MANUAL",
                String.format("Курьер %s назначен вручную на заказ #%s",
                        courier.getUser().getFullName(), delivery.getOrder().getOrderNumber()));

        return DeliveryResponse.fromEntity(saved);
    }

    @Transactional
    public DeliveryResponse pickupDelivery(Long deliveryId, String courierUsername) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Доставка не найдена: ID " + deliveryId));

        if (delivery.getStatus() != DeliveryStatus.ASSIGNED) {
            throw new ConflictException("Заказ не может быть взят в доставку со статусом: " + delivery.getStatus());
        }

        delivery.setStatus(DeliveryStatus.IN_TRANSIT);
        delivery.setPickedUpAt(Instant.now());
        Delivery saved = deliveryRepository.save(delivery);

        // Update Order Status to DELIVERING
        orderService.changeStatus(delivery.getOrder().getId(), OrderStatus.DELIVERING, courierUsername, UserRole.COURIER, "Курьер выехал по адресу доставки");

        return DeliveryResponse.fromEntity(saved);
    }

    @Transactional
    public DeliveryResponse completeDelivery(Long deliveryId, String courierUsername) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Доставка не найдена: ID " + deliveryId));

        if (delivery.getStatus() != DeliveryStatus.IN_TRANSIT && delivery.getStatus() != DeliveryStatus.ASSIGNED) {
            throw new ConflictException("Невозможно завершить доставку в статусе: " + delivery.getStatus());
        }

        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setDeliveredAt(Instant.now());
        Delivery saved = deliveryRepository.save(delivery);

        // Free Courier and re-insert into FIFO queue with current timestamp
        if (delivery.getCourier() != null) {
            Courier courier = delivery.getCourier();
            courier.setAvailable(true);
            courier.setCurrentDeliveriesCount(0);
            courier.setAvailableSince(Instant.now());
            courierRepository.save(courier);

            // Check if another order is waiting for a courier
            List<Delivery> pendingDeliveries = deliveryRepository.findByStatus(DeliveryStatus.PENDING_ASSIGNMENT);
            if (!pendingDeliveries.isEmpty()) {
                tryAutoAssignCourier(pendingDeliveries.get(0));
            }
        }

        // Update Order Status to COMPLETED
        orderService.changeStatus(delivery.getOrder().getId(), OrderStatus.COMPLETED, courierUsername, UserRole.COURIER, "Заказ успешно доставлен и вручен клиенту");

        return DeliveryResponse.fromEntity(saved);
    }

    // Zone & Courier Listing
    @Transactional(readOnly = true)
    public List<DeliveryZoneResponse> getActiveZones() {
        return deliveryZoneRepository.findByActiveTrue().stream()
                .map(DeliveryZoneResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeliveryZoneResponse createZone(DeliveryZoneRequest req) {
        if (deliveryZoneRepository.findByNameIgnoreCase(req.getName()).isPresent()) {
            throw new ConflictException("Зона с названием '" + req.getName() + "' уже существует");
        }
        DeliveryZone zone = new DeliveryZone(req.getName(), req.getDeliveryFee(), req.getFreeDeliveryThreshold(), req.getEstimatedMinutes());
        return DeliveryZoneResponse.fromEntity(deliveryZoneRepository.save(zone));
    }

    @Transactional(readOnly = true)
    public List<CourierResponse> getAllCouriers() {
        return courierRepository.findAll().stream()
                .map(CourierResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourierResponse> getAvailableCouriersFifo() {
        return courierRepository.findByIsAvailableTrueOrderByAvailableSinceAsc().stream()
                .map(CourierResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getDeliveries(DeliveryStatus status, Long courierUserId) {
        List<Delivery> list;
        if (courierUserId != null) {
            Courier courier = courierRepository.findByUserId(courierUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Профиль курьера не найден"));
            if (status != null) {
                list = deliveryRepository.findByCourierIdAndStatusIn(courier.getId(), List.of(status));
            } else {
                list = deliveryRepository.findByCourierId(courier.getId());
            }
        } else if (status != null) {
            list = deliveryRepository.findByStatus(status);
        } else {
            list = deliveryRepository.findAll();
        }
        return list.stream()
                .map(DeliveryResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
