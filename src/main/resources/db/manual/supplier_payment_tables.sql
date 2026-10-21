-- Additive schema for recording supplier payments and their stock-in allocations.
-- Run this script once on the database when Flyway is disabled.
CREATE TABLE IF NOT EXISTS supplier_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    supplier_id UUID NOT NULL REFERENCES suppliers(supplier_id) ON DELETE RESTRICT,
    amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS supplier_payment_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_payments_id UUID NOT NULL REFERENCES supplier_payments(id) ON DELETE CASCADE,
    stock_in_note_id UUID NOT NULL REFERENCES stock_in_notes(note_id) ON DELETE RESTRICT,
    amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    CONSTRAINT uq_supplier_payment_note UNIQUE (supplier_payments_id, stock_in_note_id)
);

-- Earlier project SQL used the singular supplier_payment_id column. Normalize it to
-- the column name supplied for this feature when that legacy table is still present.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'supplier_payment_details'
          AND column_name = 'supplier_payment_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'supplier_payment_details'
          AND column_name = 'supplier_payments_id'
    ) THEN
        ALTER TABLE supplier_payment_details RENAME COLUMN supplier_payment_id TO supplier_payments_id;
    END IF;
END $$;

-- The old supplier/payment tables may have survived a manual schema refresh with
-- their foreign keys dropped. Reconnect them to the current warehouse tables.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_supplier_payments_supplier_current')
       AND NOT EXISTS (
           SELECT 1 FROM supplier_payments payment
           LEFT JOIN suppliers supplier ON supplier.supplier_id = payment.supplier_id
           WHERE supplier.supplier_id IS NULL
       ) THEN
        ALTER TABLE supplier_payments
            ADD CONSTRAINT fk_supplier_payments_supplier_current
            FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_supplier_payment_details_payment_current')
       AND NOT EXISTS (
           SELECT 1 FROM supplier_payment_details detail
           LEFT JOIN supplier_payments payment ON payment.id = detail.supplier_payments_id
           WHERE payment.id IS NULL
       ) THEN
        ALTER TABLE supplier_payment_details
            ADD CONSTRAINT fk_supplier_payment_details_payment_current
            FOREIGN KEY (supplier_payments_id) REFERENCES supplier_payments(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_supplier_payment_details_stock_in_current')
       AND NOT EXISTS (
           SELECT 1 FROM supplier_payment_details detail
           LEFT JOIN stock_in_notes stock_in_note ON stock_in_note.note_id = detail.stock_in_note_id
           WHERE stock_in_note.note_id IS NULL
       ) THEN
        ALTER TABLE supplier_payment_details
            ADD CONSTRAINT fk_supplier_payment_details_stock_in_current
            FOREIGN KEY (stock_in_note_id) REFERENCES stock_in_notes(note_id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_supplier_payments_supplier_created
    ON supplier_payments(supplier_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_supplier_payment_details_stock_in_note
    ON supplier_payment_details(stock_in_note_id);
