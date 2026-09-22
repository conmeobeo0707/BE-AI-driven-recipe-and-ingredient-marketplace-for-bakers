CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    avatar_url VARCHAR(1024),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    address VARCHAR(255),
    bod VARCHAR(10),
    gender VARCHAR(20),
    skill_level VARCHAR(20),

    CONSTRAINT chk_users_role
        CHECK (role IN ('LEARNER', 'STORE', 'BAKER', 'STAFF', 'ADMIN')),
    CONSTRAINT chk_users_skill_level
        CHECK (skill_level IS NULL OR skill_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
);

CREATE UNIQUE INDEX ux_users_email_lower ON users (LOWER(email));
