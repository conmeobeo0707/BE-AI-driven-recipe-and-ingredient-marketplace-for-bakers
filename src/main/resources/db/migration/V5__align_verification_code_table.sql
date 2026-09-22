ALTER TABLE email_verification_codes
    RENAME TO verification_code;

ALTER TABLE verification_code
    RENAME COLUMN verification_id TO verification_code_id;

ALTER TABLE verification_code
    RENAME COLUMN code_hash TO email_verification_token;

ALTER TABLE verification_code
    RENAME COLUMN expires_at TO email_verification_token_expiry_time;
