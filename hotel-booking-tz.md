# Hotel Booking Platform — Техническое задание

> Микросервисная архитектура · Java · Spring Boot 3

---

## 0. Контекст и цели

Исходное тестовое задание — монолитный Spring Boot сервис бронирования номеров в отеле (Kotlin, JDBC, Kafka producer, Micrometer). Цель расширения — превратить его в реальную многосервисную платформу, охватывающую весь стек технологий, активно используемых в промышленной разработке на JVM.

**Ключевые цели:**

- Построить полноценную систему, а не учебный набор заданий — каждый компонент должен решать реальную продуктовую задачу.
- Максимальное покрытие актуального стека: JPA, Security, Redis, Kafka consumers, Observability, CI/CD, Testcontainers.
- Масштабируемость домена: несколько отелей, тарифы, платежи, отзывы.
- Портфолио-проект: читаемый код, документация, работающий Docker Compose, GitHub Actions.

---

## 1. Архитектура системы

### 1.1 Сервисы

Система состоит из пяти самостоятельных Spring Boot приложений и общей инфраструктуры. Каждый сервис владеет своей схемой БД и общается с другими исключительно через Kafka (async) или REST (sync — только там, где нужна немедленная консистентность).

| Сервис | Ответственность | Основные технологии |
|---|---|---|
| `booking-service` | Управление отелями, номерами, бронированиями, поиск доступности | Kotlin, Spring Boot, JPA, PostgreSQL, Kafka, Redis |
| `auth-service` | Регистрация, аутентификация, выдача JWT, управление пользователями и ролями | Kotlin, Spring Security, JWT, PostgreSQL, Redis (token blacklist) |
| `payment-service` | Создание платежей, обработка статусов, возвраты, история транзакций | Kotlin, Spring Boot, JPA, PostgreSQL, Kafka |
| `notification-service` | Отправка уведомлений (email/SMS-mock) по Kafka-событиям от других сервисов | Kotlin, Spring Boot, Kafka Consumer, PostgreSQL |
| `review-service` | Отзывы и рейтинги гостей на отели и номера, агрегация оценок | Kotlin, Spring Boot, JPA, PostgreSQL, Redis (рейтинг-кэш) |

### 1.2 Инфраструктура

| Компонент | Назначение |
|---|---|
| PostgreSQL (отдельная БД на сервис) | Персистентность. Каждый сервис работает только со своей схемой. |
| Apache Kafka | Асинхронная межсервисная коммуникация. Все доменные события (booking, payment, review) передаются через топики. |
| Redis | Кэширование горячих данных (доступность номеров, рейтинги), blacklist JWT refresh-токенов, distributed lock при бронировании. |
| Prometheus + Grafana | Метрики приложений. Дашборды: RPS, latency p99, Kafka consumer lag, JVM. |
| Zipkin / OpenTelemetry | Distributed tracing: сквозной trace-id от REST-запроса через Kafka до всех downstream сервисов. |
| Docker Compose | Локальный запуск полного стека одной командой. Все сервисы + инфраструктура. |
| GitHub Actions | CI: lint → unit tests → integration tests → build Docker images. |

### 1.3 Взаимодействие сервисов

Синхронные вызовы (REST / Spring WebClient) допустимы только для `booking-service → auth-service` (валидация токена) и `review-service → booking-service` (проверка что гость действительно проживал). Всё остальное — через Kafka события.

| Событие (топик) | Producer | Consumer(s) |
|---|---|---|
| `booking.created` | booking-service | payment-service, notification-service |
| `booking.cancelled` | booking-service | payment-service, notification-service |
| `booking.completed` | booking-service | review-service (открыть возможность оставить отзыв), notification-service |
| `payment.confirmed` | payment-service | booking-service (перевести в PAID), notification-service |
| `payment.failed` | payment-service | booking-service (отменить бронирование), notification-service |
| `review.created` | review-service | booking-service (обновить avg rating отеля/номера в кэше) |

---

## 2. Доменная модель

### 2.1 booking-service

Сервис расширяется от одного отеля до платформы с несколькими отелями. Появляются сущности `Hotel`, `RoomType`, `Tariff`. Номер теперь связан с типом номера, а тип — с тарифами.

