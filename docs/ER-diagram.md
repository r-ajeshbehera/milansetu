# MilanSetu — ER Diagram

Source of truth: `backend/src/main/resources/db/migration/V1__init_schema.sql`. Audit timestamps are left out of the diagram.

```mermaid
erDiagram
    USERS ||--o{ EVENTS : "creates"
    EVENTS ||--o{ HELP_DESKS : "has"
    USERS ||--o{ HELP_DESKS : "in-charge of"
    EVENTS ||--o{ EVENT_STAFF : "staffed by"
    USERS ||--o{ EVENT_STAFF : "works at"
    HELP_DESKS ||--o{ EVENT_STAFF : "home desk"
    USERS ||--o{ MEMBERS : "guardian of"
    EVENTS ||--o{ MEMBERS : "registered for"
    EVENTS ||--o{ MISSING_REPORTS : "has"
    MEMBERS ||--o{ MISSING_REPORTS : "reported missing"
    USERS ||--o{ MISSING_REPORTS : "reports"
    EVENTS ||--o{ FOUND_REPORTS : "has"
    HELP_DESKS ||--o{ FOUND_REPORTS : "brought to"
    MEMBERS ||--o{ FOUND_REPORTS : "found via QR"
    MISSING_REPORTS ||--o{ CASE_FILES : "missing side"
    FOUND_REPORTS ||--o{ CASE_FILES : "found side"
    MEMBERS ||--o{ CASE_FILES : "person"
    CASE_FILES ||--o{ CASE_FILES : "merged into"
    CASE_FILES ||--o{ CASE_EVENTS : "timeline"
    CASE_FILES ||--o{ HANDOVER_OTPS : "handover"
    USERS ||--o{ NOTIFICATIONS : "receives"
    CASE_FILES ||--o{ NOTIFICATIONS : "about"

    USERS { bigint id PK
        varchar full_name
        varchar email UK
        varchar phone "private"
        char password_hash
        varchar role }
    EVENTS { bigint id PK
        varchar name UK
        varchar city
        date start_date
        date end_date
        boolean active }
    HELP_DESKS { bigint id PK
        bigint event_id FK
        varchar name
        decimal latitude
        decimal longitude
        bigint in_charge_id FK }
    EVENT_STAFF { bigint event_id PK,FK
        bigint user_id PK,FK
        bigint desk_id FK }
    MEMBERS { bigint id PK
        bigint guardian_id FK
        bigint event_id FK
        varchar full_name
        smallint age
        varchar gender
        varchar notes "private"
        char qr_token UK "random UUID" }
    MISSING_REPORTS { bigint id PK
        bigint event_id FK
        bigint reported_by FK
        bigint member_id FK
        varchar person_name
        smallint approx_age
        varchar gender
        varchar clothing_color
        decimal last_seen_lat
        decimal last_seen_lng
        datetime last_seen_at
        varchar status }
    FOUND_REPORTS { bigint id PK
        bigint event_id FK
        bigint desk_id FK
        bigint reported_by FK
        bigint member_id FK
        varchar source "QR_SCAN or MANUAL"
        smallint approx_age
        varchar gender
        varchar clothing_color
        decimal found_lat
        decimal found_lng
        datetime found_at
        varchar status }
    CASE_FILES { bigint id PK
        bigint event_id FK
        bigint missing_report_id FK
        bigint found_report_id FK
        bigint member_id FK
        tinyint match_score
        varchar status
        bigint merged_into_id FK
        datetime opened_at
        datetime reunited_at
        bigint version "optimistic lock" }
    CASE_EVENTS { bigint id PK
        bigint case_id FK
        varchar from_status
        varchar status
        varchar note
        bigint changed_by FK
        datetime changed_at }
    HANDOVER_OTPS { bigint id PK
        bigint case_id FK
        char otp_hash "BCrypt"
        datetime expires_at
        tinyint attempts "max 5"
        varchar status
        bigint active_case_key "generated UNIQUE" }
    NOTIFICATIONS { bigint id PK
        bigint user_id FK
        bigint case_id FK
        varchar type
        varchar channel
        varchar message "never OTP" }
```

Supporting tables not drawn: `revoked_tokens` and `audit_logs`.
