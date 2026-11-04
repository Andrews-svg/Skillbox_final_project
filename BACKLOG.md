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
