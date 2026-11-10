# MilanSetu — Architecture (Phase 1, for review)

> MilanSetu is a portfolio project. Events such as "Rath Yatra (DEMO)" are sample data. It is not an officially approved emergency system. A real deployment involving children would need organisational, legal and privacy approval.

## 1. Technology versions

| Layer | Choice |
|---|---|
| Language | Java 21 (LTS) |
| Framework | **Spring Boot 3.5.16** (decision D1 approved; free support for 3.5 ended 30 Jun 2026; 4.x is the upgrade path) |
| Security | Spring Security 6, JWT (`jjwt` 0.12.x), BCrypt |
| Persistence | Spring Data JPA, Hibernate 6, Flyway + `flyway-mysql`, MySQL 8.4 LTS in Docker (schema validated on 8.0.46) |
| QR | ZXing 3.5.3 (`core` + `javase`) → PNG |
| Mail | Spring Mail. Dev uses a mock notifier plus MailHog/Mailpit container (free) |
| Rate limiting | small in-house fixed-window filter, in-memory (no Redis, as the spec requires) |
| API docs | springdoc-openapi 2.x |
| Frontend | React 19 + TypeScript + Vite, React Router, Tailwind CSS, Axios, Recharts, `html5-qrcode` |
| Testing | JUnit 5, Mockito, MockMvc, `@DataJpaTest`, Testcontainers MySQL, JaCoCo (coverage report) |
| DevOps | Maven, Docker, Docker Compose, Nginx, GitHub Actions |

## 2. Style: layered modular monolith

```
React SPA ─HTTPS─► Nginx ─► Spring Boot JAR ─► MySQL 8
                              ├ controller → service → repository
                              ├ security (JWT filter, AccessPolicy: ownership + event scope)
                              ├ notification (NotificationSender interface: Email | Mock)
                              └ scheduler (OTP expiry, stale-case detection)
```

## 3. Core workflows

### 3.1 QR scan: `POST /api/scan/{qrToken}` (VOLUNTEER, DESK)

1. Check the token is a valid UUID, then look up the member by `qr_token`. An unknown or revoked token gets a **generic 404 "Invalid wristband"**.
2. The member's event must be active, and the scanner must be in `event_staff` for that event.
3. Choose the desk:
   - if the request has the scanner's GPS location, use the nearest active desk of that event (Haversine)
   - otherwise use the scanner's home desk
4. In one transaction:
   - insert `found_reports` (source = QR_SCAN, member_id set)
   - insert `case_files` (REPORTED, member_id set)
   - insert `case_events`
   - if an open missing report for this member exists, link it and tell the desk "registered member: family report on file" (desk staff still confirm)
5. A second scan while a case is active hits the DB unique key `uq_fr_active_member`. The API then returns the **existing** case (200), not an error and not a duplicate.
6. Notify the guardian: "Your family member has been found and is being taken to <desk>."
7. Response to the volunteer contains only: `caseId, firstName, approxAge, gender, deskName, deskLat/Lng, distanceKm, instructions`. It never includes the phone number, notes or guardian identity.

### 3.2 Matching engine (MatchingService, pure Java, fully unit-tested)

```
candidates = found_reports WHERE event_id = :event AND status = 'OPEN'   -- uses idx (event_id, status)
for each candidate:
    if gender differs                → skip (hard filter)
    score = 25                                       // same gender
    ageDiff = |missing.age - found.age|  → ≤2: +25 · ≤5: +12
    clothing equal (fixed palette)       → +20
    km = haversine(lastSeen, foundAt)    → ≤1: +20 · ≤3: +10
    h  = minutes(lastSeenAt → foundAt) / 60
         if h < 0 → 0 points (found *before* last seen is not evidence)
         ≤2: +10 · ≤6: +5
    keep if score ≥ 50
sort by score desc, then by distance asc (tie-break)
return score + factor breakdown, e.g. [GENDER 25, AGE 25, CLOTHING 20, DISTANCE 10, TIME 10]
```

- Boundaries are **inclusive** (exactly 2 years = 25, exactly 1.000 km = 20, exactly 120 min = 10).
- Time uses full `LocalDateTime` / `Duration`, never hour-of-day.
- The maximum score is 25+25+20+20+10 = 100.
- Matching only **suggests**. Nothing changes until a DESK user calls `confirm-match`.

### 3.3 Case lifecycle

```
REPORTED ──confirm-match──► MATCHED ──arrived──► AT_DESK ──handover(valid OTP)──► REUNITED ──► CLOSED
   │                          │  └─reject match─► REPORTED          │
   └───────── close (POLICE / family withdraws own report) ─────────┴──► CLOSED (POLICE only)
```

Transitions live in one place: `CaseStatus.canMoveTo()` plus `CaseService.changeStatus()`. Every change writes `case_events`.

**REUNITED has no generic endpoint.** It can only be set inside `OtpService.verifyAndHandover()`.

**Confirming a match** (when the missing case and the found case are separate):
1. Close the found-side case with `merged_into_id` set.
2. Attach the found report to the missing case and mark both reports MATCHED.

