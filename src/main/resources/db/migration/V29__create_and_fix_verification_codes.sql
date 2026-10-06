-- V29__create_and_fix_verification_codes.sql

-- 1. Tạo bảng nếu hoàn toàn chưa tồn tại
CREATE TABLE IF NOT EXISTS verification_codes (
                                                  verification_code_id UUID PRIMARY KEY,
                                                  email VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    status VARCHAR(50)
    );

-- 2. Nếu bảng đã tồn tại từ trước, đổi tên cột id thành verification_code_id (nếu có cột id)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'verification_codes' AND column_name = 'id'
    ) THEN
ALTER TABLE verification_codes RENAME COLUMN id TO verification_code_id;
END IF;
END $$;

-- 3. Đảm bảo các cột cần thiết đều tồn tại trong trường hợp bảng cũ bị thiếu
ALTER TABLE verification_codes ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE verification_codes ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE verification_codes ADD COLUMN IF NOT EXISTS expiry_date TIMESTAMP;
ALTER TABLE verification_codes ADD COLUMN IF NOT EXISTS status VARCHAR(50);

-- 4. Tạo index cho email
CREATE INDEX IF NOT EXISTS idx_verification_codes_email ON verification_codes(email);