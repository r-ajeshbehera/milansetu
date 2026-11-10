# MilanSetu — Database Design

Migration: `backend/src/main/resources/db/migration/V1__init_schema.sql` (Flyway). InnoDB, utf8mb4. **Validated on MySQL 8.0.46**, with results in §4.

## 1. Tables

| Table | Purpose | Key rules |
|---|---|---|
| users | All accounts | unique email; BCrypt; role enum; **FAMILY must have phone** (CHECK) |
| events | Festivals | `end_date >= start_date`; inactive by default |
| help_desks | Desks per event | unique (event, name); lat/lng range; `UNIQUE(id, event_id)` as a composite-FK target |
| event_staff | Staff ↔ event (+ home desk) | PK (event, user); desk must belong to the same event (composite FK) |
| members | Registered children/elderly | random UUID `qr_token` UNIQUE; age 0–120; member is bound to one event |
| missing_reports | Family reports | colour palette enum; GPS range; status OPEN/MATCHED/RESOLVED/CLOSED; **only one active report per member** (generated key) |
| found_reports | QR or manual found reports | desk must be in the same event; `QR_SCAN ⇒ member_id`; **only one active found report per member** (stops double-scan duplicates) |
| case_files | One case per person-incident | see §2; `version` for optimistic locking |
| case_events | Timeline | from/to status, note, who, when |
| handover_otps | OTPs | hash only; attempts 0–5; LOCKED ⇒ attempts = 5; USED ⇔ used_at; **only one ACTIVE per case** |
| notifications | In-app inbox | type/channel enums; must never contain an OTP |
| revoked_tokens, audit_logs | Logout and audit trail | |

## 2. How the "case" ambiguity was resolved

The conceptual design had `case_files(missing_report_id, found_report_id)` and both columns could be NULL. That left open questions: can a case exist with neither report? Can a found report sit in two cases? Can a case pair reports from different events?

Decisions:
1. **A case is always opened from a report.** `CHECK (missing_report_id IS NOT NULL OR found_report_id IS NOT NULL)`.
2. **Same event, enforced by the DB.** `case_files.event_id` plus composite FKs `(missing_report_id, event_id)` and `(found_report_id, event_id)`. A pair from two events is impossible.
3. **One live case per report.** Generated columns `canonical_missing_key` / `canonical_found_key` (= report id while `merged_into_id IS NULL`) are UNIQUE.
4. **Matching merges two cases.** The found-side case is CLOSED with `merged_into_id`, and the missing case gets the found report. The full history of both cases is kept. `CHECK (merged_into_id IS NULL OR status='CLOSED')`.
5. **A case can't be MATCHED or later without a pair.** It needs a found report *and* either a missing report or a known registered member (the QR path). `ck_cf_pair` enforces this.
6. **REUNITED needs a timestamp.** It requires `reunited_at`, and it is set only by the OTP service.

## 3. Other deliberate changes to the conceptual SQL

- **`event_staff` (new).** Needed for "event-level authorisation" of VOLUNTEER/DESK/POLICE.
- **`help_desks.in_charge` → `in_charge_id` FK.** An `active` flag was added too.
- **`members.qr_revoked`.** Lets a family revoke a lost wristband and get a new token.
- **`found_reports.source` and `safe_notes`.** For the QR vs manual paths and volunteer notes.
- **`clothing_color` as a fixed palette (12 colours + MULTI).** Free text ("maroon", "dark red") would never match exactly. This is a documented design choice, and the UI shows colour chips.
- **`handover_otps.used` → `status` + `used_at`.** `used = (status = 'USED')`. The status also covers EXPIRED/LOCKED/SUPERSEDED, which the spec needs (lockout, new-OTP policy). `version` and `issued_by` were added.
- **`case_events.from_status`, `case_files.assigned_desk_id` / `assigned_officer_id`, `matched_at`, `closed_at`.** For the police assignment and the dashboard's "average time to reunite".
- **`case_files.stale_flagged_at` (added during implementation).** `StaleCaseJob` sets it once, so a case is flagged only once until its status changes. The job never closes cases.
- **All FKs use RESTRICT.** MySQL forbids CHECKs on CASCADE/SET NULL FK columns, and records are deactivated, not deleted. A configurable retention job anonymises old events.

## 4. MySQL 8.0.46 validation results (actual run)

| # | Test | Result |
|---|---|---|
| 1 | FAMILY user without phone | rejected (`ck_users_family_phone`) |
| 2 | duplicate QR token | rejected (`uq_members_qr`) |
| 3 | second active missing report for the same member | rejected (`uq_mr_active_member`) |
| 4 | missing report using a member from another event | rejected (FK `fk_mr_member`) |
| 5 | found report at a desk of another event | rejected (FK `fk_fr_desk`) |
| 6 | QR_SCAN found report without member | rejected (`ck_fr_qr_member`) |
| 7 | **double QR scan → second active found report** | **rejected (`uq_fr_active_member`)** |
| 8 | clothing colour outside palette | rejected (`ck_fr_color`) |
| 9 | case with no report | rejected (`ck_cf_has_report`) |
| 10 | second live case for the same missing report | rejected (`uq_cf_missing`) |
| 11 | MATCHED with only a missing report | rejected (`ck_cf_pair`) |
| 12 | case pairing reports from different events | rejected (FK `fk_cf_missing`) |
| 13 | merge in wrong order (attach before closing found case) | rejected (`uq_cf_found`) |
| 14 | correct merge in a transaction | accepted: case 1 MATCHED (m=1, f=1), case 2 CLOSED, merged_into=1 |
| 15 | REUNITED without reunited_at | rejected (`ck_cf_reunited`) |
| 16 | first OTP | accepted |
| 17 | **second ACTIVE OTP for the same case** | **rejected (`uq_otp_one_active`)** |
| 18 | attempts = 6 | rejected (`ck_otp_attempts`) |
| 19 | LOCKED with 3 attempts | rejected (`ck_otp_locked`) |
| 20 | USED without used_at | rejected (`ck_otp_used`) |
| 21 | supersede then re-issue | accepted (old SUPERSEDED/NULL key, new ACTIVE) |
| 22 | merged case reopened | rejected (`ck_cf_merged_closed`) |

## 5. Indexes

| Index | Used by |
|---|---|
| `missing_reports(event_id, status)`, `found_reports(event_id, status)` | matching queries (spec) |
| `members(guardian_id, event_id)` | "My Members" and ownership checks |
| `found_reports(desk_id, status)` | desk queue |
| `case_files(event_id, status)` | police/desk case lists, dashboard |
| `case_events(case_id, changed_at)` | timeline |
| `handover_otps(case_id, created_at)`, `(status, expires_at)` | verification, expiry job |
| `notifications(user_id, read_at)` | inbox |
| `event_staff(user_id)` | per-request event authorisation |
