# Testing — MilanSetu

## Results (real runs, 9–10 Nov 2026)

| Check | Result |
|---|---|
| `mvn verify` (Java 21, Spring Boot 3.5.16, Maven 3.9) | ✅ **BUILD SUCCESS** |
| Unit + Mockito tests (Surefire) | ✅ **48 / 48 passed** |
| Integration tests (Failsafe, real MySQL 8.0.46, Flyway V1 + V2) | ✅ **20 / 20 passed** |
| **Total** | ✅ **68 / 68**, 0 failures, 0 skipped |
| Concurrency test repeated 5 times | ✅ 5 / 5 |
| JaCoCo line coverage | **85.3 % overall, 88.7 % service layer** (target ≥ 70 %) |
| Frontend `npm run lint` + `npm run build` | ✅ |
| Docker Compose stack (MySQL 8.4, backend, Nginx, Mailpit) | ✅ all healthy; app, API and Swagger served through Nginx; Flyway migrated on start |
| Screenshots | ✅ captured from the running Docker stack (`docs/screenshots/`) |

**Where the integration tests ran.** The build workspace has no shared Docker for Testcontainers, so the tests ran against a real empty MySQL 8.0.46 database, selected with `TEST_DB_URL`. Without that variable (CI, your laptop) the same tests start a MySQL 8.4 container automatically.

**Docker note.** Inside the build workspace, containers had no direct internet access, so the image *build* stages (Maven/npm downloads) could not run there. The runtime images were built from the already-compiled jar and frontend with an identical runtime stage, and the full compose stack was started and checked. On a normal machine or in GitHub Actions, `docker compose up --build` builds everything from source.

## Bugs the real build found (and fixes)

1. **`RateLimitFilter` had two constructors**, so Spring could not create it. Fixed with `@Autowired`.
2. **Derived query `findByEventIdAndActiveTrue` failed at start-up.** `HelpDesk` has a `getEventId()` helper, so Spring Data looked for a field `eventId` that does not exist. Renamed to `findByEvent_IdAndActiveTrue`.
3. **Demo timeline in the wrong order** (seen on a screenshot). The demo data used fixed dates in 2027, which is in the future, so real actions sorted before them. Demo dates are now relative to `NOW()`; the match score is still 90.

## Test inventory (68 tests)

| Class | Type | Tests |
|---|---|---|
| `MatchScorerTest`, `MatchingRankTest`, `CaseStatusTest`, `HandoverOtpTest`, `SmallHelpersTest` | unit | 43 |
| `OtpServiceTest` | Mockito | 5 |
| `SecurityAndPrivacyIT` | MockMvc + MySQL | 12 |
| `AdminAndCaseManagementIT` | MockMvc + MySQL | 4 (admin set-up, member lifecycle, wrong-match undo + police assign/close + family withdraw, validation rules) |
| `ReunificationWorkflowIT` | API workflow | 3 |
| `ConcurrentHandoverIT` | concurrency | 1 (passed 5/5 repeated runs) |

## Run it yourself

```bash
cd backend
mvn verify                                   # needs Docker running (Testcontainers starts MySQL 8.4)
# or against an existing empty MySQL 8 database:
TEST_DB_URL="jdbc:mysql://localhost:3306/test_db?serverTimezone=UTC" TEST_DB_USER=root TEST_DB_PASSWORD=secret mvn verify
# coverage report: backend/target/site/jacoco/index.html

cd ../frontend && npm ci && npm run lint && npm run build
```

Postman: `postman/MilanSetu.postman_collection.json` (29 requests).
