# Interview Preparation: MilanSetu

## 60-second pitch

"MilanSetu helps reunite lost children and elderly people with their families at crowded festivals like Rath Yatra. It's built with Java 21, Spring Boot, Spring Security JWT, JPA/Hibernate, MySQL with Flyway, and React.
- Families register members and print a QR wristband. The QR holds only a random UUID, never a phone number.
- When a volunteer scans it, a case opens and the family is notified, but the volunteer never sees their contact details.
- For people without wristbands, a weighted matching engine ranks found reports against missing reports by gender, age, clothing, Haversine distance and time, and explains each score.
- A desk officer confirms the match, and the child is handed over only after the guardian gives a one-time OTP. The OTP is stored as a BCrypt hash, expires in 10 minutes and locks after 5 wrong tries.
- Optimistic locking stops two desks from completing the same handover."

## Matching algorithm (explain on a whiteboard)

1. Candidates: same event and `status = OPEN`, using the `(event_id, status)` index.
2. Hard filter: gender must match, and two different registered members can never match.
3. Points:
   - gender 25
   - age ≤2 → 25, ≤5 → 12
   - clothing 20
   - distance ≤1 km → 20, ≤3 km → 10
   - time ≤2 h → 10, ≤6 h → 5
4. Show only scores of 50 or more, sorted by score then distance, with the factor breakdown.
5. *Why deterministic and not ML?* It's explainable to desk staff and police, testable at every boundary, and needs no training data. A wrong match with a child is dangerous, so a human always confirms.
6. *Why a fixed colour palette?* Free text ("maroon" vs "dark red") would never match exactly.
7. *Time edge cases:* I use `Duration.between` on full timestamps (works across midnight). Found-before-last-seen gives 0 points.

## OTP handover

- **Generate:** `SecureRandom`, `%06d` (keeps leading zeros), BCrypt hash, expiry now + 10 min. Any old ACTIVE code is set to SUPERSEDED first; a DB unique key allows only one ACTIVE code.
- **Deliver:** only through `NotificationSender` (mock or email). The API response contains only `expiresAt`.
- **Verify:** the case must be `AT_DESK`.
  - The rules live in `HandoverOtp.verify()`: used → reject, locked → reject, expired → EXPIRED, wrong → attempts + 1, 5th wrong → LOCKED, correct → USED.
  - The service method is `@Transactional(noRollbackFor = OtpRejectedException.class)`, so the failed-attempt counter is saved even though an exception is thrown.
- **Concurrency:** `@Version` on the case and the OTP. Two desks verifying at once: one commits, the other's UPDATE matches 0 rows → 409. Proven by `ConcurrentHandoverIT`.
- *Why hash a 6-digit code?* So a database leak can't be used during the 10-minute window. The real protection against guessing is the 5-attempt lock plus rate limiting (10⁶ combinations, 5 tries = 0.0005 %).

## Data model questions

- *How do you stop a report from being in two cases?* Generated column + UNIQUE: MySQL's version of a partial unique index.
- *How do you stop pairing reports from different events?* Composite foreign keys `(report_id, event_id)`.
- *What happens on match confirmation?* The found-side case is closed with `merged_into_id`, then the found report is attached to the missing case. The order matters because of the unique keys, and it's tested.

## Privacy questions

- *How does a volunteer contact the family?* They don't. The system notifies the guardian; the volunteer only learns which desk to go to.
- *What does a scan with a wrong token reveal?* Nothing: the same generic 404 for invalid, revoked or inactive tokens.

## Be honest about

- The backend has not yet been built with Maven in this environment (blocked network). 43 core unit tests were run with real `javac`.
- There's no SMS (paid gateway).
- It's not deployed yet.
