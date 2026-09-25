-- V9 created product_id as a BIGINT identity. This conversion expects an empty product table.
-- If rows were inserted meanwhile, the cast fails instead of silently replacing their IDs.
ALTER TABLE product
    ALTER COLUMN product_id DROP IDENTITY;

ALTER TABLE product
    ALTER COLUMN product_id TYPE UUID USING product_id::text::uuid;
