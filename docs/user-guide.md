# User Guide

Run it locally (README), open http://localhost:8082, and use a demo account. The password is `Demo@1234`, dev only.

| Role | Demo account |
|---|---|
| Family (Mohanty: Riya 6, Bhagaban 74) | family1@milansetu.demo |
| Family (Sahu: Arjun 9) | family2@milansetu.demo |
| Volunteer | volunteer@milansetu.demo |
| Singhadwara help desk | desk1@milansetu.demo |
| Gundicha help desk | desk2@milansetu.demo |
| Police control | police@milansetu.demo |
| Event admin | admin@milansetu.demo |

Demo wristband codes for **manual entry** on the volunteer page:
- Riya: `3f2a9c1e-7b4d-4e8a-9c21-5d6f7a8b9c01`
- Bhagaban: `8d1e2f3a-4b5c-4d6e-8f70-1a2b3c4d5e02`

## Family

1. **Register** (or use a demo family).
2. **My members:** add a child or elderly relative. Then *Download PNG* or *Print wristband*. The QR contains only a random code.
3. If someone is lost, use **Report missing**: name, age, gender, clothing colour, where and when they were last seen.
4. **My cases:** follow the timeline. When a match is confirmed you receive a 6-digit code. In dev it appears under *Notifications → simulated inbox*. Give it **only** to desk staff.

## Volunteer

- **Scan & register:** start the camera and scan the wristband, or type the code.
- You see only the first name, age, gender and which desk to take the person to.
- No wristband? Use *Register found person*.

## Help desk

1. **Desk cases:** open a missing-person case to see **match suggestions**. Each shows its score and reasons, side by side with the report.
2. *Confirm this match*: the guardian gets an OTP.
   - Wristband cases: *Confirm identity* instead.
3. When the person and guardian are both present, click *Mark at desk*.
4. Type the guardian's code, then *Verify & hand over*. The case becomes **REUNITED**.
5. After 5 wrong codes the OTP locks; use *Issue new OTP*.

## Police / Admin

- **Dashboard:** open cases, reunited today, missing/found totals, awaiting confirmation, average time to reunite, trend and desk charts.
- **Cases:** filter, assign to a desk, close with a reason.
- **Admin:**
  - Events & desks: create an event, add desks with GPS and an in-charge, assign staff, activate.
  - Users: create staff accounts.
