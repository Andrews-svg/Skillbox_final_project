# Поисковый движок (Search Engine)

Дипломный проект курса «Java-разработчик».

## О проекте

Backend поисковой системы, аналогичной по принципу работы Яндекс Серверу.
Приложение самостоятельно обходит сайты, индексирует их страницы,
лемматизирует текст и выполняет поиск по проиндексированным данным
с ранжированием по релевантности.

## Цель проекта

Создать полноценный поисковый движок, демонстрирующий навыки backend-разработки:

- **многопоточность** — параллельный обход сайтов через `ForkJoinPool`
- **базы данных** — MySQL, JPA/Hibernate, миграции через Liquibase
- **парсинг веб-страниц** — Jsoup, Selenium для CSR-сайтов
- **морфология русского и английского языка** — Lucene Morphology (лемматизация, частотность)

## Возможности

- Многопоточная индексация сайтов (обход всех страниц, `ForkJoinPool`)
- Режимы индексации: **SINGLE** (один сайт) и **MULTI** (несколько параллельно)
- Лемматизация русского и английского текста (Lucene Morphology)
- Индексация отдельных страниц по URL
- Поиск по проиндексированным страницам с ранжированием
- Статистика по сайтам, страницам и леммам
- **История поисковых запросов** — аналитика: топ-запросы, запросы без результатов, средняя скорость
- JWT-аутентификация (регистрация, логин, refresh, активация по email)
- Роли `USER` и `ADMIN`, защита от подбора пароля
- Админ-панель: управление индексацией, статистика, аналитика, логи
- SPA-интерфейс на Thymeleaf + jQuery
- Поддержка CSR-сайтов через Selenium (опционально)

## Технологический стек

- **Java 21**
- **Spring Boot 3.1.12** — MVC, Data JPA, Security, Mail
- **MySQL 8** + Hibernate 6
- **Liquibase** — миграции схемы БД
- **JWT** (jjwt 0.13.0) — аутентификация
- **Thymeleaf** + **jQuery** — веб-интерфейс
- **Jsoup** — парсинг HTML
- **Selenium** — рендеринг CSR-сайтов
- **Lucene Morphology 1.5** — лемматизация (русский + английский)
- **Lombok**, **Maven**
- **Docker** + **Docker Compose**
- **GitHub Actions** — CI/CD

Актуальные версии зависимостей — в `pom.xml`.

## Быстрый старт

### Docker (рекомендуется)

```bash
docker compose up -d
```

Поднимает 4 сервиса:
- `app` — приложение на `http://localhost:8080`
- `mysql` — MySQL 8 (база `search_engine`)
- `selenium` — Selenium Chrome (для CSR-сайтов)
- `mailpit` — SMTP-сервер для dev-писем (`http://localhost:8025`)

Приложение ждёт готовности MySQL и Selenium (healthcheck), затем стартует.
Миграции Liquibase применяются автоматически при первом запуске.

Остановить:

```bash
docker compose down
```

С удалением данных MySQL:

```bash
docker compose down -v
```

Логи приложения — в `./logs/` (volume). Внутри контейнера: `app.log`, `error.log`.

### Локальный запуск

Требования: Java 21, Maven, MySQL 8.

1. Установить локальные артефакты Lucene Morphology (один раз):

```bash
mvn install:install-file -Dfile=lib/morphology-1.5.jar        -DpomFile=lib/morphology-1.5.pom
mvn install:install-file -Dfile=lib/dictionary-reader-1.5.jar -DpomFile=lib/dictionary-reader-1.5.pom
mvn install:install-file -Dfile=lib/morph-1.5.jar             -DpomFile=lib/morph-1.5.pom
mvn install:install-file -Dfile=lib/russian-1.5.jar           -DpomFile=lib/russian-1.5.pom
mvn install:install-file -Dfile=lib/english-1.5.jar           -DpomFile=lib/english-1.5.pom
```

2. Настроить MySQL (база `search_engine`, пользователь).

3. Запустить:

```bash
mvn spring-boot:run
```

Приложение будет на `http://localhost:8080`.

## Переменные окружения

| Переменная | Назначение | Dev-дефолт | Prod |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL MySQL | `jdbc:mysql://localhost:3306/search_engine?...` | обязательна |
| `DB_USER` | Пользователь БД | `root` | **обязательна, не `root`** |
| `DB_PASS` | Пароль БД | `root` | обязательна |
| `JWT_SECRET` | Base64-ключ для подписи JWT | встроенный dev-ключ | **обязательна** |
| `SELENIUM_REMOTE_URL` | URL Selenium Grid | не задан (локальный Chrome) | опционально |
| `SPRING_MAIL_HOST` | SMTP-сервер | не задан | опционально |

