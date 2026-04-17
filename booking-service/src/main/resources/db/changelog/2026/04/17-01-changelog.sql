-- liquibase formatted sql

-- changeset tim0shkaa:booking-19
ALTER TABLE hotels
    ADD COLUMN total_reviews INTEGER NOT NULL DEFAULT 0;