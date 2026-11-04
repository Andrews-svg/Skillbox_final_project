## CI/CD — план (Блок 2)

### Перед пайплайном (backlog-хвосты)
- [ ] isSynchronizationActive() в UserServiceImpl.registerNewUser
- [ ] activation_token → NULL после активации
- [ ] AuthController.register: catch (Exception) → catch (IllegalArgumentException) throw
- [ ] application.yaml:url → ${SPRING_DATASOURCE_URL:...}
- [ ] IndexingState: AtomicBoolean → AtomicInteger

### Тесты — первый слой (unit)
- [ ] IndexingStateTest (wasActive: false→false, true→true)
- [ ] PasswordValidatorTest (короткий/длинный/спецсимволы)

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

## Решения (журнал)

- **04.11.2026** — Легаси-тесты (16 файлов, 1846 строк) **удалены**. Причина: писались под старую архитектуру (до refactor), не поддерживались, давали ложную уверенность «покрыто». Файлы в истории git (`git log --all -- src/test`). Новые тесты — с нуля, по принципу «защита от конкретного регресса», не «покрытие ради покрытия». Коммит `b81a66e`.
- **04.11.2026** — `application-test.yaml` **оставлен** (тестовый конфиг, не тест). Пересмотрим при первом integration-тесте.

## Поправки к плану

- **IndexingStateTest** — уточнение: `wasActive` паттерн живёт в `IndexingService.indexPage`, **не** в `IndexingState`. Поэтому:
  - `IndexingStateTest` — простой: `set(true)` → `isActive()=true`, `set(false)` → `isActive()=false`, идемпотентность `set`. **Защищает**: саму атомарность флага (тривиально, но даёт эталон стиля тестов).
  - `IndexingServiceTest.indexPage_*` — **защищает регресс Ф6**: если состояние было `false` → после `indexPage` снова `false`; если было `true` → остаётся `true`. Требует моков зависимостей.