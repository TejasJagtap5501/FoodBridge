-- =====================================================================
-- FoodBridge V1 - baseline schema
-- MySQL 8 compatible. All timestamps stored in UTC.
-- =====================================================================

CREATE TABLE users (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    email               VARCHAR(160)    NOT NULL,
    password_hash       VARCHAR(100)    NOT NULL,
    full_name           VARCHAR(120)    NOT NULL,
    phone               VARCHAR(24)     NULL,
    role                VARCHAR(16)     NOT NULL,
    status              VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    email_verified      BOOLEAN         NOT NULL DEFAULT FALSE,
    failed_login_count  INT             NOT NULL DEFAULT 0,
    locked_until        DATETIME(6)     NULL,
    last_login_at       DATETIME(6)     NULL,
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('DONOR','NGO','ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE','INACTIVE','PENDING'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_role_status ON users (role, status);
CREATE INDEX idx_users_created_at ON users (created_at);

-- ---------------------------------------------------------------------
-- Donor (restaurant / hotel / cafe / food business) profile
-- ---------------------------------------------------------------------
CREATE TABLE donor_profiles (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    business_name       VARCHAR(160)    NOT NULL,
    business_type       VARCHAR(32)     NOT NULL,
    contact_person      VARCHAR(120)    NOT NULL,
    address_line1       VARCHAR(200)    NOT NULL,
    address_line2       VARCHAR(200)    NULL,
    city                VARCHAR(80)     NOT NULL,
    state               VARCHAR(80)     NOT NULL,
    postal_code         VARCHAR(16)     NOT NULL,
    latitude            DECIMAL(9,6)    NULL,
    longitude           DECIMAL(9,6)    NULL,
    fssai_license       VARCHAR(32)     NULL,
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,
    CONSTRAINT uk_donor_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_donor_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_donor_profiles_city ON donor_profiles (city);

-- ---------------------------------------------------------------------
-- NGO profile + verification workflow
-- ---------------------------------------------------------------------
CREATE TABLE ngo_profiles (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    organization_name   VARCHAR(160)    NOT NULL,
    registration_number VARCHAR(64)     NOT NULL,
    contact_person      VARCHAR(120)    NOT NULL,
    website             VARCHAR(200)    NULL,
    address_line1       VARCHAR(200)    NOT NULL,
    address_line2       VARCHAR(200)    NULL,
    city                VARCHAR(80)     NOT NULL,
    state               VARCHAR(80)     NOT NULL,
    postal_code         VARCHAR(16)     NOT NULL,
    latitude            DECIMAL(9,6)    NULL,
    longitude           DECIMAL(9,6)    NULL,
    verification_status VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    verification_note   VARCHAR(500)    NULL,
    verified_by         BIGINT          NULL,
    verified_at         DATETIME(6)     NULL,
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,
    CONSTRAINT uk_ngo_profiles_user UNIQUE (user_id),
    CONSTRAINT uk_ngo_profiles_reg UNIQUE (registration_number),
    CONSTRAINT fk_ngo_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ngo_profiles_verifier FOREIGN KEY (verified_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_ngo_verification CHECK (verification_status IN ('PENDING','APPROVED','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ngo_profiles_status_city ON ngo_profiles (verification_status, city);

-- ---------------------------------------------------------------------
-- Food categories (admin managed)
-- ---------------------------------------------------------------------
CREATE TABLE food_categories (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(80)     NOT NULL,
    slug                VARCHAR(80)     NOT NULL,
    description         VARCHAR(255)    NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    display_order       INT             NOT NULL DEFAULT 0,
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,
    CONSTRAINT uk_food_categories_name UNIQUE (name),
    CONSTRAINT uk_food_categories_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Donations
-- ---------------------------------------------------------------------
CREATE TABLE donations (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_code          VARCHAR(24)     NOT NULL,
    donor_id                BIGINT          NOT NULL,
    category_id             BIGINT          NOT NULL,
    food_name               VARCHAR(160)    NOT NULL,
    description             VARCHAR(1000)   NULL,
    quantity                DECIMAL(10,2)   NOT NULL,
    quantity_unit           VARCHAR(16)     NOT NULL,
    food_type               VARCHAR(16)     NOT NULL,
    prepared_at             DATETIME(6)     NOT NULL,
    expires_at              DATETIME(6)     NOT NULL,
    pickup_address          VARCHAR(300)    NOT NULL,
    city                    VARCHAR(80)     NOT NULL,
    latitude                DECIMAL(9,6)    NULL,
    longitude               DECIMAL(9,6)    NULL,
    pickup_instructions     VARCHAR(500)    NULL,
    storage_conditions      VARCHAR(255)    NULL,
    allergen_info           VARCHAR(500)    NULL,
    handling_instructions   VARCHAR(500)    NULL,
    safety_declaration      BOOLEAN         NOT NULL DEFAULT FALSE,
    estimated_meals         INT             NOT NULL DEFAULT 0,
    status                  VARCHAR(16)     NOT NULL DEFAULT 'AVAILABLE',
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at              DATETIME(6)     NOT NULL,
    updated_at              DATETIME(6)     NOT NULL,
    CONSTRAINT uk_donations_reference UNIQUE (reference_code),
    CONSTRAINT fk_donations_donor FOREIGN KEY (donor_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_donations_category FOREIGN KEY (category_id) REFERENCES food_categories (id),
    CONSTRAINT chk_donations_status CHECK (status IN ('AVAILABLE','CLAIMED','COMPLETED','EXPIRED','CANCELLED')),
    CONSTRAINT chk_donations_food_type CHECK (food_type IN ('VEGETARIAN','NON_VEGETARIAN','VEGAN','MIXED')),
    CONSTRAINT chk_donations_qty CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_donations_status_expires ON donations (status, expires_at);
CREATE INDEX idx_donations_donor_created ON donations (donor_id, created_at);
CREATE INDEX idx_donations_city_status ON donations (city, status);
CREATE INDEX idx_donations_category_id ON donations (category_id);
CREATE INDEX idx_donations_food_name ON donations (food_name);

CREATE TABLE donation_images (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    donation_id         BIGINT          NOT NULL,
    storage_key         VARCHAR(255)    NOT NULL,
    original_filename   VARCHAR(255)    NOT NULL,
    content_type        VARCHAR(64)     NOT NULL,
    size_bytes          BIGINT          NOT NULL,
    created_at          DATETIME(6)     NOT NULL,
    CONSTRAINT fk_donation_images_donation FOREIGN KEY (donation_id) REFERENCES donations (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_donation_images_donation ON donation_images (donation_id);

-- ---------------------------------------------------------------------
-- Claims: exactly ONE active claim per donation, enforced by the database
-- ---------------------------------------------------------------------
CREATE TABLE claims (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    donation_id             BIGINT          NOT NULL,
    ngo_id                  BIGINT          NOT NULL,
    status                  VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    message                 VARCHAR(500)    NULL,
    suitability_check       BOOLEAN         NOT NULL DEFAULT FALSE,
    claimed_at              DATETIME(6)     NOT NULL,
    resolved_at             DATETIME(6)     NULL,
    cancelled_reason        VARCHAR(300)    NULL,
    created_at              DATETIME(6)     NOT NULL,
    updated_at              DATETIME(6)     NOT NULL,
    active_donation_id      BIGINT          GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN donation_id ELSE NULL END) STORED,
    CONSTRAINT fk_claims_donation FOREIGN KEY (donation_id) REFERENCES donations (id),
    CONSTRAINT fk_claims_ngo FOREIGN KEY (ngo_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_claims_active_donation UNIQUE (active_donation_id),
    CONSTRAINT chk_claims_status CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_claims_ngo_status ON claims (ngo_id, status);
CREATE INDEX idx_claims_donation ON claims (donation_id);

-- ---------------------------------------------------------------------
-- Pickup coordination
-- ---------------------------------------------------------------------
CREATE TABLE pickups (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    claim_id                BIGINT          NOT NULL,
    scheduled_at            DATETIME(6)     NOT NULL,
    status                  VARCHAR(24)     NOT NULL DEFAULT 'SCHEDULED',
    instructions            VARCHAR(500)    NULL,
    donor_confirmed_at      DATETIME(6)     NULL,
    ngo_confirmed_at        DATETIME(6)     NULL,
    completed_at            DATETIME(6)     NULL,
    cancelled_reason        VARCHAR(300)    NULL,
    created_by              BIGINT          NOT NULL,
    created_at              DATETIME(6)     NOT NULL,
    updated_at              DATETIME(6)     NOT NULL,
    CONSTRAINT fk_pickups_claim FOREIGN KEY (claim_id) REFERENCES claims (id) ON DELETE CASCADE,
    CONSTRAINT fk_pickups_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_pickups_status CHECK (status IN ('SCHEDULED','HANDED_OVER','COMPLETED','CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pickups_claim ON pickups (claim_id);
CREATE INDEX idx_pickups_status_sched ON pickups (status, scheduled_at);

-- ---------------------------------------------------------------------
-- NGO food requirement requests
-- ---------------------------------------------------------------------
CREATE TABLE ngo_requests (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    ngo_id              BIGINT          NOT NULL,
    title               VARCHAR(160)    NOT NULL,
    description         VARCHAR(1000)   NULL,
    category_id         BIGINT          NULL,
    quantity_needed     DECIMAL(10,2)   NOT NULL,
    quantity_unit       VARCHAR(16)     NOT NULL,
    city                VARCHAR(80)     NOT NULL,
    needed_by           DATETIME(6)     NOT NULL,
    beneficiaries       INT             NOT NULL DEFAULT 0,
    status              VARCHAR(16)     NOT NULL DEFAULT 'OPEN',
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,
    CONSTRAINT fk_ngo_requests_ngo FOREIGN KEY (ngo_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ngo_requests_category FOREIGN KEY (category_id) REFERENCES food_categories (id) ON DELETE SET NULL,
    CONSTRAINT chk_ngo_requests_status CHECK (status IN ('OPEN','FULFILLED','CANCELLED','EXPIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ngo_requests_status_city ON ngo_requests (status, city);
CREATE INDEX idx_ngo_requests_ngo ON ngo_requests (ngo_id, created_at);

-- ---------------------------------------------------------------------
-- Notifications (persistent history + SSE live delivery)
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id        BIGINT          NOT NULL,
    type                VARCHAR(40)     NOT NULL,
    title               VARCHAR(160)    NOT NULL,
    message             VARCHAR(500)    NOT NULL,
    link                VARCHAR(255)    NULL,
    read_flag           BOOLEAN         NOT NULL DEFAULT FALSE,
    read_at             DATETIME(6)     NULL,
    created_at          DATETIME(6)     NOT NULL,
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notifications_recipient_read ON notifications (recipient_id, read_flag, created_at);

-- ---------------------------------------------------------------------
-- Impact log: immutable record of completed redistributions
-- ---------------------------------------------------------------------
CREATE TABLE impact_logs (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    donation_id         BIGINT          NOT NULL,
    donor_id            BIGINT          NOT NULL,
    ngo_id              BIGINT          NOT NULL,
    quantity            DECIMAL(10,2)   NOT NULL,
    quantity_unit       VARCHAR(16)     NOT NULL,
    meals_served        INT             NOT NULL,
    city                VARCHAR(80)     NOT NULL,
    category_id         BIGINT          NULL,
    completed_at        DATETIME(6)     NOT NULL,
    created_at          DATETIME(6)     NOT NULL,
    CONSTRAINT uk_impact_logs_donation UNIQUE (donation_id),
    CONSTRAINT fk_impact_logs_donation FOREIGN KEY (donation_id) REFERENCES donations (id) ON DELETE CASCADE,
    CONSTRAINT fk_impact_logs_donor FOREIGN KEY (donor_id) REFERENCES users (id),
    CONSTRAINT fk_impact_logs_ngo FOREIGN KEY (ngo_id) REFERENCES users (id),
    CONSTRAINT fk_impact_logs_category FOREIGN KEY (category_id) REFERENCES food_categories (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_impact_logs_completed ON impact_logs (completed_at);
CREATE INDEX idx_impact_logs_city ON impact_logs (city, completed_at);

-- ---------------------------------------------------------------------
-- Audit log for sensitive administrative actions
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id            BIGINT          NULL,
    actor_email         VARCHAR(160)    NULL,
    action              VARCHAR(64)     NOT NULL,
    entity_type         VARCHAR(48)     NOT NULL,
    entity_id           BIGINT          NULL,
    details             VARCHAR(1000)   NULL,
    ip_address          VARCHAR(64)     NULL,
    created_at          DATETIME(6)     NOT NULL,
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_logs_created ON audit_logs (created_at);
CREATE INDEX idx_audit_logs_action ON audit_logs (action, created_at);

-- ---------------------------------------------------------------------
-- Refresh tokens: only SHA-256 hashes are persisted
-- ---------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    token_hash          VARCHAR(64)     NOT NULL,
    expires_at          DATETIME(6)     NOT NULL,
    revoked             BOOLEAN         NOT NULL DEFAULT FALSE,
    revoked_at          DATETIME(6)     NULL,
    user_agent          VARCHAR(255)    NULL,
    ip_address          VARCHAR(64)     NULL,
    created_at          DATETIME(6)     NOT NULL,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id, revoked);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);

-- ---------------------------------------------------------------------
-- One-time passwords / tokens for password reset + email verification
-- ---------------------------------------------------------------------
CREATE TABLE one_time_tokens (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    token_hash          VARCHAR(64)     NOT NULL,
    purpose             VARCHAR(32)     NOT NULL,
    expires_at          DATETIME(6)     NOT NULL,
    used_at             DATETIME(6)     NULL,
    created_at          DATETIME(6)     NOT NULL,
    CONSTRAINT uk_one_time_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_one_time_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_one_time_tokens_purpose CHECK (purpose IN ('PASSWORD_RESET','EMAIL_VERIFICATION'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_one_time_tokens_user_purpose ON one_time_tokens (user_id, purpose, used_at);
