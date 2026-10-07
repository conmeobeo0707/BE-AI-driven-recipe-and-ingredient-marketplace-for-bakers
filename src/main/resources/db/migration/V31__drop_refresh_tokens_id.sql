

-- 2. Gỡ FK tự tham chiếu (replaced_by_id -> id) vì nó phụ thuộc vào PK cũ
ALTER TABLE refresh_tokens
DROP CONSTRAINT IF EXISTS fk_refresh_tokens_replaced_by;

-- 3. Bỏ PK cũ (nằm trên cột id)
ALTER TABLE refresh_tokens
DROP CONSTRAINT IF EXISTS refresh_tokens_pkey;

-- 4. Xóa cột id
ALTER TABLE refresh_tokens
DROP COLUMN id;

-- 5. Đặt refresh_token_id làm PK mới
ALTER TABLE refresh_tokens
    ALTER COLUMN refresh_token_id SET NOT NULL,
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (refresh_token_id);

-- 6. Tạo lại FK tự tham chiếu, trỏ vào PK mới
ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_replaced_by
        FOREIGN KEY (replaced_by_id) REFERENCES refresh_tokens(refresh_token_id);