| Сущность | Ключевые поля | Примечание |
|---|---|---|
| `Hotel` | id, name, address, city, starRating, description, amenities (jsonb), avgRating, isActive | Отель может быть деактивирован (скрыт из поиска) |
| `RoomType` | id, hotelId, name, description, capacity, amenities (jsonb), photos (jsonb) | Категория номера: стандарт, делюкс, сьют и т.д. |
| `Room` | id, roomTypeId, roomNumber, floor, status (AVAILABLE/MAINTENANCE/OUT_OF_SERVICE) | Конкретный физический номер. Статус независим от бронирований. |
| `Tariff` | id, roomTypeId, name, pricePerNight, currency, validFrom, validTo, conditions | Несколько тарифов на тип номера: ранняя броня, невозвратный, стандарт. |
| `Guest` | id, userId, firstName, surname, patronymic, birthDate, phoneNumber, passportNumber | Привязан к пользователю из auth-service. passportNumber — зашифровано. |
| `Booking` | id, guestId, roomId, tariffId, checkIn, checkOut, status, totalPrice, currency, notes | Статусы: `PENDING_PAYMENT → CONFIRMED → CHECKED_IN → COMPLETED / CANCELLED` |
| `BookingStatusHistory` | id, bookingId, status, changedAt, changedBy, reason | Полная история смены статусов. Нужна для аудита и диспутов по платежам. |

Поиск доступности: выбрать все `RoomType` для отеля → для каждого типа найти хотя бы один свободный `Room` в заданный период → вернуть тип с минимальной ценой действующего тарифа. Кэшировать результат в Redis на 1 минуту с ключом по параметрам поиска.

### 2.2 payment-service

| Сущность | Ключевые поля | Примечание |
|---|---|---|
| `Payment` | id, bookingId, guestId, amount, currency, status, provider, providerPaymentId, createdAt, updatedAt | Статусы: `PENDING → PROCESSING → CONFIRMED / FAILED / REFUNDED` |
| `PaymentAttempt` | id, paymentId, attemptNumber, status, errorCode, errorMessage, attemptedAt | История попыток оплаты. Нужна для повторных попыток и отладки. |
| `Refund` | id, paymentId, amount, reason, status, createdAt, processedAt | Возврат может быть частичным (early checkout) или полным (отмена). |

Платёжный провайдер мокируется: сервис принимает запрос, через configurable задержку (1–3 сек) публикует `payment.confirmed` или `payment.failed` с вероятностью, настраиваемой через `application.yaml`. Позволяет тестировать весь event flow без реального провайдера.

### 2.3 review-service

| Сущность | Ключевые поля | Примечание |
|---|---|---|
| `Review` | id, bookingId, guestId, hotelId, roomTypeId, overallRating, cleanlinessRating, serviceRating, locationRating, valueRating, title, body, createdAt, isVerified | isVerified = true если бронирование есть в eligible_bookings |
| `ReviewReply` | id, reviewId, authorId, body, createdAt | Ответ администратора отеля на отзыв гостя |
| `RatingAggregate` | id, targetType (HOTEL/ROOM_TYPE), targetId, avgRating, totalReviews, ratingDistribution (jsonb) | Денормализованная агрегация. Обновляется при каждом новом review через Kafka. |
| `EligibleBooking` | id, bookingId, guestId, hotelId, roomTypeId, createdAt | Заполняется при получении booking.completed из Kafka. Используется для проверки права на отзыв. |

Гость может оставить отзыв только после завершения проживания. Один отзыв на одно бронирование. При создании отзыва `review-service` проверяет наличие записи в `eligible_bookings` по `bookingId` — без синхронного вызова в `booking-service`.
### 2.4 auth-service

| Сущность | Ключевые поля | Примечание |
|---|---|---|
| `User` | id, email, passwordHash, role, isActive, createdAt, lastLoginAt | Роли: `ADMIN`, `HOTEL_MANAGER`, `GUEST`. Один пользователь = одна роль. |
| `RefreshToken` | id, userId, tokenHash, expiresAt, isRevoked, createdAt | При logout — помечается revoked, заносится в Redis blacklist. |
| `AuditLog` | id, userId, action, ipAddress, userAgent, createdAt | Фиксирует login, logout, смену пароля, попытки доступа к чужим ресурсам. |

---

## 3. Функциональные требования

Все эндпоинты требуют JWT, если не указано иное (`—`).

### 3.1 booking-service

