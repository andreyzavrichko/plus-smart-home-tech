# Smart Home Technologies — Microservices Platform

Учебный pet-проект на базе Spring Boot / Spring Cloud. Реализует e-commerce слой платформы умного дома: каталог товаров, склад, корзина покупателя. Дополнен телеметрическим стеком (сбор, агрегация, анализ событий с IoT-устройств).

---

## Содержание

1. [Архитектура](#архитектура)
2. [Структура модулей](#структура-модулей)
3. [Технологический стек](#технологический-стек)
4. [Схема базы данных](#схема-базы-данных)
5. [API — краткий справочник](#api--краткий-справочник)
6. [Конфигурация](#конфигурация)
7. [Запуск](#запуск)
8. [Тестирование](#тестирование)
9. [Circuit Breaker](#circuit-breaker)
10. [Известные ограничения](#известные-ограничения)

---

## Архитектура

```
                        ┌──────────────────┐
                        │  Discovery Server │  :8761 (Eureka)
                        └────────┬─────────┘
                                 │ регистрация
          ┌──────────────────────┼───────────────────────┐
          ▼                      ▼                       ▼
  ┌───────────────┐   ┌──────────────────┐   ┌──────────────────┐
  │ Config Server │   │    API Gateway   │   │  Микросервисы    │
  │  (native FS)  │   │  :8080 (lb://)   │   │ (порт=0 / Eureka)│
  └───────────────┘   └────────┬─────────┘   └──────────────────┘
                               │
              ┌────────────────┼─────────────────┐
              ▼                ▼                 ▼
      shopping-store    shopping-cart         warehouse
         :8081              :8083              :8082
              │                │                 │
              └────────────────┴─────────────────┘
                                 │
                         PostgreSQL :5432
                          (analyzer_db)
```

Межсервисное взаимодействие — **OpenFeign** через **Eureka** (lb://). Отказоустойчивость — **Resilience4j CircuitBreaker**.  
Телеметрия: collector → Kafka → aggregator → analyzer (PostgreSQL).

---

## Структура модулей

```
smart-home-tech/                      ← root POM (BOM: Spring Boot 3.3.2, Spring Cloud 2023.0.3)
├── infra/
│   ├── discovery-server/             ← Eureka Server (:8761)
│   ├── config-server/                ← Spring Cloud Config (native, classpath)
│   └── gateway/                     ← Spring Cloud Gateway (:8080)
├── commerce/
│   ├── interaction-api/              ← shared DTOs + Feign-клиенты (jar, без main)
│   ├── shopping-store/               ← каталог товаров
│   ├── shopping-cart/                ← корзина, circuit breaker → warehouse
│   └── warehouse/                   ← склад, бронирование
└── telemetry/
    ├── collector/                    ← gRPC-приёмник событий → Kafka
    ├── aggregator/                   ← Kafka consumer → снэпшоты
    └── analyzer/                     ← анализ снэпшотов, gRPC → hub-router
```

### interaction-api — публичный контракт

| Пакет | Содержимое |
|---|---|
| `dto.cart` | `ShoppingCartDto`, `ChangeProductQuantityRequest` |
| `dto.product` | `ProductDto`, `ProductCategory`, `ProductState`, `QuantityState` |
| `dto.warehouse` | `NewProductInWarehouseRequest`, `AddProductToWarehouseRequest`, `AddressDto`, `BookedProductsDto`, `DimensionDto` |
| `exception` | `NoProductsInShoppingCartException`, `NotAuthorizedUserException`, `ProductInShoppingCartLowQuantityInWarehouseException`, `ProductNotFoundException` |
| `feign` | `ShoppingCartClient`, `ShoppingStoreClient`, `WarehouseClient` |

---

## Технологический стек

| Слой | Технология |
|---|---|
| Фреймворк | Spring Boot 3.3.2 |
| Сервис-дискавери | Spring Cloud Netflix Eureka |
| Конфигурация | Spring Cloud Config (native) |
| Gateway | Spring Cloud Gateway |
| Межсервисный HTTP | OpenFeign |
| Отказоустойчивость | Resilience4j (CircuitBreaker + TimeLimiter) |
| ORM | Spring Data JPA / Hibernate |
| БД | PostgreSQL 16 |
| Сообщения | Apache Kafka (telemetry) |
| RPC | gRPC (telemetry) |
| Сериализация | Apache Avro (telemetry) |
| Сборка | Maven 3.9, Java 21 |
| Контейнеризация | Docker / Docker Compose |

---

## Схема базы данных

Все три commerce-сервиса используют одну БД `analyzer_db`, разнесённую по схемам.

### shopping_store.products

| Колонка | Тип | Описание |
|---|---|---|
| product_id | UUID PK | идентификатор |
| product_name | VARCHAR(255) NOT NULL | название |
| description | TEXT NOT NULL | описание |
| image_src | VARCHAR(512) | ссылка на изображение |
| quantity_state | VARCHAR(50) NOT NULL | ENDED / FEW / ENOUGH / MANY |
| product_state | VARCHAR(50) NOT NULL | ACTIVE / DEACTIVATE |
| product_category | VARCHAR(50) | LIGHTING / CONTROL / SENSORS |
| price | NUMERIC(19,2) NOT NULL | цена |

### shopping_cart.shopping_carts

| Колонка | Тип | Описание |
|---|---|---|
| shopping_cart_id | UUID PK | идентификатор |
| username | VARCHAR(255) NOT NULL | владелец |
| active | BOOLEAN NOT NULL DEFAULT TRUE | флаг активности |

### shopping_cart.shopping_cart_items

| Колонка | Тип | Описание |
|---|---|---|
| shopping_cart_id | UUID FK | ссылка на корзину |
| product_id | UUID | идентификатор товара |
| quantity | BIGINT NOT NULL | количество |

### warehouse.warehouse_products

| Колонка | Тип | Описание |
|---|---|---|
| product_id | UUID PK | идентификатор (внешний) |
| fragile | BOOLEAN NOT NULL DEFAULT FALSE | хрупкость |
| weight | DOUBLE PRECISION NOT NULL | вес, кг |
| width / height / depth | DOUBLE PRECISION NOT NULL | габариты, см |
| quantity | BIGINT NOT NULL DEFAULT 0 | количество на складе |

---

## API — краткий справочник

> Все эндпоинты доступны через Gateway: `http://localhost:8080`  
> Прямой доступ: shopping-store `:8081`, warehouse `:8082`, shopping-cart `:8083`

### Shopping Store `/api/v1/shopping-store`

| Метод | Путь | Описание |
|---|---|---|
| PUT | `/` | Создать товар |
| POST | `/` | Обновить товар |
| GET | `/?category={LIGHTING\|CONTROL\|SENSORS}&page=0&size=20&sort=productName,DESC` | Список по категории (Page) |
| GET | `/{productId}` | Получить товар |
| POST | `/removeProductFromStore` | Деактивировать товар (body: UUID) |
| POST | `/quantityState?productId=&quantityState=` | Установить статус наличия |

### Warehouse `/api/v1/warehouse`

| Метод | Путь | Описание |
|---|---|---|
| PUT | `/` | Зарегистрировать новый товар на складе |
| POST | `/add` | Добавить количество товара |
| POST | `/check` | Проверить наличие под корзину (body: ShoppingCartDto) |
| GET | `/address` | Получить адрес склада |

### Shopping Cart `/api/v1/shopping-cart`

| Метод | Путь | Описание |
|---|---|---|
| GET | `/?username=` | Получить активную корзину |
| PUT | `/?username=` | Добавить товары (body: Map<UUID, Long>) |
| DELETE | `/?username=` | Деактивировать корзину |
| POST | `/remove?username=` | Удалить товары из корзины (body: List<UUID>) |
| POST | `/change-quantity?username=` | Изменить количество (body: ChangeProductQuantityRequest) |

---

## Конфигурация

### Иерархия application.yml

```
Сервис bootstrap.yml          ← только: spring.application.name + eureka
    ↓ fetch
Config Server (classpath)     ← classpath:config/commerce/{application}/application.yml
    ↓ merge
Env variables (Docker)        ← SERVER_PORT, SPRING_DATASOURCE_URL, EUREKA_...
```

### Ключевые переменные окружения (Docker Compose)

| Переменная | Пример | Описание |
|---|---|---|
| `SERVER_PORT` | `8081` | HTTP порт сервиса |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/analyzer_db` | JDBC URL |
| `SPRING_CLOUD_CONFIG_URI` | `http://config-server:8888` | Адрес config-server |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://discovery-server:8761/eureka/` | Eureka endpoint |

---

## Запуск

### Предварительные требования

- Docker Desktop / Docker Engine + Compose v2
- Порты свободны: `5432`, `8761`, `8888`, `8080`, `8081`, `8082`, `8083`, `9092`

### Полный старт

```bash
docker compose up --build -d
```

### Порядок инициализации (healthcheck-зависимости)

```
postgres ──► discovery-server ──► config-server ──► gateway
                                                  ├──► shopping-store
                                                  ├──► shopping-cart
                                                  └──► warehouse
kafka ──► kafka-init-topics ──► collector / aggregator / analyzer
```

### Проверка состояния

```bash
# Eureka dashboard
open http://localhost:8761

# Health всех сервисов
curl http://localhost:8080/actuator/health
curl http://localhost:8888/actuator/health

# Список зарегистрированных инстансов
curl -s -H "Accept: application/json" http://localhost:8761/eureka/v2/apps | \
  python3 -m json.tool | grep '"app"'
```

### Остановка и очистка данных

```bash
docker compose down -v   # -v удаляет volumes (БД)
```

---

## Тестирование

Postman-коллекция: `(Sprint 21) Smart Home Technologies API.postman_collection.json`

### Импорт и настройка

1. Импортировать коллекцию в Postman.
2. Collection variables установятся автоматически через pre-request script (опрос Eureka).
3. Запустить **Collection Runner** — порядок: `warehouse` → `shopping-store` → `shopping-cart`.

### Переменные коллекции

| Переменная | Дефолт | Источник |
|---|---|---|
| `baseUrl` | `http://localhost:` | статичная |
| `shopping-store-port` | `12345` | Eureka (pre-request) |
| `shopping-cart-port` | `12345` | Eureka (pre-request) |
| `warehouse-port` | `12345` | Eureka (pre-request) |

> **Внимание:** порты динамические (`server.port: 0`). Pre-request script коллекции автоматически опрашивает Eureka и обновляет переменные перед каждым запросом.

### Покрытие тестами (Postman)

| Сервис | Тест | Проверяет |
|---|---|---|
| shopping-store | add Product To Shop | статус 200/201, поля ProductDto |
| shopping-store | get Product | статус 200, совпадение полей |
| shopping-store | get Products | статус 200, пагинация, сортировка DESC |
| shopping-store | update Product | статус 200/201, обновлённые поля |
| shopping-store | remove Product | статус 200/203, productState=DEACTIVATE |
| shopping-store | set Product Quantity State | статус 200/201, quantityState обновлён |
| warehouse | new Product In Warehouse | статус 200/201 |
| warehouse | add Product To Warehouse | статус 200/201 |
| warehouse | get Warehouse Address | статус 200, наличие полей адреса |
| shopping-cart | add Product To Shopping Cart | статус 200/201, продукты в корзине |
| shopping-cart | get Shopping Cart | статус 200, совпадение продуктов |
| shopping-cart | change Product Quantity | статус 200/201/204, новое количество |
| shopping-cart | remove From Shopping Cart | статус 200/201/204, shoppingCartId |
| shopping-cart | deactivate Current Shopping Cart | статус 200/201/203/204 |

---

## Circuit Breaker

Настроен в `shopping-cart` для вызовов к `warehouse`.

```yaml
resilience4j:
  circuitbreaker:
    instances:
      warehouse:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50        # % ошибок для открытия
        wait-duration-in-open-state: 10000 # мс в OPEN перед HALF_OPEN
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
  timelimiter:
    instances:
      warehouse:
        timeout-duration: 3s
```

**Fallback** (`WarehouseClientFallback`): при недоступности склада выбрасывает `ProductInShoppingCartLowQuantityInWarehouseException` с HTTP 400.

---

## Известные ограничения

| # | Ограничение | Влияние |
|---|---|---|
| 1 | `checkProductQuantityEnoughForShoppingCart` возвращает `void`, `BookedProductsDto` строится но не возвращается | данные о весе/объёме/хрупкости теряются |
| 2 | `WarehouseService.CURRENT_ADDRESS` инициализируется случайно при старте JVM | при нескольких инстансах warehouse адрес будет разным |
| 3 | shopping-store не использует Feign (нет `@EnableFeignClients`) | нет межсервисных вызовов из store |
| 4 | Все три сервиса используют одну схему в одной БД | нарушает изоляцию данных микросервисов |
| 5 | `server.port: 0` + Postman требует опроса Eureka перед каждым запросом | нельзя использовать фиксированные curl-команды без gateway |
