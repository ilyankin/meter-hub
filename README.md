# MeterHub — сервис управления приборами учёта электроэнергии

![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-6DB33F?logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-7.0-6DB33F?logo=springsecurity&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-migrations-CC0200?logo=flyway&logoColor=white)

**MeterHub** — это типичный REST-сервис для учёта приборов
электроэнергии, передачи показаний по тарифным зонам, импорта показаний из CSV
и периодической отправки CSV-отчёта на email.

## Технологический стек

| Компонент | Назначение |
|---|---|
| Java 25 | язык (Gradle toolchain подтянет JDK сам) |
| Spring Boot 4.0.6 / Framework 7 | каркас приложения |
| Spring Security 7 | opaque Bearer-токены, роли |
| Spring Data JPA + Hibernate | доступ к данным |
| Flyway | миграции — единственный источник схемы (`ddl-auto: validate`) |
| PostgreSQL 17 (dev/prod) | основная БД (dev — авто-старт из `compose.yaml`) |
| H2 in-memory (тесты) | БД для тестов (`MODE=PostgreSQL`) |
| MapStruct | маппинг entity ↔ DTO |
| Caffeine + Spring Cache | кэш справочника тарифных зон |
| springdoc-openapi | Swagger UI / OpenAPI |
| spring-boot-starter-mail + Mailpit | отправка отчёта (Mailpit — локальная SMTP-песочница) |
| JUnit 5 + MockMvc | тесты |

## Быстрый старт

Из пререквизитов нужен только **Docker** (JDK 25 Gradle toolchain скачает сам) —
профиль `dev` поднимет PostgreSQL и Mailpit из `compose.yaml` (`spring-boot-docker-compose`).

```bash
./gradlew bootRun        # запуск (порт 8080, профиль dev по умолчанию)
./gradlew test           # все тесты (Docker не нужен — H2 in-memory)
./gradlew build          # компиляция + тесты + сборка
```

Полезные URL после запуска:

- Swagger UI — http://localhost:8080/swagger-ui.html (на `prod` отключён)
- OpenAPI JSON — http://localhost:8080/api-docs
- Health — http://localhost:8080/actuator/health (открыт без токена)
- Mailpit UI (письма отчёта) — http://localhost:8025

В `dev` при старте сидируются две учётки (`app.bootstrap.*`, см. `config/DataInitializer`):

| Роль | Email | Пароль |
|---|---|---|
| ADMIN | `admin@example.com` | `admin12345` |
| MANAGER | `manager@example.com` | `manager123` |

На `prod` сид выключен; первый админ заводится одноразово через
`APP_BOOTSTRAP_ENABLED=true` + `APP_ADMIN_*` из окружения.

## Профили

| Профиль | БД | Особенности |
|---|---|---|
| `dev` (default) | PostgreSQL из Compose | сид учёток, DEBUG-логи, Swagger |
| `prod` | внешний PostgreSQL (`APP_DB_URL`, `APP_DB_PASSWORD`) | ECS-структурированные логи, Swagger выключен |
| `test` | H2 in-memory | сид учёток, mail-health отключён |

Выбор: `SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun`.

## Аутентификация

Opaque **Bearer-токен в БД** (таблица `auth_tokens`), не JWT. Саморегистрации нет —
пользователей создаёт ADMIN. TTL токена — `app.auth.token-ttl` (по умолчанию `1d`),
истёкшие токены ночью вычищает `TokenCleanupScheduler`. Logout = удаление строки токена.

```bash
# 1. Логин
curl -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"admin12345"}'
# → {"token":"<...>", ...}

# 2. Использование токена
curl localhost:8080/api/readings -H 'Authorization: Bearer <token>'
```

Аноним на защищённом маршруте получает **401** (не 403). Авторизация двухуровневая:
URL-правила в `SecurityConfig` + `@PreAuthorize` на контроллерах.

## REST API

| Метод | Путь | Роль | Описание |
|---|---|---|---|
| POST | `/api/auth/login` | — | Вход, возврат токена |
| POST | `/api/auth/logout` | любая | Отзыв токена |
| GET | `/api/users/me` | любая аутентиф. | Собственный профиль |
| GET/POST | `/api/users` | ADMIN | Список / создание пользователей |
| GET/PUT/DELETE | `/api/users/{id}` | ADMIN | Пользователь по ID (в PUT пароль опционален) |
| GET/POST | `/api/meters` | ADMIN | Список / создание приборов |
| GET/PUT/DELETE | `/api/meters/{id}` | ADMIN | Прибор по ID |
| GET/POST | `/api/readings` | ADMIN, MANAGER | Показания (`?meterId=` — фильтр по прибору) |
| GET/PUT/DELETE | `/api/readings/{id}` | ADMIN, MANAGER | Показание по ID |
| POST | `/api/readings/import` | ADMIN | Импорт CSV (multipart, поле `file`) |
| POST | `/api/reports/send` | ADMIN, MANAGER | Отправить отчёт немедленно |
| GET | `/actuator/health` | — | Health-чек (детали — авторизованным) |
| GET | `/actuator/info` | любая аутентиф. | Версия, git SHA, время сборки |