This must happen in one transaction, in that order. The DB's unique "canonical" keys make the wrong order fail, which was tested.

### 3.4 OTP handover

| Step | Rule |
|---|---|
| Generate | `SecureRandom` → 6 digits (000000–999999, leading zeros kept) |
| Store | BCrypt hash only; `expires_at = now + 10 min`; any previous ACTIVE OTP for the case → SUPERSEDED (DB allows only one ACTIVE) |
| Deliver | `NotificationSender` → email to guardian (prod) / mock outbox (dev only). Never in an API response, log line or notifications row |
| Re-issue | max 3 OTPs per case per hour (rate limit) |
| Verify | inside `@Transactional`; case must be AT_DESK; OTP must be ACTIVE and not expired; wrong code → `attempts+1`; 5th wrong → LOCKED (new OTP needed); correct → OTP USED + case REUNITED + reports RESOLVED |
| Concurrency | `case_files.version` and `handover_otps.version`: two desks verifying at once → one commits, the other gets `OptimisticLockException` → 409 |

### 3.5 Scheduled jobs

| Job | Rule |
|---|---|
| `OtpExpiryJob` (every minute) | `UPDATE status='EXPIRED' WHERE status='ACTIVE' AND expires_at < now`. Idempotent. Verification also checks `expires_at` itself, so correctness doesn't depend on the job. |
| `OtpCleanupJob` (daily) | deletes non-active OTP rows older than the retention period (default 30 days, configurable) |
| `StaleCaseJob` (every 15 min) | flags cases with no activity for N hours (default 2) on the police dashboard and notifies the assigned desk. It **never auto-closes** a case. |

## 4. Security model

| Role | Can see |
|---|---|
| FAMILY | own members, own reports, cases linked to own reports or members (ownership check: `guardian_id` / `reported_by = me`) |
| VOLUNTEER | scan + found registration for events they staff; minimal person data |
| DESK | cases of events they staff; full report fields **except guardian phone/notes** |
| POLICE | event-wide cases of events they staff; assign/close |
| ADMIN | events, desks, users, staff assignment, reports |

Other rules:
- Public `/auth/register` always creates a FAMILY account; a `role` field in the request body is ignored.
- Other accounts are created by ADMIN.
- Guardian contact goes only through `NotificationService`. No DTO for a non-family role has a phone field, and a test proves it.
- Another family's member or case returns **404** (not 403).

## 5. Folder structure

```
milansetu/
├── backend/src/main/java/com/rajesh/milansetu/
│   ├── MilanSetuApplication.java
│   ├── config/       SecurityConfig, CorsConfig, OpenApiConfig, SchedulingConfig
│   ├── security/     JwtService, JwtAuthenticationFilter, AppUserDetailsService, AccessPolicy, RateLimitFilter
│   ├── entity/       User, Role, Event, HelpDesk, EventStaff, Member, MissingReport, FoundReport,
│   │                 CaseFile, CaseStatus, CaseEvent, HandoverOtp, OtpStatus, Notification, AuditLog
│   ├── repository/
│   ├── dto/request/  RegisterRequest, LoginRequest, MemberRequest, MissingReportRequest, FoundReportRequest, ScanRequest, HandoverRequest
│   ├── dto/response/ ScanResponse (safe), MatchSuggestion (+factors), CaseResponse, TimelineEntry, DashboardSummary
│   ├── service/      AuthService, EventService, MemberService, QrService, ScanService, ReportService,
│   │                 MatchingService, GeoUtils, CaseService, OtpService, DashboardService
│   ├── notification/ NotificationService, NotificationSender, EmailNotificationSender, MockNotificationSender (dev profile)
│   ├── controller/   Auth, Event, Member, Scan, Report, Case, Notification, User, Dashboard
│   ├── scheduler/    OtpExpiryJob, OtpCleanupJob, StaleCaseJob
│   └── exception/    GlobalExceptionHandler, ApiError, BusinessRuleException, ResourceNotFoundException
├── backend/src/main/resources/db/migration/V1__init_schema.sql, V2__demo_data.sql (dev)
├── frontend/  docs/  postman/  infrastructure/{docker,nginx}/  .github/workflows/
└── docker-compose.yml  .env.example  .gitignore  LICENSE  README.md
```

## 6. Learning notes

| Concept | Why it's used here | Interview question |
|---|---|---|
| Composite FK `(report_id, event_id)` | the DB itself refuses to pair reports from different events | "How do you enforce cross-table consistency without triggers?" |
| Generated column + UNIQUE | MySQL has no partial index; NULLs don't collide | "How do you allow only one *active* row per parent?" |
| `@Version` | two desks can't both reunite the same case | "Optimistic vs pessimistic locking?" |
| BCrypt for OTP | a DB leak doesn't reveal live OTPs | "Why hash a 6-digit code that expires in 10 minutes?" (and why the 5-attempt lock matters more) |
| Strategy interface for notifications | swap Mock/Email without changing services | "Explain the Strategy pattern in your project." |