| Метод | Путь | Роль                                      | Описание                                                                                                                                             |
|---|---|-------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| GET | `/hotels` | ANY                                       | Список отелей с фильтрами (город, starRating, amenities). Пагинация. Сортировка по avgRating, цене.                                                  |
| GET | `/hotels/{id}` | ANY                                       | Детальная информация об отеле с RoomType и текущими минимальными ценами.                                                                             |
| POST | `/hotels` | ADMIN                                     | Создать отель.                                                                                                                                       |
| PUT | `/hotels/{id}` | ADMIN, HOTEL_MANAGER                      | Обновить данные отеля.                                                                                                                               |
| POST | `/hotels/{id}/activate` | ADMIN                                     | Активировать отель.                                                                                                                                  |
| POST | `/hotels/{id}/deactivate` | ADMIN                                     | Деактивировать отель.                                                                                                                                |
| GET | `/hotels/{id}/availability` | ANY                                       | Поиск доступных типов номеров на заданные даты. checkIn, checkOut в query params. Кэшируется в Redis.                                                |
| POST | `/hotels/{id}/room-types` | ADMIN, HOTEL_MANAGER                      | Добавить тип номера в отель.                                                                                                                         |
| GET | `/room-types/{id}/rooms` | ADMIN, HOTEL_MANAGER                      | Список номеров типа.                                                                                                                                 |
| POST | `/room-types/{id}/rooms` | ADMIN, HOTEL_MANAGER                      | Добавить физический номер к типу.                                                                                                                    |
| PUT | `/rooms/{id}/status` | ADMIN, HOTEL_MANAGER                      | Изменить статус номера (AVAILABLE/MAINTENANCE/OUT_OF_SERVICE).                                                                                       |
| POST | `/room-types/{id}/tariffs` | ADMIN, HOTEL_MANAGER                      | Добавить тариф на тип номера.                                                                                                                        |
| GET | `/room-types/{id}/tariffs` | ANY                                       | Список актуальных тарифов для типа номера.                                                                                                           |
| POST | `/bookings` | GUEST                                     | Создать бронирование (guestId, roomTypeId, tariffId, checkIn, checkOut). Ответ: bookingId + ссылка на оплату. Distributed lock на roomTypeId+период. |
| GET | `/bookings/{id}` | GUEST (свои), ADMIN, MANAGER              | Детали бронирования с историей статусов.                                                                                                             |
| GET | `/bookings` | ADMIN, HOTEL_MANAGER                      | Список бронирований с фильтрами (hotelId, status, dateRange). Пагинация.                                                                             |
| POST | `/guests` | GUEST, ADMIN, HOTEL_MANAGER (каждый свой) | Добавить информацию о профиле гостя.                                                                                                                 |
| PUT | `/guests/me` | GUEST, ADMIN, HOTEL_MANAGER (каждый свой)                              | Обновить информацию о госте. |
| GET | `/guests/{guestId}/bookings` | GUEST (свой), ADMIN                       | История бронирований гостя.                                                                                                                          |
| POST | `/bookings/{id}/cancel` | GUEST (свой), ADMIN                       | Отменить бронирование. Инициирует refund если payment.status = CONFIRMED.                                                                            |
| POST | `/bookings/{id}/check-in` | ADMIN, HOTEL_MANAGER                      | Отметить заезд гостя.                                                                                                                                |
| POST | `/bookings/{id}/check-out` | ADMIN, HOTEL_MANAGER                      | Отметить выезд. Booking переходит в COMPLETED. Публикует booking.completed.                                                                          |
| GET | `/audit/bookings/{id}` | ADMIN                                     | История событий из event_audit по bookingId.                                                                                                         |
### 3.2 auth-service

| Метод | Путь | Роль | Описание |
|---|---|---|---|
| POST | `/auth/register` | — | Регистрация. Email + password + role. Возвращает userId. |
| POST | `/auth/login` | — | Логин. Возвращает accessToken (15 мин) + refreshToken (7 дней). |
| POST | `/auth/refresh` | — | Обновление accessToken по refreshToken. |
| POST | `/auth/logout` | ANY | Ревокация refreshToken. Заносит в Redis blacklist. |
| GET | `/users/me` | ANY | Данные текущего пользователя. |
| PUT | `/users/me/password` | ANY | Смена пароля. Ревокация всех refresh токенов пользователя. |
| GET | `/users` | ADMIN | Список пользователей с фильтрами. |
| PUT | `/users/{id}/role` | ADMIN | Изменить роль пользователя. |
| POST | `/users/{id}/deactivate` | ADMIN | Деактивировать аккаунт. |
| GET | `/users/audit` | ADMIN | Лог действий с фильтрами по userId, action, dateRange. |

