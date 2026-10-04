# Контракт между модулями

Только сигнатуры и правила. Тела методов пишет владелец модуля. Приложение на JavaFX + Spring Boot: экраны вызывают сервисы напрямую, HTTP нет.

## 1. Кто за что отвечает

| | Модуль / пакет | Экраны | Сервисы |
|---|---|---|---|
| **П1 Ксюша** | `orders`, `auth`, `customers`, `export` | Авторизация, Заказы, Новый заказ, Клиент | `AuthService`, `OrderService`, `PromoService`, `ExportService` (Apache POI) |
| **П2 Алина** | `menu`, `kitchen`, база данных | Меню, Кухня | `MenuService`, `KitchenService`, `InventoryService` |
| **П3 Алия** | `delivery`, `reports`, `common` | Dashboard, Курьеры, Доставка, Отчёты | `DeliveryService`, `CourierService`, `DashboardService`, `ReportService`, `AuditLogger` |

Дополнительно: Алина ведёт настройки БД и тестовые данные (пользователь на каждую роль, меню, зоны, курьеры), Алия отвечает за исключения, Log4j 2 и тесты, Ксюша за главное окно с меню по ролям.

## 2. Правила

1. Статус заказа меняется **только** через `OrderService.changeStatus`.
2. Чужие репозитории не вызываем. Нужны данные соседа, вызываем его сервис.
3. Между модулями передаём DTO (`XxxResponse`), а не сущности. Исключение: `@ManyToOne` на `Order` из `KitchenOrder` и `Delivery` разрешён (внешний ключ в БД и удобные запросы), но менять `Order` можно только через `OrderService`.
4. Соседи реагируют на смену статуса через событие, а не вызываются из `OrderService` (так нет циклических зависимостей).
5. Ошибки бизнес-логики это исключения из `common.exception`; интерфейс показывает их в Alert.

## 3. Общие типы (пакет `common`, владелец Алия, нужны в первый день)

```java
public record Actor(Long userId, Role role) {}
public enum Role { ADMIN, MANAGER, COOK, COURIER, CUSTOMER }
public enum OrderStatus { NEW, CONFIRMED, COOKING, READY, DELIVERING, COMPLETED, CANCELLED }
public enum OrderType { DELIVERY, PICKUP }
public enum PaymentMethod { CASH, CARD }

public class BusinessException extends RuntimeException {}               // базовый, сообщение на русском
public class OrderNotFoundException extends BusinessException {}
public class InvalidStatusTransitionException extends BusinessException {}
public class ForbiddenActionException extends BusinessException {}
public class MenuItemUnavailableException extends BusinessException {}
public class InsufficientStockException extends BusinessException {}     // Алина

public interface AuditLogger {
    void log(Actor actor, String action, String details);                // в audit.log (Log4j 2)
}
```

Один хелпер `AlertUtil.showError(Throwable)` (Алия) показывает `BusinessException` как есть, остальные ошибки как «Что-то пошло не так» плюс запись в `error.log`.

## 4. Событие смены статуса

Публикует `OrderService.changeStatus` в конце той же транзакции (`@EventListener`, не `@TransactionalEventListener`): ошибка слушателя откатывает смену статуса.

```java
public record OrderStatusChangedEvent(
        Long orderId, OrderStatus from, OrderStatus to, OrderType type, Actor actor, Instant at) {}
```

Слушатели:

- **Алина:** `to == CONFIRMED` создаёт `KitchenOrder`; `COOKING` списывает ингредиенты и записывает повара; `READY` записывает `finishedAt`; `CANCELLED` убирает из очереди.
- **Алия:** `READY` и `type == DELIVERY` создаёт `Delivery` и назначает курьера; `DELIVERING` и `COMPLETED` обновляют статус доставки.

Из-за этого кухонный экран тоже вызывает просто `OrderService.changeStatus(id, COOKING, actor)`, а экран курьера `changeStatus(id, DELIVERING, actor)`. Отдельных «start» и «complete» у соседей нет.

## 5. Интерфейсы

### П1 Ксюша

