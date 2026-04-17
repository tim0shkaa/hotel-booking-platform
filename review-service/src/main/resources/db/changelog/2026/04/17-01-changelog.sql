-- liquibase formatted sql

-- changeset tim0shkaa:review-13
ALTER TABLE eligible_booking
    ADD COLUMN user_id BIGINT NOT NULL;