### 3.3 payment-service

| Метод | Путь | Роль | Описание |
|---|---|---|---|
| POST | `/payments` | GUEST | Инициировать оплату бронирования |
| GET | `/payments/{id}` | GUEST, ADMIN | Статус платежа |
| GET | `/payments/booking/{bookingId}` | GUEST, ADMIN | Платёж по бронированию |
| GET | `/payments` | ADMIN | Список платежей с фильтрами |
| POST | `/payments/{id}/refund` | GUEST, ADMIN | Запрос возврата |
| POST | `/payments/{id}/retry` | GUEST | Повторная попытка оплаты |
| GET | `/refunds/{id}` | GUEST, ADMIN | Статус возврата |
| POST | `/refunds/{id}/retry` | GUEST | Повторная попытка возврата |

### 3.4 review-service

| Метод | Путь | Роль | Описание |
|---|---|---|---|
| POST | `/reviews` | GUEST | Создать отзыв. bookingId + оценки (1–5) + текст. Проверяет наличие в eligible_bookings. |
| GET | `/reviews/{id}` | ANY | Конкретный отзыв. |
| GET | `/hotels/{hotelId}/reviews` | ANY | Отзывы об отеле. Пагинация, сортировка по дате / рейтингу. |
| GET | `/room-types/{id}/reviews` | ANY | Отзывы на тип номера. Пагинация, сортировка по дате / рейтингу. |
| GET | `/hotels/{hotelId}/rating` | ANY | Агрегированный рейтинг отеля (из кэша Redis). |
| POST | `/reviews/{id}/response` | HOTEL_MANAGER, ADMIN | Ответить на отзыв. |
| DELETE | `/reviews/{id}` | ADMIN | Удалить отзыв (модерация). |
### 3.5 notification-service

Сервис не имеет публичного REST API (только Actuator). Работает исключительно как Kafka consumer.

| Событие | Действие | Получатель |
|---|---|---|
| `booking.created` | Подтверждение бронирования с деталями и ссылкой на оплату | Guest (email) |
| `payment.confirmed` | Уведомление об успешной оплате. Ваучер. | Guest (email) |
| `payment.failed` | Уведомление о неудачной оплате с инструкцией повторной попытки | Guest (email) |
| `booking.cancelled` | Уведомление об отмене. Если есть возврат — указать сроки. | Guest (email) |
| `booking.completed` | Приглашение оставить отзыв (deeplink в review-service) | Guest (email) |

Фактическая отправка мокируется: логируется в таблицу `notification_log` с полным телом и статусом (SENT / FAILED). При FAILED — retry через 5 минут, максимум 3 попытки с экспоненциальным backoff.

---

## 4. Kafka: события и конфигурация

### 4.1 Топики

| Топик | Партиций | Ключ | Payload |
|---|---|---|---|
| `booking.created` | 3 | bookingId | eventType, bookingId, guestId, hotelId, roomTypeId, tariffId, checkIn, checkOut, totalPrice, currency, occurredAt |
| `booking.cancelled` | 3 | bookingId | eventType, bookingId, guestId, reason, occurredAt |
| `booking.completed` | 3 | bookingId | eventType, bookingId, guestId, hotelId, roomTypeId, occurredAt |
| `payment.confirmed` | 3 | paymentId | eventType, paymentId, bookingId, guestId, amount, currency, occurredAt |
| `payment.failed` | 3 | paymentId | eventType, paymentId, bookingId, guestId, reason, occurredAt |
| `review.created` | 3 | hotelId | eventType, reviewId, hotelId, roomTypeId, overallRating, occurredAt |
| `*.dlq` (6 штук) | 1 | то же | оригинальный payload + errorMessage, failedAt, retryCount |

### 4.2 Consumer конфигурация

- Каждый сервис использует отдельный groupId: `booking-service-group`, `payment-service-group`, `notification-group`, `review-service-group`.
- Обработка ошибок: `DefaultErrorHandler` с экспоненциальным backoff (1s → 2s → 5s → 10s, максимум 4 попытки), затем `DeadLetterPublishingRecoverer` в `*.dlq` топик.
- **Идемпотентность**: каждый consumer перед обработкой события проверяет по `eventId` в локальной таблице `processed_events`. Обеспечивает exactly-once семантику на уровне бизнес-логики.
- **Порядок**: ключ сообщения = entityId, что гарантирует упорядоченность всех событий одной сущности внутри партиции.
- **Distributed Tracing**: traceId/spanId пробрасываются через Kafka headers и подхватываются consumer'ами через Micrometer Observation.