```java
public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request, Actor actor);   // считает суммы, пишет StatusHistory (null → NEW)
    OrderResponse changeStatus(Long orderId, OrderStatus newStatus, Actor actor);
    OrderResponse getOrder(Long orderId);
    Page<OrderResponse> search(OrderFilter filter, Pageable pageable);    // для таблицы заказов, кухни, доставки

    // Для Алии: dashboard и отчёты
    OrderStats getStats(Instant from, Instant to);
    List<OrderReportView> findOrdersForReport(Instant from, Instant to, Set<OrderStatus> statuses);
    OrderReportView getOrderForReport(Long orderId);
}

public record CreateOrderRequest(Long customerId, OrderType orderType, PaymentMethod paymentMethod,
        List<OrderItemRequest> items, String promoCode, String deliveryAddress, Long deliveryZoneId) {}
public record OrderItemRequest(Long menuItemId, int quantity) {}

public record OrderStats(long ordersCount, BigDecimal revenue, BigDecimal averageCheck,
        Map<OrderStatus, Long> byStatus, List<DayRevenue> revenueByDay) {}
public record DayRevenue(LocalDate day, BigDecimal revenue) {}

public interface ExportService {                                          // Apache POI
    void exportSalesXlsx(List<OrderReportView> orders, Path target);
    void exportShiftDocx(ShiftReport report, Path target);
    void exportOrderDocx(OrderReportView order, Path target);
}
```

`OrderReportView` и `ShiftReport` это DTO для отчётов: номер, дата, тип, статус, клиент, позиции, суммы, курьер (для смены: период, число заказов, выручка, отмены). Поля Ксюша и Алия уточняют друг с другом при реализации.

**Определения метрик** (чтобы цифры на Dashboard и в отчётах совпадали):
- Выручка = сумма `totalAmount` заказов в статусе `COMPLETED` за период (по `completedAt`).
- Заказов за период = все заказы, кроме `CANCELLED` (по `createdAt`).
- Средний чек = выручка / число `COMPLETED`.

### П2 Алина

```java
public interface MenuService {
    MenuItemResponse getItem(Long id);                                    // цена и доступность
    List<MenuItemResponse> getItems(Collection<Long> ids);
    List<MenuItemResponse> getAvailableItems();                           // для экрана «Новый заказ»
}

public interface KitchenService {
    KitchenOrderResponse createKitchenOrder(Long orderId);                // вызывается слушателем события
    void cancelKitchenOrder(Long orderId);                                // вызывается слушателем события
    KitchenQueue getQueue();                                              // колонки: новые, готовятся, готовы
}
```

### П3 Алия

```java
public interface DeliveryService {
    DeliveryResponse createDelivery(Long orderId);                        // вызывается слушателем события, назначает курьера
    List<DeliveryZoneResponse> getZones();                                // для экрана «Новый заказ»
    BigDecimal getDeliveryFee(Long zoneId);                               // нужно OrderService при расчёте суммы
}
```

`CourierService`, `DashboardService`, `ReportService` используются только экранами Алии. `ReportService` собирает данные через `OrderService.findOrdersForReport` и передаёт их в `ExportService` Ксюши.

## 6. Кто кого вызывает

```
Экраны (все)        → OrderService.changeStatus / search / getOrder
OrderService        → MenuService.getItems, DeliveryService.getDeliveryFee, PromoService
Алина (слушатель)   ← OrderStatusChangedEvent
Алия (слушатель)    ← OrderStatusChangedEvent
Алия                → OrderService.getStats / findOrdersForReport, ExportService
```

Больше никаких перекрёстных вызовов.

## 7. С чего начать (первый день после встречи)

Каждый коммитит в `develop` интерфейсы с заглушками, возвращающими тестовые данные, чтобы остальные сразу могли компилироваться и работать:

1. **Алия:** пакет `common` (исключения, `Actor`, `Role`, enum'ы, `AuditLogger`, настройка Log4j 2).
2. **Ксюша:** сущности `Order`, `OrderItem`, `StatusHistory`, `OrderService` со всеми методами (рабочий `changeStatus`, остальное заглушки), событие.
3. **Алина:** подключение к БД, тестовые данные, `MenuService` и `KitchenService` с заглушками, слушатель события.

## 8. Главное окно

Каркас с боковым меню и показом пунктов по роли делает Ксюша (она же ведёт авторизацию). Каждый владелец экрана добавляет свой экран (FXML + контроллер) и регистрирует его одной строкой в списке экранов с нужными ролями.
