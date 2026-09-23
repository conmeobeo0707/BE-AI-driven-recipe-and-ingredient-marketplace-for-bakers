ALTER TABLE payout
    ADD COLUMN reject_reason VARCHAR(500),
    ADD COLUMN approved_at TIMESTAMP,
    ADD COLUMN rejected_at TIMESTAMP,
    ADD COLUMN completed_at TIMESTAMP,
    ADD COLUMN failed_at TIMESTAMP;