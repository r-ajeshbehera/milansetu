-- =====================================================================
-- MilanSetu - V1 initial schema (MySQL 8, InnoDB, utf8mb4)
-- Validated on MySQL 8.0.46. All FKs use the default RESTRICT action
-- (MySQL forbids CHECK constraints on columns used by CASCADE/SET NULL FKs).
-- =====================================================================

CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    full_name      VARCHAR(120) NOT NULL,
    email          VARCHAR(160) NOT NULL,
    phone          VARCHAR(15)  NULL,      -- required for FAMILY (validated in service); never returned to staff
    password_hash  CHAR(60)     NOT NULL,  -- BCrypt
    role           VARCHAR(12)  NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('FAMILY','VOLUNTEER','DESK','POLICE','ADMIN')),
    CONSTRAINT ck_users_family_phone CHECK (role <> 'FAMILY' OR phone IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE events (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(120) NOT NULL,
    city        VARCHAR(80)  NOT NULL,
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by  BIGINT       NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_events PRIMARY KEY (id),
    CONSTRAINT uq_events_name UNIQUE (name),
    CONSTRAINT fk_events_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_events_dates CHECK (end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE help_desks (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    event_id     BIGINT        NOT NULL,
    name         VARCHAR(120)  NOT NULL,
    latitude     DECIMAL(9,6)  NOT NULL,
    longitude    DECIMAL(9,6)  NOT NULL,
    in_charge_id BIGINT        NULL,      -- user with role DESK (checked in service)
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_help_desks PRIMARY KEY (id),
    CONSTRAINT uq_desks_event_name UNIQUE (event_id, name),
    CONSTRAINT uq_desks_id_event UNIQUE (id, event_id),          -- target for composite FKs
    CONSTRAINT fk_desks_event FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_desks_incharge FOREIGN KEY (in_charge_id) REFERENCES users (id),
    CONSTRAINT ck_desks_lat CHECK (latitude  BETWEEN -90  AND 90),
    CONSTRAINT ck_desks_lng CHECK (longitude BETWEEN -180 AND 180)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Which staff (VOLUNTEER / DESK / POLICE) work at which event => event-level authorisation
CREATE TABLE event_staff (
    event_id    BIGINT    NOT NULL,
    user_id     BIGINT    NOT NULL,
    desk_id     BIGINT    NULL,          -- home desk for VOLUNTEER / DESK users
    assigned_by BIGINT    NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_event_staff PRIMARY KEY (event_id, user_id),
    CONSTRAINT fk_es_event FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_es_user  FOREIGN KEY (user_id)  REFERENCES users (id),
    CONSTRAINT fk_es_desk  FOREIGN KEY (desk_id, event_id) REFERENCES help_desks (id, event_id),
    CONSTRAINT fk_es_assigned_by FOREIGN KEY (assigned_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_event_staff_user ON event_staff (user_id);

CREATE TABLE members (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    guardian_id  BIGINT       NOT NULL,
    event_id     BIGINT       NOT NULL,
    full_name    VARCHAR(120) NOT NULL,
    age          SMALLINT     NOT NULL,
    gender       VARCHAR(10)  NOT NULL,
    notes        VARCHAR(500) NULL,      -- private: never shown to volunteers
    qr_token     CHAR(36)     NOT NULL,  -- random UUID v4, not the member id
    qr_revoked   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_members PRIMARY KEY (id),
    CONSTRAINT uq_members_qr UNIQUE (qr_token),
    CONSTRAINT uq_members_id_event UNIQUE (id, event_id),
    CONSTRAINT fk_members_guardian FOREIGN KEY (guardian_id) REFERENCES users (id),
    CONSTRAINT fk_members_event FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT ck_members_age CHECK (age BETWEEN 0 AND 120),
    CONSTRAINT ck_members_gender CHECK (gender IN ('MALE','FEMALE','OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_members_guardian ON members (guardian_id, event_id);

CREATE TABLE missing_reports (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    event_id        BIGINT        NOT NULL,
    reported_by     BIGINT        NOT NULL,
    member_id       BIGINT        NULL,      -- NULL when the person was not pre-registered
    person_name     VARCHAR(120)  NOT NULL,
    approx_age      SMALLINT      NOT NULL,
    gender          VARCHAR(10)   NOT NULL,
    clothing_color  VARCHAR(12)   NOT NULL,  -- fixed palette => exact matching works
    last_seen_lat   DECIMAL(9,6)  NOT NULL,
    last_seen_lng   DECIMAL(9,6)  NOT NULL,
    last_seen_at    DATETIME      NOT NULL,
    status          VARCHAR(10)   NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- one active missing report per registered member
    active_member_key BIGINT AS (IF(status IN ('OPEN','MATCHED'), member_id, NULL)) STORED,
    CONSTRAINT pk_missing_reports PRIMARY KEY (id),
    CONSTRAINT uq_mr_id_event UNIQUE (id, event_id),
    CONSTRAINT uq_mr_active_member UNIQUE (active_member_key),
    CONSTRAINT fk_mr_event    FOREIGN KEY (event_id)    REFERENCES events (id),
    CONSTRAINT fk_mr_reporter FOREIGN KEY (reported_by) REFERENCES users (id),
    CONSTRAINT fk_mr_member   FOREIGN KEY (member_id, event_id) REFERENCES members (id, event_id),
    CONSTRAINT ck_mr_age    CHECK (approx_age BETWEEN 0 AND 120),
    CONSTRAINT ck_mr_gender CHECK (gender IN ('MALE','FEMALE','OTHER')),
    CONSTRAINT ck_mr_color  CHECK (clothing_color IN ('RED','ORANGE','YELLOW','GREEN','BLUE','PURPLE','PINK','WHITE','BLACK','GREY','BROWN','MULTI')),
    CONSTRAINT ck_mr_lat    CHECK (last_seen_lat BETWEEN -90 AND 90),
    CONSTRAINT ck_mr_lng    CHECK (last_seen_lng BETWEEN -180 AND 180),
    CONSTRAINT ck_mr_status CHECK (status IN ('OPEN','MATCHED','RESOLVED','CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_mr_event_status ON missing_reports (event_id, status);
CREATE INDEX idx_mr_reporter     ON missing_reports (reported_by);

CREATE TABLE found_reports (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    event_id        BIGINT        NOT NULL,
    desk_id         BIGINT        NOT NULL,  -- desk the person is taken to (nearest desk if volunteer)
    reported_by     BIGINT        NOT NULL,
    member_id       BIGINT        NULL,      -- set when found via QR scan
    source          VARCHAR(8)    NOT NULL,
    approx_age      SMALLINT      NOT NULL,
    gender          VARCHAR(10)   NOT NULL,
    clothing_color  VARCHAR(12)   NOT NULL,
    found_lat       DECIMAL(9,6)  NOT NULL,
    found_lng       DECIMAL(9,6)  NOT NULL,
    found_at        DATETIME      NOT NULL,
    safe_notes      VARCHAR(500)  NULL,
    status          VARCHAR(10)   NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- prevents duplicate active found cases for the same member (double scan)
    active_member_key BIGINT AS (IF(status IN ('OPEN','MATCHED'), member_id, NULL)) STORED,
    CONSTRAINT pk_found_reports PRIMARY KEY (id),
    CONSTRAINT uq_fr_id_event UNIQUE (id, event_id),
    CONSTRAINT uq_fr_active_member UNIQUE (active_member_key),
    CONSTRAINT fk_fr_event    FOREIGN KEY (event_id)    REFERENCES events (id),
    CONSTRAINT fk_fr_desk     FOREIGN KEY (desk_id, event_id)   REFERENCES help_desks (id, event_id),
    CONSTRAINT fk_fr_reporter FOREIGN KEY (reported_by) REFERENCES users (id),
    CONSTRAINT fk_fr_member   FOREIGN KEY (member_id, event_id) REFERENCES members (id, event_id),
    CONSTRAINT ck_fr_source CHECK (source IN ('QR_SCAN','MANUAL')),
    CONSTRAINT ck_fr_qr_member CHECK (source <> 'QR_SCAN' OR member_id IS NOT NULL),
    CONSTRAINT ck_fr_age    CHECK (approx_age BETWEEN 0 AND 120),
    CONSTRAINT ck_fr_gender CHECK (gender IN ('MALE','FEMALE','OTHER')),
    CONSTRAINT ck_fr_color  CHECK (clothing_color IN ('RED','ORANGE','YELLOW','GREEN','BLUE','PURPLE','PINK','WHITE','BLACK','GREY','BROWN','MULTI')),
    CONSTRAINT ck_fr_lat    CHECK (found_lat BETWEEN -90 AND 90),
    CONSTRAINT ck_fr_lng    CHECK (found_lng BETWEEN -180 AND 180),
    CONSTRAINT ck_fr_status CHECK (status IN ('OPEN','MATCHED','RESOLVED','CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_fr_event_status ON found_reports (event_id, status);
CREATE INDEX idx_fr_desk         ON found_reports (desk_id, status);

-- A case starts from ONE report (missing or found). When desk staff confirm a match,
-- the found report is attached to the missing report's case and the found-side case
-- is closed with merged_into_id set. Composite FKs force both reports and the case
-- to belong to the same event. "Canonical" (= not merged) uniqueness keys guarantee
-- a report can never sit in two live cases.
CREATE TABLE case_files (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    event_id            BIGINT       NOT NULL,
    missing_report_id   BIGINT       NULL,
    found_report_id     BIGINT       NULL,
    member_id           BIGINT       NULL,     -- registered person (QR or registered missing report)
    match_score         TINYINT      NULL,     -- set when a scored suggestion is confirmed
    status              VARCHAR(10)  NOT NULL DEFAULT 'REPORTED',
    assigned_desk_id    BIGINT       NULL,
    assigned_officer_id BIGINT       NULL,
    merged_into_id      BIGINT       NULL,
    opened_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    matched_at          DATETIME(6)  NULL,
    reunited_at         DATETIME(6)  NULL,
    closed_at           DATETIME(6)  NULL,
    stale_flagged_at    DATETIME(6)  NULL,     -- set by StaleCaseJob (flag only, never auto-close)
    version             BIGINT       NOT NULL DEFAULT 0,   -- JPA @Version: blocks double handover
    canonical_missing_key BIGINT AS (IF(merged_into_id IS NULL, missing_report_id, NULL)) STORED,
    canonical_found_key   BIGINT AS (IF(merged_into_id IS NULL, found_report_id,   NULL)) STORED,
    CONSTRAINT pk_case_files PRIMARY KEY (id),
    CONSTRAINT uq_cf_missing UNIQUE (canonical_missing_key),
    CONSTRAINT uq_cf_found   UNIQUE (canonical_found_key),
    CONSTRAINT fk_cf_event   FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_cf_missing FOREIGN KEY (missing_report_id, event_id) REFERENCES missing_reports (id, event_id),
    CONSTRAINT fk_cf_found   FOREIGN KEY (found_report_id,   event_id) REFERENCES found_reports (id, event_id),
    CONSTRAINT fk_cf_member  FOREIGN KEY (member_id, event_id)         REFERENCES members (id, event_id),
    CONSTRAINT fk_cf_desk    FOREIGN KEY (assigned_desk_id, event_id)  REFERENCES help_desks (id, event_id),
    CONSTRAINT fk_cf_officer FOREIGN KEY (assigned_officer_id) REFERENCES users (id),
    CONSTRAINT fk_cf_merged  FOREIGN KEY (merged_into_id) REFERENCES case_files (id),
    CONSTRAINT ck_cf_has_report CHECK (missing_report_id IS NOT NULL OR found_report_id IS NOT NULL),
    CONSTRAINT ck_cf_status CHECK (status IN ('REPORTED','MATCHED','AT_DESK','REUNITED','CLOSED')),
    -- MATCHED / AT_DESK / REUNITED need a found person AND a known family
    -- (a missing report, or a registered member found by QR)
    CONSTRAINT ck_cf_pair CHECK (status IN ('REPORTED','CLOSED') OR
        (found_report_id IS NOT NULL AND (missing_report_id IS NOT NULL OR member_id IS NOT NULL))),
    CONSTRAINT ck_cf_reunited CHECK (status <> 'REUNITED' OR reunited_at IS NOT NULL),
    CONSTRAINT ck_cf_merged_closed CHECK (merged_into_id IS NULL OR status = 'CLOSED'),
    CONSTRAINT ck_cf_score CHECK (match_score IS NULL OR match_score BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_cf_event_status ON case_files (event_id, status);
CREATE INDEX idx_cf_member       ON case_files (member_id);

CREATE TABLE case_events (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    case_id      BIGINT       NOT NULL,
    from_status  VARCHAR(10)  NULL,
    status       VARCHAR(10)  NOT NULL,     -- new status
    note         VARCHAR(500) NULL,
    changed_by   BIGINT       NULL,         -- NULL = system job
    changed_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_case_events PRIMARY KEY (id),
    CONSTRAINT fk_ce_case FOREIGN KEY (case_id)    REFERENCES case_files (id),
    CONSTRAINT fk_ce_user FOREIGN KEY (changed_by) REFERENCES users (id),
    CONSTRAINT ck_ce_status CHECK (status IN ('REPORTED','MATCHED','AT_DESK','REUNITED','CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_ce_case_time ON case_events (case_id, changed_at);

-- OTP for handover. Only the BCrypt hash is stored. active_case_key allows
-- at most ONE active OTP per case; issuing a new one supersedes the old one.
CREATE TABLE handover_otps (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    case_id          BIGINT       NOT NULL,
    otp_hash         CHAR(60)     NOT NULL,
    expires_at       DATETIME(6)  NOT NULL,
    attempts         TINYINT      NOT NULL DEFAULT 0,
    status           VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',  -- spec field "used" = (status = 'USED')
    issued_by        BIGINT       NOT NULL,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    used_at          DATETIME(6)  NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    active_case_key  BIGINT AS (IF(status = 'ACTIVE', case_id, NULL)) STORED,
    CONSTRAINT pk_handover_otps PRIMARY KEY (id),
    CONSTRAINT uq_otp_one_active UNIQUE (active_case_key),
    CONSTRAINT fk_otp_case   FOREIGN KEY (case_id)   REFERENCES case_files (id),
    CONSTRAINT fk_otp_issuer FOREIGN KEY (issued_by) REFERENCES users (id),
    CONSTRAINT ck_otp_attempts CHECK (attempts BETWEEN 0 AND 5),
    CONSTRAINT ck_otp_status CHECK (status IN ('ACTIVE','USED','EXPIRED','LOCKED','SUPERSEDED')),
    CONSTRAINT ck_otp_used CHECK ((status = 'USED') = (used_at IS NOT NULL)),
    CONSTRAINT ck_otp_locked CHECK (status <> 'LOCKED' OR attempts = 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_otp_case    ON handover_otps (case_id, created_at);
CREATE INDEX idx_otp_expires ON handover_otps (status, expires_at);

-- In-app notifications. NEVER contains an OTP or a phone number.
CREATE TABLE notifications (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    case_id     BIGINT       NULL,
    type        VARCHAR(24)  NOT NULL,
    channel     VARCHAR(8)   NOT NULL,
    message     VARCHAR(300) NOT NULL,
    delivered   BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at     DATETIME     NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_notif_case FOREIGN KEY (case_id) REFERENCES case_files (id),
    CONSTRAINT ck_notif_type CHECK (type IN ('MEMBER_FOUND','MATCH_CONFIRMED','OTP_SENT','STATUS_CHANGED','REUNITED')),
    CONSTRAINT ck_notif_channel CHECK (channel IN ('IN_APP','EMAIL','MOCK'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_notif_user ON notifications (user_id, read_at);

CREATE TABLE revoked_tokens (
    jti         CHAR(36)  NOT NULL,
    expires_at  DATETIME  NOT NULL,
    CONSTRAINT pk_revoked_tokens PRIMARY KEY (jti)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE audit_logs (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    actor_id     BIGINT       NULL,
    action       VARCHAR(60)  NOT NULL,   -- e.g. QR_SCANNED, OTP_ISSUED, OTP_FAILED, OTP_LOCKED
    entity_type  VARCHAR(40)  NOT NULL,
    entity_id    BIGINT       NULL,
    details      VARCHAR(500) NULL,       -- never OTPs, phone numbers or notes
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
