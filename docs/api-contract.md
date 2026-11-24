# API Контракт и межмодульные интерфейсы PizzaFlow

## 1. Межмодульные сервисные интерфейсы (Java Contracts)

В рамках монолита модули взаимодействуют через сервисные интерфейсы без прямого доступа к чужим JPA-репозиториям:

```java
// Модуль 1: Заказы (П1)
package com.pizzaflow.order.service;

public interface OrderService {
    // Единственная точка смены статуса и записи в StatusHistory
    OrderResponse changeStatus(Long orderId, OrderStatus newStatus, String actorUsername, UserRole actorRole, String comment);
    
    // Получение данных для смежных модулей и отчётов
    OrderDto getOrderById(Long orderId);
    List<OrderReportDto> getOrdersForPeriod(Instant from, Instant to);
}

// Модуль 2: Кухня и склад (П2)
package com.pizzaflow.kitchen.service;

public interface KitchenService {
    KitchenOrderResponse createKitchenOrder(Long orderId);
    KitchenOrderResponse startCooking(Long kitchenOrderId, String cookUsername);
    KitchenOrderResponse completeCooking(Long kitchenOrderId);
    KitchenLoadResponse getKitchenLoad();
}

// Модуль 3: Доставка и оплата (П3)
package com.pizzaflow.delivery.service;

public interface DeliveryService {
    DeliveryResponse createDelivery(Long orderId);
    DeliveryResponse autoAssignCourier(Long deliveryId);
    DeliveryResponse manualAssignCourier(Long deliveryId, Long courierId);
    DeliveryResponse pickupDelivery(Long deliveryId, String courierUsername);
    DeliveryResponse completeDelivery(Long deliveryId, String courierUsername);
}
```

---

## 2. Каталог REST Endpoints

Все пути имеют префикс `/api/v1`.

### Модуль 1: Авторизация, клиенты и заказы (Участник 1)

| Метод | Путь | Роли | Описание | Request Body | Response Body |
|---|---|---|---|---|---|
| `POST` | `/auth/register` | ALL | Регистрация клиента | `RegisterRequest` | `AuthResponse (JWT, User)` |
| `POST` | `/auth/login` | ALL | Вход в систему | `LoginRequest` | `AuthResponse (JWT, User)` |
| `GET` | `/auth/me` | AUTH | Получение текущего профиля | - | `UserResponse` |
| `GET` | `/addresses` | CUSTOMER, ADMIN | Список адресов пользователя | - | `List<AddressResponse>` |
| `POST` | `/addresses` | CUSTOMER, ADMIN | Добавление адреса | `CreateAddressRequest` | `AddressResponse` |
| `DELETE` | `/addresses/{id}` | CUSTOMER, ADMIN | Удаление адреса | - | `204 No Content` |
| `POST` | `/orders` | CUSTOMER, ADMIN | Создание заказа (корзина, адрес, промо) | `CreateOrderRequest` | `OrderResponse` |
| `GET` | `/orders` | ALL (фильтр по роли) | Список заказов (клиент видит свои, менеджер все) | Query: `status, page, size` | `PageResponse<OrderResponse>` |
| `GET` | `/orders/{id}` | AUTH | Детализация заказа с позициями | - | `OrderResponse` |
| `POST` | `/orders/{id}/confirm` | MANAGER, ADMIN | Подтверждение заказа менеджером | - | `OrderResponse` |
| `POST` | `/orders/{id}/cancel` | CUSTOMER, MANAGER, ADMIN | Отмена заказа (до COOKING) | `CancelOrderRequest` | `OrderResponse` |
| `GET` | `/orders/{id}/history` | AUTH | История переходов статусов | - | `List<StatusHistoryResponse>` |
| `POST` | `/promocodes/validate` | AUTH | Предварительная проверка промокода | `ValidatePromoRequest` | `PromoValidationResponse` |
| `GET` | `/promocodes` | MANAGER, ADMIN | Список всех промокодов | - | `List<PromoCodeResponse>` |
| `POST` | `/promocodes` | MANAGER, ADMIN | Создание нового промокода | `CreatePromoCodeRequest` | `PromoCodeResponse` |

---

### Модуль 2: Меню, кухня и склад (Участник 2)

