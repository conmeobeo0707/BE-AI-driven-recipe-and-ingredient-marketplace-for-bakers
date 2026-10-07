-- ========== expense_categories ==========

ALTER TABLE expense_categories
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;


ALTER TABLE expense_categories
ALTER COLUMN name TYPE VARCHAR(150),
    ALTER COLUMN name SET NOT NULL,
    ALTER COLUMN type TYPE VARCHAR(30),
    ALTER COLUMN type SET NOT NULL;

ALTER TABLE expense_categories
    ADD CONSTRAINT uk_expense_categories_name UNIQUE (name);


-- ========== expense_vouchers ==========

ALTER TABLE expense_vouchers
    ADD COLUMN IF NOT EXISTS status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS cancelled_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at    TIMESTAMP;

UPDATE expense_vouchers SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;

ALTER TABLE expense_vouchers
ALTER COLUMN code TYPE VARCHAR(50),
    ALTER COLUMN code SET NOT NULL,
    ALTER COLUMN expense_category_id SET NOT NULL,
    ALTER COLUMN amount TYPE NUMERIC(19, 2),
    ALTER COLUMN amount SET NOT NULL,
    ALTER COLUMN reason TYPE VARCHAR(500),
    ALTER COLUMN created_by SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_expense_vouchers_category_id
    ON expense_vouchers(expense_category_id);