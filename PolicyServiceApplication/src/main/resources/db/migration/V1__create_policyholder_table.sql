-- V1: Create policyholder table
CREATE TABLE policyholder (
    id            BIGSERIAL       PRIMARY KEY,
    full_name     VARCHAR(200)    NOT NULL,
    email         VARCHAR(255)    NOT NULL UNIQUE,
    phone         VARCHAR(20),
    date_of_birth DATE            NOT NULL,
    address       TEXT,
    created_at    TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_policyholder_email ON policyholder(email);
