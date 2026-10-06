-- V27__alter_product_status_type.sql
ALTER TABLE product ALTER COLUMN status TYPE SMALLINT USING status::integer;