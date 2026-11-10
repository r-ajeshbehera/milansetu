# Code Walkthrough: learn MilanSetu class by class

For each class: **responsibility → why it exists → depends on → used by → interview question.**

## Domain (`entity/`)

| Class | Responsibility | Interview question |
|---|---|---|
| `Member` | Registered person. Creates a random UUID token in its constructor. `regenerateToken()`, `firstName()`. | Why not encode the member id in the QR? (enumeration and privacy) |
| `MissingReport`, `FoundReport` | The two kinds of report. `FoundReport.source` is QR_SCAN or MANUAL. | Why keep two tables instead of one "report" table? |
| `CaseFile` | The case aggregate: `moveTo()` (validated transitions), `markReunited()` (AT_DESK only), `mergeInto()`, `attachFound()`, `detachFound()`, `guardian()`, `@Version`. | Why is REUNITED not reachable through `moveTo()`? |
| `CaseStatus` | Transition table `allowedNext()` | How would you add a new status safely? |
| `HandoverOtp` | `verify(code, now, matcher, maxAttempts)`, which holds all the OTP rules. The matcher is passed in, so the rules are testable without BCrypt or Spring. | Why pass a `Predicate` instead of a `PasswordEncoder`? (dependency inversion, testability) |
| `CaseEvent` | One timeline row per status change | Append-only audit vs updating a status column? |
| `Notification` | In-app inbox row. Never contains an OTP. | — |

## Pure logic (`service/`, no Spring needed)

| Class | Responsibility |
|---|---|
| `MatchScorer` | Weighted score + factor list. 19 boundary tests. |
| `MatchingService.rank()` | Filters, scores, applies the threshold, sorts |
| `GeoUtils` | Haversine |
| `DeskLocator` | Nearest active desk |
| `OtpGenerator` | SecureRandom, 6 digits |
| `QrTokens` | `MILANSETU:` payload format and strict UUID parsing |

Talking point: keeping the important rules in plain Java makes them fast to unit test and easy to explain.

## Application services (`service/`, Spring)

| Class | What it orchestrates |
|---|---|
| `AuthService` | Register (always FAMILY), login, logout (`jti` revocation) |
| `EventService` | Events, desks (in-charge auto-added to staff), staff assignment |
| `MemberService` | Owner-only member CRUD, QR PNG (`QrService` → ZXing), printable wristband HTML (escaped) |
| `ScanService` | Token → member → active event → event staff → nearest/home desk → duplicate check → found report + case + timeline + notification → **safe** `ScanResponse` |
| `ReportService` | Missing reports (FAMILY) and found reports without QR (VOLUNTEER/DESK) |
| `CaseService` | Lists (family vs staff scope), timeline (including merged cases), confirm, reject, arrived, assign, close |
| `OtpService` | Issue and verify. `noRollbackFor` keeps failed-attempt counts. |
| `DashboardService` | Aggregates for police and admin |
| `CaseMapper` | Builds role-appropriate DTOs (families don't see staff names or found coordinates) |

## Notifications (`notification/`): Strategy pattern

- `NotificationSender` is the interface. Implementations: `MockNotificationSender` (in-memory outbox, `mode=mock`) and `EmailNotificationSender` (Spring Mail, `mode=email`).
- `NotificationService` writes the in-app row and calls the sender. Failures are logged; they never break a reunion.

## Security (`security/`)

- `JwtService`, `JwtAuthenticationFilter`, `RateLimitFilter` (login/register/scan/otp/handover).
- `AccessPolicy` holds ownership and event-scope rules, and returns 404 for anything out of scope.

## Controllers

- Thin: `@Valid` + `@PreAuthorize` + one service call.
- `ScanController` returns 201 for a new case and 200 for an existing one.
- `DevOutboxController` exists only in the dev profile.

## Schedulers

- `OtpExpiryJob`: every minute
- `OtpCleanupJob`: daily retention
- `StaleCaseJob`: flags stale cases and notifies the desk in-charge. **Never** auto-closes.

## Frontend (`frontend/src`)

| Part | What it does |
|---|---|
| `pages/family/*` | dashboard, members + QR (fetched as a blob because it needs auth), report missing |
| `pages/staff/VolunteerPage` + `QrScanner` | html5-qrcode camera, manual entry fallback, safe result card |
| `pages/staff/CaseDetail` | role-aware actions: suggestions side by side, confirm, arrived, OTP input, assign/close, timeline |
| `pages/staff/PoliceDashboard` | Recharts trend and per-desk charts |
| `pages/admin/*` | events, desks, staff, users |