### 4.3 DLQ Monitoring

Реализовать `AdminController` с эндпоинтами:
- `GET /admin/dlq` — просмотр сообщений из всех DLQ топиков (только ADMIN).
- `POST /admin/dlq/{topic}/replay` — переотправить сообщение из DLQ обратно в основной топик после ручного исправления ситуации.

---

## 5. Нефункциональные требования

### 5.1 Безопасность

- **JWT Access Token**: срок 15 минут, подписывается RS256 (asymmetric). Публичный ключ доступен по `/.well-known/jwks.json` в auth-service. Остальные сервисы валидируют токен локально по публичному ключу — без обращения в auth-service при каждом запросе.
- **JWT Refresh Token**: срок 7 дней, хранится хэш в PostgreSQL. При logout и смене пароля — все токены пользователя ревокируются. Хэши ревоцированных токенов хранятся в Redis blacklist до истечения срока (TTL = expiry).
- **Шифрование паспортных данных**: поле `passportNumber` шифруется AES-256 на уровне приложения перед сохранением. Ключ задаётся через environment variable.
- **Rate Limiting**: Bucket4j + Redis. 100 req/min на IP для анонимов, 500 req/min для аутентифицированных, 1000 req/min для ADMIN. Ответ 429 с заголовком `Retry-After`.
- **CORS**: настроен явно, без wildcard. Доверенные origins задаются через конфигурацию.
- Sensitive endpoints (смена пароля, изменение роли) требуют свежий токен (max age 5 минут).

### 5.2 Кэширование (Redis)

| Данные | TTL | Инвалидация |
|---|---|---|
| Доступность номеров (по параметрам поиска) | 1 мин | Вытеснение при создании/отмене бронирования на пересекающийся период |
| Детали отеля | 10 мин | Явный evict при `PUT /hotels/{id}` |
| Рейтинг отеля (RatingAggregate) | 5 мин | Обновляется consumer'ом при получении `review.created` |
| Активные тарифы на тип номера | 15 мин | Evict при добавлении/изменении тарифа |
| JWT Public Key (JWKS) | 1 час | Обновляется при ротации ключей |

### 5.3 Валидация

- Все входящие DTO аннотированы Jakarta Validation (`@NotBlank`, `@NotNull`, `@Size`, `@Min`, `@Max`, `@Future`, `@Email` и т.д.).
- Бизнес-валидации выбрасывают кастомные исключения (`InvalidBookingPeriodException`, `RoomNotAvailableException`, `PaymentAlreadyExistsException`) с информативными сообщениями.
- Глобальный `@RestControllerAdvice` возвращает унифицированный `ErrorResponse { timestamp, status, error, message, path, traceId }`.
- `checkOut` должен быть строго позже `checkIn`, минимальная длительность бронирования — 1 ночь.

### 5.4 Observability

**Метрики (Micrometer → Prometheus):**
- Counters: `bookings.created`, `bookings.cancelled`, `bookings.completed`, `payments.confirmed`, `payments.failed`, `reviews.created` — по каждому сервису.
- Timers: `http.server.requests` (автоматически), `booking.availability.search.duration`, `payment.processing.duration`.
- Gauges: `bookings.active` (текущее количество активных бронирований), `kafka.consumer.lag` по каждому topicPartition.
- DistributionSummary: `booking.duration.nights` (статистика длины бронирований), `payment.amount` (распределение сумм).

**Distributed Tracing:**
- Micrometer Tracing + Brave bridge + Zipkin reporter. TraceId автоматически добавляется в MDC → присутствует во всех log-строках.
- Kafka: traceId пробрасывается через header `X-B3-TraceId` при публикации, извлекается при потреблении.
- REST: traceId возвращается в response header `X-Trace-Id` и в теле `ErrorResponse`.

**Grafana Dashboards (минимум 2):**
- *Operational Dashboard*: RPS по сервисам, latency p50/p95/p99, error rate, Kafka consumer lag, активные бронирования.
- *Business Dashboard*: бронирований в день, успешность платежей (%), средний рейтинг отелей, топ-5 отелей по количеству бронирований.

