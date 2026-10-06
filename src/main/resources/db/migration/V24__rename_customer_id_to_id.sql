-- V24__rename_customer_id_to_id.sql
-- PostgreSQL
-- Chuyển đổi cột customer_id thành id trong bảng customers và cập nhật các khóa ngoại liên quan

-- 1. Xóa các khóa ngoại đang tham chiếu đến customer_id cũ
ALTER TABLE customer_hobbies DROP CONSTRAINT IF EXISTS fk_customer_hobbies_customer;
ALTER TABLE request_quote DROP CONSTRAINT IF EXISTS fk_request_quote_customer;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS fk_orders_customer;
ALTER TABLE bookmarks DROP CONSTRAINT IF EXISTS fk_bookmarks_customer;
ALTER TABLE ratings DROP CONSTRAINT IF EXISTS fk_ratings_customer;

-- 2. Đổi tên cột customer_id thành id trong bảng customers
ALTER TABLE customers RENAME COLUMN customer_id TO id;

-- 3. Cập nhật lại tên các khóa ngoại (hoặc giữ nguyên tên và trỏ tới cột id mới)
-- Vì cột đã được đổi tên thành id và vẫn giữ nguyên ràng buộc PRIMARY KEY,
-- ta chỉ cần tạo lại các khóa ngoại trỏ tới cột `id` mới của bảng `customers`.

ALTER TABLE customer_hobbies
    ADD CONSTRAINT fk_customer_hobbies_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE request_quote
    ADD CONSTRAINT fk_request_quote_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE bookmarks
    ADD CONSTRAINT fk_bookmarks_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE ratings
    ADD CONSTRAINT fk_ratings_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id);