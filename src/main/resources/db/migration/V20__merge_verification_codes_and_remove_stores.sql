ALTER TABLE verification_code RENAME COLUMN email_verification_token TO code_hash;
ALTER TABLE verification_code RENAME COLUMN email_verification_token_expiry_time TO expires_at;

-- Existing rows are email verification codes. All future writes must specify purpose.
ALTER TABLE verification_code ADD COLUMN purpose VARCHAR(30) NOT NULL DEFAULT 'EMAIL_VERIFICATION';
ALTER TABLE verification_code ALTER COLUMN purpose DROP DEFAULT;
ALTER TABLE verification_code ADD CONSTRAINT chk_verification_code_purpose
    CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET'));

-- PostgreSQL retains the original auto-generated constraint name after a table rename.
ALTER TABLE verification_code DROP CONSTRAINT IF EXISTS email_verification_codes_user_id_key;
ALTER TABLE verification_code DROP CONSTRAINT IF EXISTS verification_code_user_id_key;
ALTER TABLE verification_code ADD CONSTRAINT uk_verification_code_user_purpose UNIQUE (user_id, purpose);

-- Preserve password-reset hashes and timestamps before dropping the old table.
-- Generate IDs in the shared table to avoid collisions between the two former ID sets.
INSERT INTO verification_code (verification_code_id, user_id, purpose, code_hash, expires_at, created_at)
SELECT gen_random_uuid(), user_id, 'PASSWORD_RESET', code_hash, expires_at, created_at
FROM password_reset_codes;

DROP TABLE password_reset_codes;

-- No CASCADE: unexpected external dependencies must be handled explicitly.
DROP TABLE stores;
