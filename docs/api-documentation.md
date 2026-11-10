# MilanSetu — REST API Specification (Phase 1, for review)

Base `/api`. Bearer JWT. Lists take `page`, `size` (≤100) and `sort`.

**Roles:** FAM, VOL, DESK, POL, ADM, PUB (no login).
- "(own)" = the server checks ownership.
- "(event)" = the caller must be staff of that event (or ADMIN).

## Auth and profile

| Method | Path | Roles | Notes |
|---|---|---|---|
| POST | `/auth/register` | PUB | `{fullName, email, phone, password}` → **always FAMILY**; rate-limited |
| POST | `/auth/login` | PUB | → `{accessToken, expiresIn, user}`; 5 failures per 15 min per IP+email → 429 |
| POST | `/auth/logout` | any | revoke `jti` |
| GET / PATCH | `/users/me` | any | own profile (phone visible to self only) |
| POST | `/users` | ADM | create VOL/DESK/POL/ADM |
| GET / PATCH | `/users`, `/users/{id}` | ADM | list, activate/deactivate, change role |

## Events, desks, staff

| Method | Path | Roles | Notes |
|---|---|---|---|
| POST | `/events` | ADM | `{name, city, startDate, endDate}` |
| GET | `/events`, `/events/{id}` | any authenticated | FAM sees active events for registration |
| PATCH | `/events/{id}` | ADM | edit / `active` toggle |
| POST | `/events/{id}/desks` | ADM | `{name, latitude, longitude, inChargeId?}` |
| GET | `/events/{id}/desks` | any authenticated | public-safe fields only |
| PATCH | `/desks/{id}` | ADM | edit, assign in-charge |
| PUT / DELETE | `/events/{id}/staff/{userId}` | ADM | `{deskId?}` assign / unassign staff |

## Members and QR

| Method | Path | Roles | Notes |
|---|---|---|---|
| POST | `/members` | FAM | `{eventId, fullName, age, gender, notes?}` → 201 with member (token not shown in list views) |
| GET | `/members` | FAM (own) | my members |
| GET | `/members/{id}` | FAM (own) | |
| GET | `/members/{id}/qr` | FAM (own) | `image/png` QR. Payload = `MILANSETU:<uuid>` only |
| GET | `/members/{id}/wristband` | FAM (own) | printable HTML/PDF strip: QR, first name, event, "Show this to any help desk". No phone number |
| POST | `/members/{id}/qr/regenerate` | FAM (own) | revoke old token, issue new one |

## Scan

| Method | Path | Roles | Notes |
|---|---|---|---|
| POST | `/scan/{qrToken}` | VOL (event), DESK (event) | body `{lat?, lng?, clothingColor?}` → 201 new case / 200 existing active case. Response: `{caseId, firstName, approxAge, gender, desk{name, lat, lng, distanceKm}, instructions}`. Invalid/revoked/inactive → generic 404. Rate-limited per user |

## Reports and matching

| Method | Path | Roles | Notes |
|---|---|---|---|
| POST | `/reports/missing` | FAM | `{eventId, memberId?, personName, approxAge, gender, clothingColor, lastSeenLat, lastSeenLng, lastSeenAt}` → report + case. `lastSeenAt` can't be in the future |
| GET | `/reports/missing` | FAM (own), DESK/POL (event) | filter `status` |
| POST | `/reports/found` | VOL (event), DESK (event) | manual found report (no QR) → report + case |
| GET | `/reports/found` | DESK/POL (event) | |
| GET | `/reports/missing/{id}/matches` | DESK (event), POL (event) | `[{foundReportId, score, factors:[{factor, points, detail}], distanceKm, minutesApart}]`, score ≥ 50, sorted |

## Cases

| Method | Path | Roles | Notes |
|---|---|---|---|
| GET | `/cases` | FAM (own), DESK/POL (event) | `?eventId&status&deskId&from&to` |
| GET | `/cases/{id}` | same | DTO depends on role (FAM sees no volunteer identity; staff never see phone/notes) |
| GET | `/cases/{id}/timeline` | same | ordered case events |
| POST | `/cases/{id}/confirm-match` | DESK (event) | `{foundReportId?, note}` (empty body for a QR member case) → MATCHED + OTP issued and sent to guardian |
| POST | `/cases/{id}/reject-match` | DESK (event) | MATCHED → REPORTED, unlink |
| POST | `/cases/{id}/arrived` | DESK (event) | MATCHED → AT_DESK |
| POST | `/cases/{id}/otp` | DESK (event) | re-issue OTP (supersedes old one; max 3 per hour) → `{expiresAt}` **only** |
| POST | `/cases/{id}/handover` | DESK (event) | `{otp}` → REUNITED. Wrong: 422 `{attemptsLeft}`; locked/expired/used: 409 `OTP_LOCKED` / `OTP_EXPIRED` / `OTP_USED`; concurrent: 409 |
| POST | `/cases/{id}/assign` | POL (event) | `{deskId?, officerId?}` |
| POST | `/cases/{id}/close` | POL (event); FAM (own, REPORTED only) | `{note}` (required) |

## Notifications and dashboard

| Method | Path | Roles | Notes |
|---|---|---|---|
| GET | `/notifications` | any (own) | inbox |
| POST | `/notifications/{id}/read` | any (own) | |
| GET | `/dashboard/summary?eventId&from&to&status` | POL (event), ADM | openCases, reunitedToday, totalMissing, totalFound, awaitingConfirmation, avgMinutesToReunite, byDesk[], trend[] |
| GET | `/dev/outbox` | **dev profile only** | mock-delivered messages for local demo; the bean doesn't exist in prod |

## Error format

`{timestamp, status, code, message, path, fieldErrors?}`

| Status | Meaning |
|---|---|
| 400 | validation error |
| 401 | not logged in |
| 403 | wrong role |
| 404 | not found or not yours |
| 409 | state conflict |
| 422 | business rule broken |
| 429 | rate limit |

Messages never reveal private data.
