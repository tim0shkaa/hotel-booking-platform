-- liquibase formatted sql

-- changeset tim0shkaa:payment-12
ALTER TABLE payment
    ADD COLUMN user_id BIGINT NOT NULL DEFAULT 0;

-- changeset tim0shkaa:payment-13
CREATE INDEX idx_payment_user_id ON payment (user_id);