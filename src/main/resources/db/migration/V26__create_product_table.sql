-- V26__create_product_table.sql
-- PostgreSQL
-- Tạo bảng product khớp với Product Entity

CREATE TABLE IF NOT EXISTS product (
                                       product_id UUID PRIMARY KEY,
                                       name VARCHAR(255) NOT NULL,
    description VARCHAR(512),
    img_url VARCHAR(255),
    price VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATE,
    store_id UUID NOT NULL
    );

-- Tạo Index cho store_id để tối ưu hiệu suất truy vấn
CREATE INDEX IF NOT EXISTS idx_product_store_id ON product(store_id);