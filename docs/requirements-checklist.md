# MilanSetu — Requirements Checklist

Legend: ✅ done & verified · ⬜ not done

**Phase: implemented and verified. `mvn verify` BUILD SUCCESS, 68/68 tests, coverage 88.7 % (service layer).**

| # | Requirement | Code | Verification |
|---|---|---|---|
| 1 | Auth: register (FAMILY only), login, BCrypt, JWT, roles, logout, profile | ✅ | ✅ `publicRegistrationAlwaysCreatesFamily`, 401/403 tests pass |
| 2 | Events, desks (GPS, in-charge), staff assignment | ✅ | ✅ schema · ✅ API |
| 3 | Members, UUID QR, PNG, printable wristband, owner-only, regenerate | ✅ | ✅ token unit tests · ✅ unique key on MySQL · ✅ API |
| 4 | QR scan: validation, active event, nearest desk, found case, notify, safe response, no duplicates | ✅ | ✅ duplicate-scan DB key · ✅ helpers · ✅ API |
| 5 | Missing report (registered or not), validation | ✅ | ✅ DB checks · ✅ API |
| 6 | Found report without QR | ✅ | ✅ |
| 7 | Weighted matching (boundaries, Haversine, threshold 50, factors, manual confirm) | ✅ | ✅ **22 unit tests run** |
| 8 | Case workflow and timeline (every change → `case_events`) | ✅ | ✅ 7 transition tests · ✅ timeline API |
| 9 | Secure OTP handover (hash, 10 min, 5 attempts, used, rate limit, re-issue, `@Version`) | ✅ | ✅ 8 OTP domain tests · ✅ DB keys · ✅ Mockito + concurrency |
| 10 | Notifications (mock + Spring Mail, no phone/OTP leaks) | ✅ | ✅ |
| 11 | Scheduled jobs (expiry, retention, stale flag; no auto-close) | ✅ | ✅ |
| 12 | Police/admin dashboard (real queries) | ✅ | ✅ |
| — | React frontend: family, volunteer (camera + manual), desk, police, admin | ✅ | ✅ lint + build · ✅ used in the browser against the running stack (screenshots) |
| — | Meaningful tests | ✅ 68 | ✅ 68/68 pass (incl. concurrency, 5/5 repeats) |
| — | ≥ 70 % service coverage | ✅ | ✅ 88.7 % (JaCoCo) |
| — | Docker, Compose (+ Mailpit), Nginx, GitHub Actions | ✅ | ✅ compose stack ran healthy (runtime images); CI workflow ready |
| — | Postman (29 requests) | ✅ | ⬜ not run |
| — | Swagger/OpenAPI | ✅ | ✅ |
| — | Docs (11 + README) | ✅ | ✅ |
| — | Screenshots | ✅ | ✅ from the running app (`docs/screenshots/`) |
| — | Live deployment | ⬜ | not deployed; no URL claimed |
| N1–N5 | Found items, SMS, hotspots, Odia/Hindi, archives | ⬜ | after MVP verification |

## Decisions (approved: "do complete")

| ID | Decision |
|---|---|
| D1 | Spring Boot 3.5.x |
| D2 | Fixed colour palette |
| D3 | Merge on confirm |
| D4 | Handover only at AT_DESK |
| D5 | Members registered per event |
| D6 | Found-before-last-seen = 0 points |
| D7 | Stale cases are flagged only |
| D8 | Mailpit + mock, no SMS |
| D9 | TypeScript frontend |