---

## 6. Тестирование

### 6.1 Уровни

| Уровень | Инструменты | Что покрывать |
|---|---|---|
| Unit | JUnit 5, Mockito-Kotlin | Сервисы: бизнес-логика, маппинг, расчёт цены, валидация периодов. Цель: 70%+ покрытие сервисного слоя. |
| Integration (slice) | `@DataJpaTest` + Testcontainers PostgreSQL | Repository: нетривиальные JPQL-запросы, N+1 проверки, constraint violations. |
| Kafka Integration | EmbeddedKafka или Testcontainers Kafka | Consumer: опубликовать event вручную → убедиться что handler отработал корректно (проверить в БД). |
| Web Layer | `@WebMvcTest` | Controllers: маппинг URL, HTTP-коды, сериализация, валидация входных данных, обработка ошибок. |
| Full Integration (E2E) | `@SpringBootTest` + Testcontainers (Postgres + Kafka + Redis) | Сквозные сценарии: полный booking flow, double-booking protection, payment lifecycle. |

### 6.2 Обязательные E2E сценарии

| Сценарий | Шаги | Проверки |
|---|---|---|
| Happy path бронирования | Создать отель, тип номера, тариф, гостя → POST /bookings → POST /payments → симулировать payment.confirmed → GET /bookings/{id} | Booking.status = CONFIRMED, notification_log содержит PAYMENT_CONFIRMED |
| Double booking protection | Создать бронирование → попытаться создать второе на те же даты и номер | Второй POST /bookings → 409 Conflict |
| Отмена с возвратом | Создать и оплатить бронирование → POST /bookings/{id}/cancel → симулировать refund processed | Booking.status = CANCELLED, Refund.status = PROCESSED, notification_log содержит отмену |
| Review после checkout | Завершить проживание через check-out → POST /reviews | Review создан, RatingAggregate обновился; попытка без completed booking → 403 |
| JWT lifecycle | Login → использовать token → logout → попытка использовать тот же token | После logout: 401 Unauthorized |
| Rate limiting | Отправить 101 запрос за 60 секунд с одного IP | 101-й запрос → 429 с заголовком Retry-After |

### 6.3 Test Infrastructure

- Абстрактный `BaseIntegrationTest` с Testcontainers (PostgreSQL, Kafka, Redis). `@DynamicPropertySource` настраивает datasource и bootstrap-servers из контейнеров.
- TestDataBuilder / ObjectMother паттерн: фабричные методы для создания тестовых сущностей с разумными дефолтами.
- WireMock для мокирования REST-вызовов между сервисами в изолированных тестах одного сервиса.

---

## 7. Инфраструктура и DevOps

### 7.1 Структура репозитория

Монорепозиторий с Gradle multi-project build. Каждый сервис — отдельный subproject. Общие библиотеки выносятся в отдельные модули.

| Модуль | Содержимое |
|---|---|
| `common-events` | Kotlin data classes для всех Kafka событий (BookingCreatedEvent, PaymentConfirmedEvent и т.д.). Используется как зависимость в producer и consumer. |
| `common-api` | Общие классы: ErrorResponse, PageResponse, базовые исключения (DomainException, NotFoundException). |
| `booking-service` | Основной сервис. Зависит от common-events, common-api. |
| `auth-service` | Сервис аутентификации. Зависит от common-api. |
| `payment-service` | Платёжный сервис. Зависит от common-events, common-api. |
| `notification-service` | Сервис уведомлений. Зависит от common-events. |
| `review-service` | Сервис отзывов. Зависит от common-events, common-api. |

### 7.2 Docker Compose

Корневой `docker-compose.yml` запускает полный стек: все 5 сервисов + PostgreSQL (5 баз, отдельный volume на каждую) + Kafka + Zookeeper + Redis + Prometheus + Grafana + Zipkin. Секреты передаются через `.env` файл (в `.gitignore`). Healthcheck настроен для postgres и kafka; сервисы имеют `depends_on` с `condition: service_healthy`.

### 7.3 CI/CD (GitHub Actions)

| Job | Триггер | Шаги |
|---|---|---|
| `lint` | push, PR | ktlintCheck для всех модулей. Detekt static analysis. |
| `test` | push, PR | Unit + integration тесты. JaCoCo coverage report. Fail если coverage < 70%. |
| `build` | push main | `./gradlew build`. Docker build для каждого сервиса. Публикация в GitHub Container Registry (ghcr.io). |
| `e2e` | push main | Docker Compose up → E2E тесты → Docker Compose down. |

