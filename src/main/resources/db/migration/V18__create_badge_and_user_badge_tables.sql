-- V18__create_badge_and_user_badge_tables.sql
-- Tạo bảng badge và user_badge tương ứng với các entity trong project

CREATE TABLE badge (
                       badge_id UUID NOT NULL PRIMARY KEY,
                       name VARCHAR(255),
                       description VARCHAR(1000),
                       created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_badge (
                            user_badge_id UUID NOT NULL PRIMARY KEY,
                            user_id UUID NOT NULL,
                            badge_id UUID NOT NULL,
                            earned_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT uk_user_badge UNIQUE (user_id, badge_id),
                            CONSTRAINT fk_user_badge_user FOREIGN KEY (user_id)
                                REFERENCES users (user_id) ON DELETE CASCADE,
                            CONSTRAINT fk_user_badge_badge FOREIGN KEY (badge_id)
                                REFERENCES badge (badge_id) ON DELETE CASCADE
);

-- Optional: indexes to speed up lookups by user_id / badge_id
CREATE INDEX idx_user_badge_user_id ON user_badge (user_id);
CREATE INDEX idx_user_badge_badge_id ON user_badge (badge_id);