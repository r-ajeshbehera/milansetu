# Matching Algorithm

**Code:**
- `MatchScorer`: pure Java, 19 unit tests
- `MatchingService.rank()`: 3 unit tests
- `GET /api/reports/missing/{id}/matches`

## 1. Candidate filter (SQL)

```sql
SELECT ... FROM found_reports WHERE event_id = :event AND status = 'OPEN'
```

This uses the `idx_fr_event_status (event_id, status)` index. Then, in Java:
- If both reports belong to **different registered members**, skip (they can't be the same person).
- If the genders differ, skip (hard filter).

## 2. Score (max 100). Only the highest step per factor counts; boundaries are inclusive.

| Factor | Points | Implementation |
|---|---:|---|
| Same gender | 25 (mandatory) | enum equality |
| Age difference ≤ 2 | 25 | `abs(a - b)` |
| Age difference ≤ 5 | 12 | |
| Same clothing colour | 20 | fixed 12-colour palette, so exact match is meaningful |
| Distance ≤ 1 km | 20 | Haversine, R = 6371 km |
| Distance ≤ 3 km | 10 | |
| Found within 2 h | 10 | `Duration.between(lastSeenAt, foundAt)`: full timestamps, works across midnight |
| Found within 6 h | 5 | negative difference (found *before* last seen) = 0 points |

## 3. Threshold and order

- Show only suggestions with a score of **50 or more**.
- Sort by score descending; ties go to the nearer report.
- Each suggestion lists its **factors** with points and a reason, for example `AGE +12 — age difference 3 (≤ 5)`, so desk staff can see why it was suggested.

## 4. Safety rules

- Matching **never** changes data.
- `POST /cases/{id}/confirm-match` is a separate human action by a DESK user.
- Even after confirmation, the case becomes REUNITED only after OTP verification at the desk (`OtpService`).
- Confirmation also refuses mismatched genders and two different registered members.

## Worked example (demo data)

The missing report is Arjun:
- 9 years, MALE, YELLOW
- last seen at 19.806, 85.820 at 10:00

The found report:
- ~10 years, MALE, YELLOW
- found at 19.812, 85.829 at 11:30

| Factor | Value | Points |
|---|---|---:|
| Gender | MALE = MALE | 25 |
| Age | difference 1 | 25 |
| Clothing | YELLOW = YELLOW | 20 |
| Distance | 1.15 km | 10 |
| Time | 90 min | 10 |
| **Total** | | **90** |

This is checked in `ReunificationWorkflowIT`.

## Boundary tests (executed)

- age difference 2 / 3 / 5 / 6
- clothing match / mismatch
- distance 0.99 / 1.01 / 2.99 / 3.06 km
- time 120 / 121 / 360 / 361 / −30 minutes
- across midnight
- threshold 49 / 50
- gender filter
- factor breakdown
- ranking order and tie-break
