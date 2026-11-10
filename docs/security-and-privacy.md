# Security and Privacy

This app handles information about children and elderly people. Privacy is a requirement, not a feature.

| Control | Implementation |
|---|---|
| Passwords | BCrypt (strength 12) |
| JWT | HS256 with 30-minute expiry. Secret comes from an env var and must be at least 32 bytes. Logout revokes the `jti`. The user is reloaded on every request. |
| Registration | `/auth/register` has **no role field** and always creates FAMILY. A test sends `"role":"ADMIN"` and still gets FAMILY. |
| Staff accounts | Created only by ADMIN |
| Event-level authorisation | `event_staff` table + `AccessPolicy.assertEventRole`. Staff only see their events. |
| Ownership | Families only see their own members, reports and cases. Other records return **404**. |
| Guardian phone protection | Only `ProfileResponse` (the caller's own profile) has a phone field. Scan, case and report DTOs have none. Tests assert the phone string never appears in desk or volunteer responses. |
| Private notes | `members.notes` is returned only to the owner (tested: "peanuts" never appears for desk users) |
| QR token | Random UUID v4, unique, separate from the database id. Payload is `MILANSETU:<uuid>`. A lost wristband can be regenerated. |
| Scan errors | Invalid, revoked and inactive-event tokens all return the same generic message |
| OTP | `SecureRandom`, 6 digits, BCrypt hash only, 10-minute expiry, 5 attempts then LOCKED, single use, at most 3 issues per case per hour, one ACTIVE OTP per case (DB unique key) |
| OTP delivery | Only through `NotificationSender` to the guardian. It is never in API responses (`OtpIssuedResponse` has only `expiresAt`), never in notifications rows, never logged. |
| Double handover | `@Version` on `case_files` and `handover_otps` + a concurrency test |
| Rate limits | login/register 5 per minute; scan, OTP and handover 20 per minute per IP |
| Input validation | Bean Validation (GPS ranges, age 0–120, `@PastOrPresent`, 10-digit mobile), plus DB CHECKs |
| Errors | One `ApiError` format with no stack traces or SQL |
| Audit | `audit_logs` (QR_SCANNED, OTP_ISSUED, OTP_FAILED, OTP_LOCKED, HANDOVER_COMPLETED...) with no personal data. Every case status change goes to `case_events`. |
| Retention | Old OTP rows are deleted after `OTP_RETENTION_DAYS` (default 30). Event data can be anonymised after an event (documented procedure). |
| Headers | nosniff, frame DENY, no-referrer, CSP, and `Permissions-Policy: camera=(self)` for the scanner |
| HTTPS | Required in production. Browsers only allow the camera on HTTPS or localhost. |
| Dev-only inbox | `/api/dev/outbox` exists only with the `dev` profile **and** mock notifications, and shows each user only their own messages |

## Not an official system

This is a portfolio project. Real use with children would need organisational, legal (data protection, child-safeguarding), privacy and operational validation, plus trained staff.

## Known limitations

- The rate limiter is in-memory, so it works per instance.
- The JWT is kept in sessionStorage.
- Photos are not supported (by design). If added, they must use private storage.
- SMS is not implemented, because a paid gateway would be needed.
