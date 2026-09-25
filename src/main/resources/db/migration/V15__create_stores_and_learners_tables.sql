CREATE TABLE stores (
    store_id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    revenue NUMERIC(19, 2) NOT NULL DEFAULT 0,
    rating NUMERIC(3, 2) NOT NULL DEFAULT 0,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    logo_url VARCHAR(2048),
    status VARCHAR(20) NOT NULL DEFAULT 'CLOSED',
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_stores_status CHECK (status IN ('OPEN', 'BREAK', 'CLOSED'))
);

CREATE TABLE learners (
    learner_id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    skill_level VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_learners_skill_level CHECK (skill_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
);

INSERT INTO stores (store_id, user_id, created_at)
SELECT gen_random_uuid(), user_id, created_at FROM users WHERE role = 'STORE';

INSERT INTO learners (learner_id, user_id, skill_level, created_at)
SELECT gen_random_uuid(), user_id, skill_level, created_at FROM users WHERE role = 'LEARNER';
