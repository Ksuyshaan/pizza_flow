# Соглашения проекта PizzaFlow

Решаем один раз для всех. Изменения вносим через pull request с согласия команды.

Пункты со статусом **предложение** ещё не согласованы: обсудить и заменить на «принято».

## 1. Типы данных

| Что | Правило |
|---|---|
| Идентификаторы | `Long` |
| Деньги | только `BigDecimal`; в БД `numeric(12,2)`; округление `HALF_UP` (предложение) |
| Время | `Instant`, хранение в UTC; в JSON ISO-8601 (`write-dates-as-timestamps: false`) |
| Enum | `@Enumerated(EnumType.STRING)`, никогда `ORDINAL` |
| Генерация id | `IDENTITY` (предложение) |

## 2. Именование

- Таблицы и колонки: `snake_case`, множественное число (`orders`, `order_items`). Слово `order` зарезервировано в SQL, поэтому `orders`.
- Пакеты: `com.pizzaflow.<модуль>`.
- Модули (предложение): `orders`, `menu`, `delivery`, `users`.
- Структура модуля (предложение): `controller`, `service`, `repository`, `entity`, `dto`.

## 3. DTO

- Входящие: `XxxRequest`, исходящие: `XxxResponse`.
- Сущности (`@Entity`) наружу не отдаём ни в каких случаях.
- Валидация через `jakarta.validation` на Request-классах.
- Маппинг: MapStruct (предложение).

## 4. Формат ошибки

Единый ответ, обрабатывается одним `@RestControllerAdvice`:

```json
{
  "timestamp": "2026-10-01T10:15:30Z",
  "status": 404,
  "error": "Not Found",
  "message": "Order 42 not found",
  "path": "/api/v1/orders/42"
}
```

Для ошибок валидации (предложение) добавляем необязательное поле `errors`: список `{field, message}`.

## 5. Пагинация

- Параметры: `page`, `size`, `sort=field,asc`.
- `page` с нуля (как в Spring Data).
- Максимальный `size`: 100 (предложение).
- Ответ: стандартный `Page` от Spring (предложение).

## 6. API (предложение)

- Префикс: `/api/v1`.
- URL: существительные во множественном числе (`/api/v1/orders`).
- Коды ответов: `200` чтение и обновление, `201` создание, `204` удаление, `400` ошибка валидации, `404` не найдено, `409` конфликт.

## 7. Git

**Ветки**

- `main`: только чистые, рабочие версии (сдача этапов). Прямые пуши запрещены.
- `develop`: текущая работа, сюда пушим актуальные версии.
- Feature-веток нет: работаем напрямую в `develop`.
- В `main` изменения попадают из `develop` только когда версия стабильна (конец этапа или контрольная точка).
- Перед пушем в `develop`: `git pull --rebase`, чтобы не плодить лишние merge-коммиты (предложение).
- Версии на сдачу этапов фиксируем тегами: `stage-1`, `stage-2` (предложение).

**Коммиты**: Conventional Commits, вид `type(scope): описание`.

- Типы: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`.
- Scope: имя модуля (`orders`, `menu`, `delivery`, `users`).
- Примеры: `feat(orders): add create order endpoint`, `docs: add conventions`.

## 8. Окружение (предложение)

- Java: _версия_
- Сборщик: _Maven / Gradle_
- СУБД: PostgreSQL
- Миграции: Flyway
- Профили: `local`, `test`
- Секреты: `.env` или `application-local.properties`, оба в `.gitignore`

## 9. Тесты и стиль (предложение)

- Тесты: JUnit 5 + Mockito; интеграционные: Testcontainers (или H2).
- Названия тестов: `methodName_condition_expectedResult`.
- Язык кода и коммитов: английский.
- Форматирование: единый форматтер, отступ 4 пробела.
- Документация API: Swagger/OpenAPI (springdoc).
