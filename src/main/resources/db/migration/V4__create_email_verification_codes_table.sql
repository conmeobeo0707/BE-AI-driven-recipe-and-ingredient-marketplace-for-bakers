CREATE TABLE email_verification_codes (
    verification_id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_email_verification_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);