Ошибки — **RFC 9457 `ProblemDetail`** через `GlobalExceptionHandler`.

## Показания и тарифные зоны

Показание (`meter_readings`) хранит значения нормализованно: строки в
`meter_reading_values` ссылаются на справочник `tariff_zones` (сидируются `T1` «День»,
`T2` «Ночь»). Новая зона = одна INSERT-строка в миграции, без изменения кода.
На `(прибор, дата)` действует уникальный констрейнт (V2-миграция).

## Импорт CSV

`POST /api/readings/import` (только ADMIN). Динамический заголовок: колонки `serial`
и `date` зарезервированы, остальные — коды зон из `tariff_zones`:

```
serial,date,T1,T2
SN-001,2024-01-15,100.5,50.25
```

- `date` — ISO `yyyy-MM-dd`; значения зон — десятичные `>= 0`; пустая ячейка = нет
  значения, но в строке нужно ≥ 1 непустое.
- **Всё-или-ничего:** любая ошибка (неизвестный прибор/зона, кривое число/дата, дубль
  `(прибор, дата)` в файле или БД) отклоняет весь файл — `400` с `ProblemDetail` и
  массивом `errors: [{row, column, message}]`. Успех — `200 {"imported": N}`.
- Лимит файла — 15MB (`spring.servlet.multipart`, переопределяется env-переменными).

## Периодический отчёт

`ReportScheduler` (fixedRate, `app.report.interval`, по умолчанию `14d`) строит CSV
с последними показаниями всех приборов (формат совместим с импортом) и отправляет
вложением на `app.report.recipient`. В `dev` письмо ловит Mailpit (http://localhost:8025).
Ручной запуск — `POST /api/reports/send`.

## Структура проекта

```
src/main/java/com/ilynkin/coding_assignment/
├── config/        # SecurityConfig, AppProperties (@Validated), DataInitializer, Cache/Scheduling/Swagger
├── controller/    # REST-контроллеры (Swagger-аннотации, @PreAuthorize)
├── dto/           # request/response (Java records, Bean Validation)
├── entity/        # JPA-сущности + enum Role
├── exception/     # доменные исключения + GlobalExceptionHandler (ProblemDetail)
├── mapper/        # MapStruct-мапперы
├── repository/    # Spring Data JPA
├── scheduling/    # ReportScheduler, TokenCleanupScheduler
├── security/      # BearerTokenFilter, UserPrincipal, MDC-фильтр (requestId/userId в логах)
└── service/       # бизнес-логика (@Transactional): Auth/User/Meter/MeterReading/CsvImport/Report/TariffZone
src/main/resources/db/migration/
├── V1__init.sql                      # схема + русскоязычные COMMENT ON + сид ролей и зон
└── V2__add_meter_reading_unique.sql  # UNIQUE(meter_id, reading_date)
```

Слои строго сверху вниз: `Filter → Controller (DTO) → Service (@Transactional) →
Repository → Entity`; сущности не покидают сервисный слой.

## Конфигурация

Все `app.*`-свойства — типизированный `@ConfigurationProperties`-record `AppProperties`
с `@Validated` (мисконфигурация валит старт, а не проявляется в рантайме). Ключевые
env-переменные:

| Переменная | Default | Что задаёт |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | профиль |
| `APP_DB_URL` / `APP_DB_USERNAME` / `APP_DB_PASSWORD` | — | БД на `prod` |
| `APP_AUTH_TOKEN_TTL` | `1d` | срок жизни токена |
| `APP_AUTH_CLEANUP_CRON` | `0 0 3 * * *` | очистка истёкших токенов |
| `APP_REPORT_RECIPIENT` | `user@example.com` | получатель отчёта |
| `APP_REPORT_INTERVAL` | `14d` | период отчёта |
| `APP_BOOTSTRAP_ENABLED` + `APP_ADMIN_*` / `APP_MANAGER_*` | `false` (prod) | сид учёток |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | `localhost:1025` | SMTP |

## HTTP Client

В директории [`http/`](http) — готовые файлы для **IntelliJ HTTP Client**:

| Файл | Что покрывает |
|---|---|
| `auth.http` | login / logout; сохраняет токены в переменные окружения клиента |
| `users.http` | CRUD пользователей, `/me`, обновление без смены пароля |
| `meters.http` | CRUD приборов |
| `readings.http` | CRUD показаний, фильтр по прибору |
| `csv-import.http` | импорт CSV-файла (`multipart/form-data`) |
| `reports.http` | ручной запуск отчёта |

Переменные окружения (`host`, `adminToken`, `managerToken`, `userId` и т.д.)
задаются в `http-client.env.json` (не трекается — создаётся локально).
После логина через `auth.http` токены записываются автоматически.

## Тесты

```bash
./gradlew test
./gradlew test --tests "AuthFlowIntegrationTest"
```

Покрытие — по тесту на эндпоинт + интеграционные сценарии: полный токен-флоу
(`AuthFlowIntegrationTest`), импорт CSV, smoke отчёта, очистка токенов,
кэш зон, fail-fast валидация конфигурации.