| Метод | Путь | Роли | Описание | Request Body | Response Body |
|---|---|---|---|---|---|
| `GET` | `/categories` | ALL | Список активных категорий меню | - | `List<CategoryResponse>` |
| `POST` | `/categories` | MANAGER, ADMIN | Создание категории | `CategoryRequest` | `CategoryResponse` |
| `GET` | `/products` | ALL | Каталог продуктов с фильтром по цене и категории | Query: `categoryId, search, page, size` | `PageResponse<ProductResponse>` |
| `GET` | `/products/{id}` | ALL | Детали продукта с составом | - | `ProductResponse` |
| `POST` | `/products` | MANAGER, ADMIN | Создание продукта с рецептом | `CreateProductRequest` | `ProductResponse` |
| `PUT` | `/products/{id}` | MANAGER, ADMIN | Обновление продукта | `UpdateProductRequest` | `ProductResponse` |
| `GET` | `/kitchen/orders` | COOK, MANAGER, ADMIN | Очередь нарядов на кухне | Query: `status` | `List<KitchenOrderResponse>` |
| `POST` | `/kitchen/orders/{id}/start` | COOK, MANAGER, ADMIN | Начать готовку (списание остатков) | - | `KitchenOrderResponse` |
| `POST` | `/kitchen/orders/{id}/complete`| COOK, MANAGER, ADMIN | Завершить готовку | - | `KitchenOrderResponse` |
| `GET` | `/kitchen/load` | ALL | Расчет загрузки кухни и ожидаемого времени | - | `KitchenLoadResponse` |
| `GET` | `/inventory/ingredients` | COOK, MANAGER, ADMIN | Список сырья и остатков | - | `List<IngredientResponse>` |
| `GET` | `/inventory/low-stock` | MANAGER, ADMIN | Оповещение об остатках ниже порога | - | `List<IngredientResponse>` |
| `POST` | `/inventory/movements` | MANAGER, ADMIN | Складская операция (приход/списание/корректировка) | `StockMovementRequest` | `StockMovementResponse` |
| `GET` | `/inventory/movements` | MANAGER, ADMIN | Журнал складских проводок | Query: `ingredientId, type` | `List<StockMovementResponse>` |

---

### Модуль 3: Доставка, курьеры, оплата и отчёты (Участник 3)

| Метод | Путь | Роли | Описание | Request Body | Response Body |
|---|---|---|---|---|---|
| `GET` | `/delivery-zones` | ALL | Список тарифных зон доставки | - | `List<DeliveryZoneResponse>` |
| `POST` | `/delivery-zones` | MANAGER, ADMIN | Создание/обновление зоны | `DeliveryZoneRequest` | `DeliveryZoneResponse` |
| `GET` | `/couriers` | MANAGER, ADMIN | Список всех курьеров со статусами | - | `List<CourierResponse>` |
| `GET` | `/couriers/available` | MANAGER, ADMIN | Очередь свободных курьеров (FIFO) | - | `List<CourierResponse>` |
| `GET` | `/deliveries` | COURIER, MANAGER, ADMIN | Список доставок (курьер видит свои, менеджер все) | Query: `status` | `List<DeliveryResponse>` |
| `POST` | `/deliveries/{id}/assign` | MANAGER, ADMIN | Ручное назначение курьера | `AssignCourierRequest` | `DeliveryResponse` |
| `POST` | `/deliveries/{id}/pickup` | COURIER, MANAGER, ADMIN | Взятие заказа на доставку курьером | - | `DeliveryResponse` |
| `POST` | `/deliveries/{id}/complete` | COURIER, MANAGER, ADMIN | Завершение доставки (вручение) | - | `DeliveryResponse` |
| `POST` | `/orders/{id}/complete-pickup`| MANAGER, ADMIN | Завершение заказа самовывоза | - | `OrderResponse` |
| `GET` | `/payments/order/{orderId}` | AUTH | Просмотр статуса оплаты заказа | - | `PaymentResponse` |
| `POST` | `/payments/{id}/refund` | MANAGER, ADMIN | Возврат денежных средств | `RefundRequest` | `PaymentResponse` |
| `GET` | `/reports/revenue` | MANAGER, ADMIN | Метрики выручки за выбранный период | Query: `startDate, endDate` | `RevenueReportResponse` |
| `GET` | `/reports/sales/excel` | MANAGER, ADMIN | Скачивание отчёта по продажам в Excel (.xlsx) | Query: `startDate, endDate` | `Binary (.xlsx)` |
| `GET` | `/reports/shift/word` | MANAGER, ADMIN | Скачивание сводного отчёта смены в Word (.docx) | Query: `date` | `Binary (.docx)` |
| `GET` | `/audit/logs` | MANAGER, ADMIN | Просмотр системных событий аудита | - | `List<AuditLogResponse>` |
