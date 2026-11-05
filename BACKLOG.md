## CI/CD — план (Блок 2)

### Перед пайплайном (backlog-хвосты)
- [x] isSynchronizationActive() в UserServiceImpl.registerNewUser
- [x] activation_token → NULL после активации
- [x] AuthController.register: catch (Exception) → catch (IllegalArgumentException) throw
- [x] application.yaml:url → ${SPRING_DATASOURCE_URL:...}
- [ ] IndexingState: AtomicBoolean → AtomicInteger

### Тесты — первый слой (unit)
- [x] IndexingStateTest (wasActive: false→false, true→true)
- [x] PasswordValidatorTest (короткий/длинный/спецсимволы)

### Тесты — второй слой (integration)
- [ ] UserServiceImplTest + Testcontainers MySQL (afterCommit, один email)
- [ ] Smoke: docker compose up → / = 200, /api/statistics = 401

### Пайплайн (GitHub Actions)
- [ ] .github/workflows/ci.yml: push + pull_request → mvn clean verify
- [ ] Job 2: docker compose build + up → curl smoke
- [ ] Quality gate: JaCoCo coverage (порог — потом)
- [ ] docker push ghcr.io/andrews-svg/searchengine:${{ github.sha }}

### Секреты (GitHub Secrets)
- [ ] JWT_SECRET, DB_PASS, MYSQL_ROOT_PASSWORD — из yaml/compose → GitHub Secrets
- [ ] gitleaks — проверка, что секреты не в git

---

## Безопасность — backlog

- [ ] **Fail-fast: prod + пустой JWT_SECRET** → `IllegalStateException` на старте (`JwtConfig @PostConstruct`). Сейчас дефолт `mySuperSecret...` — если prod забудет env, приложение стартует с **тестовым** ключом → любой, кто видел git, подпишет валидный JWT. Лучше «не стартовать», чем «стартовать с дырой»
- [ ] **Fail-fast: prod + DB_USER=root** → warning/fail. `root` в prod — плохо
- [ ] **gitleaks в CI** — сканирует diff на паттерны секретов. Уже в разделе «Секреты», здесь — как **защита от будущих** ошибок
- [ ] **Ротация JWT_SECRET** — процедура: сменить env → все токены инвалидируются. При инциденте

## Технический долг — мелочи

- [ ] **Дедуплицировать `.gitignore`** — блоки `HELP.md/target/...` + `### STS ###` + `### IntelliJ ###` + `### NetBeans ###` + `### VS Code ###` повторяются **дважды** (строки 1–37 и 39–...). `.gitignore` берёт первое совпадение — не ломает, но шум. 5 минут
- [ ] **`!lib/russian-1.5.jar` до `*.jar`** — мёртвая строка (negation **до** правила — памятка 31.10). Спасает только `!lib/*.jar` в конце. Убрать или переставить
- [ ] **`Page/src/main/resources/application.yaml`** в `.gitignore` — **другой** путь. Что такое `Page/`? Старая копия, подпроект? Решить: удалить папку или задокументировать
- [ ] **`application.yaml` — YAML-структура**: `jpa.hibernate.properties` — часть `hibernate`, но `dialect` — на уровне `hibernate`. Возможно, `dialect` должен быть в `properties.hibernate.dialect`. Проверить при старте с MySQL (сейчас `test-compile` не ловит, `mvn test` не поднимает JPA)

## CI/CD — следующий шаг

- [ ] `.github/workflows/ci.yml` — **все 5 хвостов закрыты, можно начинать**

## Решения (журнал)

- **04.11.2026** — Легаси-тесты (16 файлов, 1846 строк) **удалены**. Причина: писались под старую архитектуру (до refactor), не поддерживались, давали ложную уверенность «покрыто». Файлы в истории git (`git log --all -- src/test`). Новые тесты — с нуля, по принципу «защита от конкретного регресса», не «покрытие ради покрытия». Коммит `b81a66e`.
- **04.11.2026** — `application-test.yaml` **оставлен** (тестовый конфиг, не тест). Пересмотрим при первом integration-тесте.

## Поправки к плану

- **IndexingStateTest** — уточнение: `wasActive` паттерн живёт в `IndexingService.indexPage`, **не** в `IndexingState`. Поэтому:
  - `IndexingStateTest` — простой: `set(true)` → `isActive()=true`, `set(false)` → `isActive()=false`, идемпотентность `set`. **Защищает**: саму атомарность флага (тривиально, но даёт эталон стиля тестов).
  - `IndexingServiceTest.indexPage_*` — **защищает регресс Ф6**: если состояние было `false` → после `indexPage` снова `false`; если было `true` → остаётся `true`. Требует моков зависимостей.
