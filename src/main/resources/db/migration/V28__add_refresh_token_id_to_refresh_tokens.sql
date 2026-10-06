-- V28__add_refresh_token_id_to_refresh_tokens.sql
-- Thêm cột refresh_token_id vào bảng refresh_tokens nếu chưa có

ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS refresh_token_id UUID;

-- Nếu cột này là khóa chính (Primary Key), bạn có thể gán nó làm PK (tùy thuộc vào thiết kế Entity của bạn):
-- ALTER TABLE refresh_tokens ADD PRIMARY KEY (refresh_token_id);