-- This migration intentionally deletes the legacy profile/hobby data.
-- No CASCADE: unexpected dependencies must be resolved explicitly.
DROP TABLE IF EXISTS learner_hobby, user_hobbies, hobbylearner;
DROP TABLE IF EXISTS hobby, hobbies;
DROP TABLE IF EXISTS bakers, learners;

-- UUIDs and updated_at are retained for this register-only branch.
-- PostgreSQL updates existing foreign-key references when the PK column is renamed.
ALTER TABLE users RENAME COLUMN user_id TO id;
ALTER TABLE users RENAME COLUMN name TO full_name;
ALTER TABLE users RENAME COLUMN password TO password_hash;
ALTER TABLE users RENAME COLUMN bod TO date_of_birth;
ALTER TABLE users DROP COLUMN IF EXISTS avatar_url;
ALTER TABLE users DROP COLUMN IF EXISTS skill_level;

ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;

-- Former public-registration roles do not automatically receive staff permissions.
UPDATE users SET role = 'CUSTOMER' WHERE role IN ('LEARNER', 'BAKER', 'STORE');

ALTER TABLE users ADD CONSTRAINT chk_users_role
    CHECK (role IN ('ADMIN', 'MANAGER', 'STAFF', 'BAKER_STAFF', 'CUSTOMER'));

ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
UPDATE users SET status = CASE WHEN is_active THEN 'ACTIVE' ELSE 'BLOCKED' END;
ALTER TABLE users DROP COLUMN is_active;
ALTER TABLE users ADD CONSTRAINT chk_users_status
    CHECK (status IN ('ACTIVE', 'BLOCKED', 'INACTIVE'));

CREATE TABLE customers (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    hobby_id UUID,
    skill_level VARCHAR(20),
    is_confirmed BOOLEAN NOT NULL DEFAULT FALSE
);

-- Every existing CUSTOMER account also needs its new profile.
INSERT INTO customers (id, user_id, skill_level, is_confirmed)
SELECT gen_random_uuid(), id, NULL, is_confirmed
FROM users
WHERE role = 'CUSTOMER';

-- Email confirmation is now stored only on Customer. Preserve existing confirmations
-- before removing the old column; internal staff accounts do not require verification.
ALTER TABLE users DROP COLUMN is_confirmed;

CREATE TABLE hobbies (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE customers ADD CONSTRAINT fk_customers_hobby
    FOREIGN KEY (hobby_id) REFERENCES hobbies(id) ON DELETE SET NULL;

CREATE INDEX idx_customers_hobby_id ON customers(hobby_id);

CREATE TABLE customer_hobbies (
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    hobby_id UUID NOT NULL REFERENCES hobbies(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    PRIMARY KEY (customer_id, hobby_id)
);

CREATE INDEX idx_customer_hobbies_hobby_id ON customer_hobbies(hobby_id);
