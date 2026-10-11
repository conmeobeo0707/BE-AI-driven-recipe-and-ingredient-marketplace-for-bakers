ALTER TABLE notification
    ADD COLUMN IF NOT EXISTS title      VARCHAR(255),
    ADD COLUMN IF NOT EXISTS is_read    BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS read_at    TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- phục vụ: lọc theo user + trạng thái, sắp xếp mới nhất, đếm chưa đọc
CREATE INDEX IF NOT EXISTS idx_notification_user_read_created
    ON notification(user_id, is_read, created_at DESC);