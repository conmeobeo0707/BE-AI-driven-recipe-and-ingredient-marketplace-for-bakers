-- PostgreSQL script to create a staff account.
-- Replace the sample values before running this script.
-- The password is stored as BCrypt so it is compatible with the application's
-- BCryptPasswordEncoder. Change the temporary password after the first login.

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (
    id,
    full_name,
    email,
    password_hash,
    status,
    created_at,
    updated_at,
    role,
    phone,
    address,
    gender,
    date_of_birth,
    avatar_url
)
VALUES (
    gen_random_uuid(),
    'Nguyen Van An',
    LOWER('an@example.com'),
    crypt('REPLACE_WITH_A_STRONG_TEMPORARY_PASSWORD', gen_salt('bf', 10)),
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'STAFF', -- STAFF, MANAGER, or BAKER_STAFF
    NULL,    -- Optional phone
    NULL,    -- Optional address
    NULL,    -- Optional gender
    NULL,    -- Optional date of birth (YYYY-MM-DD)
    NULL     -- Optional avatar URL
)
RETURNING id, full_name, email, role, status, created_at;

COMMIT;
