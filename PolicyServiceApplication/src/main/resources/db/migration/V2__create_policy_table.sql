-- V2: Create policy table
CREATE TABLE policy (
    id              BIGSERIAL       PRIMARY KEY,
    policy_number   VARCHAR(30)     NOT NULL UNIQUE,
    product_type    VARCHAR(30)     NOT NULL,
    sum_insured     DECIMAL(15,2)   NOT NULL,
    annual_premium  DECIMAL(10,2)   NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    start_date      DATE            NOT NULL,
    end_date        DATE            NOT NULL,
    policyholder_id BIGINT          NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    cancelled_at    TIMESTAMP,
    CONSTRAINT fk_policy_policyholder FOREIGN KEY (policyholder_id) REFERENCES policyholder(id) ON DELETE CASCADE,
    CONSTRAINT chk_policy_dates CHECK (end_date > start_date),
    CONSTRAINT chk_policy_product_type CHECK (product_type IN ('MOTOR','HOME','TRAVEL','HEALTH','LIABILITY')),
    CONSTRAINT chk_policy_status CHECK (status IN ('ACTIVE','EXPIRED','CANCELLED','LAPSED')),
    CONSTRAINT chk_policy_sum_insured CHECK (sum_insured > 0),
    CONSTRAINT chk_policy_premium CHECK (annual_premium >= 0)
);

CREATE INDEX idx_policy_policyholder_id ON policy(policyholder_id);
CREATE INDEX idx_policy_number ON policy(policy_number);
CREATE INDEX idx_policy_status ON policy(status);
CREATE INDEX idx_policy_status_end_date ON policy(status, end_date);
