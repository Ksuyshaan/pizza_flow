# PizzaFlow

Система управления пиццерией: приём заказов, кухня, склад, доставка, промокоды и отчёты.
Бэкенд — Spring Boot (REST + JWT), фронтенд — одностраничное веб-приложение на чистом JavaScript, которое Spring отдаёт как статику.

## Возможности

| Модуль | Что умеет |
|---|---|
| **Заказы** | Каталог с поиском и фильтрами, корзина, доставка или самовывоз, промокоды, статусы заказа с проверкой переходов и историей |
| **Кухня** | Очередь нарядов, списание ингредиентов по рецептуре при старте готовки, расчёт нагрузки и времени ожидания |
| **Склад** | Остатки с оптимистической блокировкой (`@Version`), движения: приход, расход, корректировка, брак; контроль дефицита |
| **Доставка** | Зоны и тарифы, очередь курьеров FIFO, авто- и ручное назначение, «забрал» / «доставлено» |
| **Оплата** | Платёж на каждый заказ, автоматический возврат при отмене, ручной возврат менеджером |
| **Отчёты** | Выручка, средний чек, отмены, продажи по категориям; выгрузка в Excel (.xlsx) и отчёт смены в Word (.docx) через Apache POI |
| **Аудит** | Журнал ключевых действий пользователей |

Статусы заказа:

```
NEW → CONFIRMED → COOKING → READY → DELIVERING → COMPLETED   (доставка)
NEW → CONFIRMED → COOKING → READY → COMPLETED                (самовывоз)
NEW / CONFIRMED → CANCELLED                                   (только до начала готовки)
```

Подробнее: [docs/status-flow.md](docs/status-flow.md).

## Стек

- Java 21, Spring Boot 3.3 (Web, Data JPA, Security, Validation)
- JWT (jjwt 0.12)
- H2 в памяти по умолчанию, PostgreSQL через профиль `postgres`
- Apache POI 5 — генерация Excel и Word
- Фронтенд: HTML + CSS + ES-модули, без сборки

## Запуск

Нужна JDK 21+ (`JAVA_HOME` должен указывать на неё). Maven ставить не нужно: Maven Wrapper скачает его сам.

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Откройте http://localhost:8080.

Другой порт:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8091
```

### PostgreSQL

По умолчанию используется H2 в памяти: при каждом запуске база создаётся заново и заполняется демо-данными. Для PostgreSQL создайте базу `pizzaflow` и запустите с профилем:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Параметры подключения — в [application-postgres.yml](src/main/resources/application-postgres.yml).

H2-консоль: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:pizzaflowdb`, пользователь `sa`, пароль пустой).

### Тесты

```bash
./mvnw test
```

## Демо-аккаунты

Пароль у всех — `password123`. На экране входа есть кнопки быстрого входа.

| Роль | Email |
|---|---|
| Клиент | customer@pizzaflow.com |
| Повар | cook@pizzaflow.com |
| Курьер | courier@pizzaflow.com, courier2@pizzaflow.com |
| Менеджер | manager@pizzaflow.com |
| Администратор | admin@pizzaflow.com |

Промокоды: `WELCOME10` (−10%), `PIZZA20` (−20% от 800 ₽, не более 400 ₽).

Права ролей — в [docs/roles.md](docs/roles.md).

## Сквозной сценарий

1. **Клиент**: меню → корзина → оформить доставку с промокодом.
2. **Менеджер**: «Заказы» → «Подтвердить» — заказ уходит на кухню.
3. **Повар**: «Кухня» → «Начать готовить» (ингредиенты списываются со склада) → «Готово».
4. **Менеджер**: «Доставка» → назначить курьера автоматически (FIFO) или вручную.
5. **Курьер**: «Доставка» → «Забрал заказ» → «Доставлено» — заказ завершён, платёж проведён.
6. **Менеджер**: «Отчёты» — выручка за период, выгрузка в Excel или Word; «Аудит» — журнал событий.

## API

Основные эндпоинты (`/api/v1`, ответы в формате `{ success, message, data }`):

| Группа | Эндпоинты |
|---|---|
| Авторизация | `POST /auth/login`, `POST /auth/register`, `GET /auth/me` |
| Каталог | `GET/POST /categories`, `GET/POST /products`, `GET /products/{id}` |
| Заказы | `GET/POST /orders`, `GET /orders/{id}`, `GET /orders/{id}/history`, `POST /orders/{id}/confirm`, `/cancel`, `/complete-pickup` |
| Адреса | `GET/POST /addresses`, `DELETE /addresses/{id}` |
| Промокоды | `GET/POST /promocodes`, `POST /promocodes/validate` |
| Кухня | `GET /kitchen/orders`, `POST /kitchen/orders/{id}/start`, `/complete`, `GET /kitchen/load` |
| Склад | `GET /inventory/ingredients`, `GET /inventory/low-stock`, `GET/POST /inventory/movements` |
| Доставка | `GET/POST /delivery-zones`, `GET /couriers`, `GET /couriers/available` |
| Оплата | `GET /payments/order/{orderId}`, `POST /payments/{orderId}/refund` |
| Отчёты | `GET /reports/revenue`, `GET /reports/sales/excel`, `GET /reports/shift/word` |
| Аудит | `GET /audit/logs` |

Операции с доставками — на `/api/delivery` (без обёртки `ApiResponse`): `GET /api/delivery`, `POST /api/delivery/{id}/assign?courierId=`, `/auto-assign`, `/pickup`, `/complete`.

Ошибки возвращаются как `{ status, error, message, path, details }`. Полный контракт: [docs/api-contract.md](docs/api-contract.md).

## Структура

```
src/main/java/com/pizzaflow/
├── order/      заказы, адреса, промокоды, пользователи, авторизация
├── kitchen/    меню, рецептуры, кухня, склад
├── delivery/   зоны, курьеры, доставки, платежи
├── report/     отчёты (Excel/Word) и аудит
├── security/   JWT, UserDetails
├── config/     Security, демо-данные
└── common/     ApiResponse, обработка ошибок

src/main/resources/static/
├── index.html, styles.css
└── js/
    ├── app.js            вход, навигация по ролям
    ├── api.js            HTTP-клиент
    ├── state.js, ui.js   состояние и UI-утилиты
    └── views/            экраны: меню, корзина, заказы, кухня, склад, доставка, …

docs/   требования, роли, статусы, ER-диаграмма, контракт API, соглашения
```

## Известные ограничения

- Профили курьеров не создаются в демо-данных ([DataInitializer.java](src/main/java/com/pizzaflow/config/DataInitializer.java)): без них назначить доставку нельзя.
- JWT-секрет в `application.yml` — демонстрационный; для реального развёртывания передавайте его через переменную окружения.
