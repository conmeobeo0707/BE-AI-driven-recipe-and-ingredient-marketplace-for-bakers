-- 1. Cập nhật thời gian hiện tại cho các sản phẩm cũ (nếu có) đang bị NULL để tránh lỗi khi set NOT NULL
UPDATE product SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;

-- 2. Thay đổi cấu trúc cột created_at: Thêm DEFAULT và NOT NULL
ALTER TABLE product
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
ALTER COLUMN created_at SET NOT NULL;