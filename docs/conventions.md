# Соглашения по разработке и стандарты кода (Conventions)

## 1. Типы данных и доменные правила
- **Идентификаторы (ID)**: строго `Long` (`bigint` в базе данных) с автоинкрементом (`GenerationType.IDENTITY`).
- **Денежные суммы и цены**: строго `BigDecimal` с округлением до 2 знаков после запятой (`RoundingMode.HALF_UP`). Запрещено использовать `double` и `float`.
- **Временные метки**: строго `java.time.Instant` (`TIMESTAMP WITH TIME ZONE`). Запрещено использовать `java.util.Date` и строковые даты в бизнес-логике.
- **Перечисления (Enum)**: сохраняются в базу данных как строковые литералы через `@Enumerated(EnumType.STRING)`.
- **Снепшоты данных**: в заказ сохраняются неизменяемые копии цен (`unitPrice`), названий блюд (`productNameSnapshot`) и адреса (`deliveryAddressSnapshot`), чтобы изменения в каталоге не искажали исторические заказы.

---

## 2. Именование и структура пакетов
- Базовый пакет: `com.pizzaflow`
- Модули:
  - `com.pizzaflow.common` — базовые исключения, конфигурации, утилиты, общие DTO.
  - `com.pizzaflow.security` — JWT аутентификация, фильтры, сервисы безопасности.
  - `com.pizzaflow.order` — модуль заказов, клиентов, адресов и промокодов (П1).
  - `com.pizzaflow.kitchen` — модуль меню, кухни, рецептов и склада (П2).
  - `com.pizzaflow.delivery` — модуль доставки, курьеров, оплат и зон (П3).
  - `com.pizzaflow.report` — модуль генерации отчётов Excel/Word на Apache POI (П3).
- Именование таблиц: `snake_case` во множественном числе (`orders`, `order_items`, `stock_movements`).
- Именование колонок: `snake_case` (`user_id`, `created_at`, `total_amount`).

---

## 3. DTO и формат взаимодействия REST API
- Сущности JPA (`@Entity`) никогда не возвращаются напрямую из контроллеров.
- Запросы: `XxxRequest` (например, `CreateOrderRequest`, `StockMovementRequest`).
- Ответы: `XxxResponse` (например, `OrderResponse`, `KitchenLoadResponse`).
- Пагинация: параметры `page` (с 0), `size` (по умолчанию 10 или 20), `sort=field,asc|desc`.
- Единый формат ответа об ошибке:
```json
{
  "timestamp": "2026-10-19T00:50:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Недостаточно ингредиента 'Сыр Моцарелла' на складе: требуется 200.0, доступно 50.0",
  "path": "/api/v1/kitchen/orders/12/start"
}
```

---

## 4. Соглашения по Git и версионированию
- Основные ветки:
  - `main` — стабильные релизы.
  - `develop` — текущая интеграционная ветка.
- Ветки задач:
  - `feature/stage1-<module>`
  - `feature/stage2-database`
  - `feature/orders-pricing`
- Формат сообщений коммитов (Conventional Commits):
  - `feat(orders): add pricing calculation with promo discount`
  - `feat(kitchen): implement recipe ingredient deduction with @Version lock`
  - `feat(delivery): add FIFO courier assignment queue`
  - `test(orders): add unit tests for state machine transitions`
  - `fix(inventory): handle optimistic locking conflict gracefully`
- Теги этапов: `stage-1`, `stage-2`, ..., `stage-7`.