**Профиль `prod`** (fail-fast):
- Без `DB_USER` или `JWT_SECRET` приложение **не стартует**
- `DB_USER=root` в prod **отвергается** валидатором
- Незаполненный placeholder `${...}` **отвергается**

Это защита от запуска в prod с dev-ключами.

## Аутентификация и роли

JWT-based, 6 эндпоинтов в `AuthController` (`/api/auth`).

### Эндпоинты

| Метод | URL | Назначение |
|---|---|---|
| `POST` | `/api/auth/register` | Регистрация, проверка username/email, отправка письма активации |
| `GET` | `/api/auth/activate/{token}` | Активация аккаунта по токену из email |
| `POST` | `/api/auth/login` | Логин, выдача JWT (access + refresh) |
| `POST` | `/api/auth/refresh-token` | Обновление пары токенов |
| `POST` | `/api/auth/logout` | Выход, очистка контекста |
| `GET` | `/api/auth/check-username/{username}` | Проверка доступности имени |

### Роли

- **`USER`** — поиск, статистика, своя история
- **`ADMIN`** — управление индексацией, админ-панель, аналитика, логи

### Защита от подбора пароля

- **5 неудачных попыток** → аккаунт блокируется на **15 минут**
- Счётчик сбрасывается при успешном входе
- Все попытки (успешные и неудачные) пишутся в `user_activity_log`

### Активация по email

- Новый пользователь получает статус `UNCONFIRMED`
- Письмо с токеном отправляется через `EmailService`
- В dev — Mailpit (`http://localhost:8025`), в prod — реальный SMTP
- После активации `activation_token → NULL`, статус `CONFIRMED`

### JWT

- **Access token** — короткоживущий (для API-запросов)
- **Refresh token** — долгоживущий (для обновления access)
- Ключ подписи — `JWT_SECRET` (base64, ≥ 32 символа)
- В prod — fail-fast, если `JWT_SECRET` не задан или содержит `${...}`

## API и контроллеры

6 контроллеров в 4 пакетах:

| Пакет | Контроллер | Назначение |
|---|---|---|
| `auth` | `AuthController` | Регистрация, логин, активация, JWT |
| `api` | `ApiController` | Поиск, статистика (REST), логирование запросов |
| `admin` | `AdminApiController` | Управление индексацией, аналитика, логи (REST) |
| `admin` | `AdminWebController` | Админ-панель (веб) |
| `web` | `DefaultController` | SPA-роутинг |
| `web` | `PageController` | Отдача страниц |

## Админ-панель

Доступ — только роль `ADMIN`. Базовый путь — `/api/admin`.

### Управление индексацией

| Метод | URL | Назначение |
|---|---|---|
| `GET` | `/api/admin/status` | Текущий режим, идёт ли индексация, прогресс, ошибки |
| `GET` | `/api/admin/mode` | Текущий режим (SINGLE/MULTI) |
| `POST` | `/api/admin/mode/single` | Переключить на SINGLE (нельзя во время индексации) |
| `POST` | `/api/admin/mode/multi` | Переключить на MULTI (нельзя во время индексации) |
| `POST` | `/api/admin/crawler/stop/{siteId}` | Ручная остановка обхода сайта |
| `GET` | `/api/admin/crawler/progress` | Прогресс по сайтам |
| `GET` | `/api/admin/crawler/errors` | Ошибки по сайтам |

### Статистика и мониторинг

| Метод | URL | Назначение |
|---|---|---|
| `GET` | `/api/admin/overview` | Обзор: сайты, страницы, леммы, пользователи, визиты, uptime |
| `GET` | `/api/admin/health` | Health-статус системы |
| `GET` | `/api/admin/sessions?limit=20` | История сессий индексации |
| `GET` | `/api/admin/sites` | Список сайтов со статусами и количеством страниц |

### Аналитика поиска

| Метод | URL | Назначение |
|---|---|---|
| `GET` | `/api/admin/analytics` | Композитный DTO: топ-запросы, zero-result, активные сессии, health, auth-логи, визиты, пользователи, uptime |
| `GET` | `/api/admin/analytics/top-queries?limit=10` | Топ поисковых запросов |
| `GET` | `/api/admin/analytics/zero-result` | Запросы без результатов (для тюнинга) |

**Источник аналитики:** таблица `search_query_logs` (17 полей: `query`, `userId`, `username`, `siteUrl`, `timestamp`, `resultsCount`, `clicked`, `responseTimeMs`, `success`, `errorMessage`, `userIp`, `userAgent`, `advancedSearch`, `filters`). Пишется при каждом поиске через `SearchLogService.logQuery()`. Индексы по `user_id`, `query`, `timestamp`, `site_url`.

### Логи