### 7.4 Конфигурация

- Все чувствительные значения (пароли БД, JWT ключи, ключи шифрования) — только через environment variables. Никаких секретов в `application.yaml`.
- Spring Profiles: `local` (Docker Compose), `test` (Testcontainers), `prod`. `application-local.yaml` переопределяет только connection strings.
- Liquibase: каждый сервис имеет собственный changelog. Схемы изолированы по базам данных (не по schema).

---

## 8. План реализации

| Этап | Название | Ключевые задачи | Новые технологии |
|---|---|---|---|
| 1 | Kafka Consumers + Audit | Consumers во всех сервисах. DLQ + error handling. Таблица `processed_events`. Таблица `event_audit`. Топики с партициями. `KafkaTopicConfig`. | `@KafkaListener`, `DefaultErrorHandler`, `DeadLetterPublishingRecoverer`, EmbeddedKafka |
| 2 | JPA + новые сущности | Переход на Spring Data JPA. Hotel, RoomType, Tariff, BookingStatusHistory. N+1 через `@EntityGraph`. Поиск доступности через JPQL. Pagination на list-эндпоинтах. | Spring Data JPA, `@EntityGraph`, JPQL, `@DataJpaTest`, Testcontainers PostgreSQL |
| 3 | Мультисервисная структура | Разбить монолит на 5 сервисов. Gradle multi-project. Вынести `common-events`, `common-api`. Межсервисные Kafka топики. | Gradle multi-project, Spring WebClient |
| 4 | auth-service + Spring Security | auth-service полностью. JWT RS256. JWKS endpoint. Spring Security во всех сервисах. Роли и ограничения. | Spring Security, JWT RS256, JWKS, Redis token blacklist |
| 5 | payment-service + review-service | Оба сервиса. Mock-провайдер платежей. Полный event flow. Review с проверкой completed booking. | WireMock, RestClient/WebClient |
| 6 | Redis: Cache + Rate Limiting | Кэш доступности, рейтингов, деталей отелей. Rate Limiting через Bucket4j. Distributed Lock при бронировании. Redis token blacklist. | Spring Cache + Redis, Bucket4j, Redisson, Testcontainers Redis |
| 7 | Observability | Timer, Gauge, DistributionSummary. Distributed Tracing + Zipkin. TraceId в Kafka headers и MDC. Docker Compose с Prometheus + Grafana + Zipkin. Дашборды. | Micrometer Tracing, Brave, Zipkin, Grafana |
| 8 | CI/CD + качество кода | GitHub Actions: lint → test → build → e2e. Dockerfile для каждого сервиса. Полный Docker Compose. Detekt, JaCoCo 70%+. README. | GitHub Actions, Docker multi-stage build, Detekt, JaCoCo |

### Итоговый стек

| Категория | Технология | Где |
|---|---|---|
| Language / Runtime | Kotlin 1.9, JVM 21 | Все сервисы |
| Framework | Spring Boot 3.x | Все сервисы |
| Persistence | Spring Data JPA + Hibernate | booking, auth, payment, review |
| DB Migration | Liquibase | Все сервисы |
| Database | PostgreSQL 17 | Все сервисы (отдельная БД) |
| Messaging | Apache Kafka + Spring Kafka | Все сервисы |
| Cache | Redis + Spring Cache | booking, auth, review |
| Rate Limiting | Bucket4j + Redis | Все сервисы |
| Distributed Lock | Redisson | booking-service |
| Security | Spring Security + JWT RS256 | Все сервисы |
| Validation | Jakarta Validation | Все сервисы |
| API Docs | SpringDoc OpenAPI 3 | Все сервисы |
| Metrics | Micrometer + Prometheus | Все сервисы |
| Tracing | Micrometer Tracing + Zipkin | Все сервисы |
| Monitoring | Grafana | Инфраструктура |
| Testing | JUnit 5, Mockito-Kotlin, Testcontainers, WireMock, EmbeddedKafka | Все сервисы |
| Build | Gradle (Kotlin DSL), ktlint, Detekt, JaCoCo | Все модули |
| CI/CD | GitHub Actions | Монорепозиторий |
| Containerization | Docker, Docker Compose | Локальный + CI |
| HTTP Client | Spring WebClient / RestClient | Межсервисные вызовы |
