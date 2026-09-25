-- Product is empty when this migration runs. If BIGINT IDs have been inserted,
-- the casts fail instead of silently replacing IDs used elsewhere.
ALTER TABLE product
    ALTER COLUMN product_id DROP IDENTITY;

ALTER TABLE product
    ALTER COLUMN product_id TYPE UUID USING product_id::text::uuid,
    ALTER COLUMN store_id TYPE UUID USING store_id::text::uuid;