| Метод | URL | Назначение |
|---|---|---|
| `GET` | `/api/admin/auth-log?limit=50` | Лог аутентификации (`user_activity_log`): вход, выход, ошибки, активация |

### UI-фрагменты

| Метод | URL | Назначение |
|---|---|---|
| `GET` | `/api/admin/layout` | Каркас страницы |
| `GET` | `/api/admin/panel` | Главная панель |
| `GET` | `/api/admin/modes` | Управление режимами |

## Архитектура

```
src/main/java/com/example/searchengine/
├── Application.java
├── config/         — Spring-конфигурация, SitesList, CrawlerConfig, DbCredentialsValidator
├── controllers/    — REST и веб-контроллеры
├── dto/            — DTO для API (adminLogs, search, statistics, …)
├── exceptions/     — GlobalExceptionHandler, кастомные исключения
├── models/         — JPA-сущности (Site, Page, Lemma, Index, AppUser, IndexingSession, statistics/*)
├── repositories/   — Spring Data JPA (включая statistics/)
├── services/       — бизнес-логика
│   ├── crawler/    — обход сайтов (CrawlerService, SiteCrawler, PageProcessor, WatchdogService, …)
│   ├── indexing/   — индексация (IndexingService, IndexingState, SessionHistoryService)
│   ├── jwt/        — генерация и валидация JWT
│   └── user/       — регистрация, активация
├── utils/          — Lemmatizer, UrlFilter
└── validation/     — @ValidPassword, PasswordValidator
```

**Ключевые архитектурные точки:**
- **Единый путь API** — фронтенд ходит через `sendData()` → `send{}` map
- **SPA-роутер** — `loadFragment(hash)` на клиенте
- **`IndexingState`** — ref-count + stop для управления индексацией (см. коммит `a6d47ca`)
- **Fail-fast** — prod не стартует без секретов и с `DB_USER=root`
- **`SearchLogService`** — единая точка записи истории поиска (вызывается из `ApiController`)
- **`IndexingRecoveryService`** — восстановление статусов сайтов после рестарта

## Тестирование

**115 unit-тестов**, все зелёные.

```bash
mvn test
```

Покрыто:
- `IndexingState`, `IndexingService`, `IndexingServiceRaceTest` (race P1b), `IndexingServiceStopTest` (P1c)
- `CrawlerService`, `PageProcessor` (контракт), `LinkExtractor` (13 тестов)
- `AuthController`, `GlobalExceptionHandler`
- `PasswordValidator`, `SearchService`, `UserServiceImpl`
- `DbCredentialsValidator`, `JwtSecretFailFastTest` (fail-fast в prod)
- `IndexingRecoveryService`

Не покрыто осознанно:
- `SeleniumFetcher`, `SiteCrawler`, `WatchdogService` — флейки / покрыто `docker-smoke`
- Асинхронная часть `IndexingService.startFullIndexing` — покрыто `docker-smoke`

## CI/CD

GitHub Actions, 4 job'а на каждый push в `master`:

| Job | Что делает | Зависимости |
|---|---|---|
| `secret-scan` | gitleaks по full history, upload SARIF | — |
| `build-and-test` | Java 21, local libs, `mvn clean verify`, upload surefire | — |
| `docker-smoke` | `docker compose up`, ждёт приложения, smoke: `GET /` = 200, `GET /api/statistics` = 401 | `build-and-test` |
| `docker-push` | Публикация образа в GHCR | `docker-smoke`, `secret-scan` |

**Образ:** `ghcr.io/andrews-svg/searchengine` (публичный).
**Gitleaks:** v8.30.1, `--redact`.

`docker-push` **не выполнится**, если `docker-smoke` или `secret-scan` упали —
защита от публикации сломанного образа.

## Миграции БД

Liquibase, режим `update` (Spring Boot auto-config).

- Changelog: `src/main/resources/db/changelog/db.changelog-master.xml`
- Дочерние миграции: `01`–`11`, `14` (12 файлов)
- Применяются автоматически при старте приложения
- `drop-first` **не задан** — данные не уничтожаются

Проверить применённые changeSet'ы — таблица `DATABASECHANGELOG` в MySQL.

## Соглашения о коммитах

См. [`docs/commit-conventions.md`](docs/commit-conventions.md).

Формат: `<тип>(<область>): <что изменилось>` — императив, ≤72 символов.
Типы: `fix`, `feat`, `test`, `ci`, `docs`, `chore`, `refactor`, `perf`, `build`, `revert`.

## Статус проекта

**Готово:** индексация (SINGLE/MULTI), поиск, JWT + активация по email, роли USER/ADMIN, админ-панель (управление индексацией + аналитика + логи), Docker, CI/CD (4 job'а), 115 тестов.

**В работе / впереди:** финальная документация (этот README), ретроспектива архитектуры.
