CREATE TABLE bakers (
    baker_id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    follower INTEGER NOT NULL DEFAULT 0 CHECK (follower >= 0),
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    bio TEXT,
    total_recipe INTEGER NOT NULL DEFAULT 0 CHECK (total_recipe >= 0),
    national_id_number VARCHAR(32),
    cccd_front_url VARCHAR(2048),
    cccd_back_url VARCHAR(2048),
    certificate_url VARCHAR(2048),
    verification_submitted_at TIMESTAMP,
    verification_reviewed_at TIMESTAMP,
    verification_reject_reason TEXT,
    CONSTRAINT chk_bakers_verification_status
        CHECK (verification_status IN ('PENDING', 'APPROVE', 'REJECT'))
);

INSERT INTO bakers (baker_id, user_id)
SELECT gen_random_uuid(), user_id
FROM users
WHERE role = 'BAKER';
