-- Additive schema for reserving saleable lot quantities while an order is being processed.
-- Run this script once on the database when Flyway is disabled.
CREATE TABLE IF NOT EXISTS stock_reservations (
    reservation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_stock_reservations_status CHECK (status IN ('ACTIVE', 'CONSUMED', 'RELEASED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_stock_reservations_status_expiry
    ON stock_reservations(status, expires_at);

CREATE TABLE IF NOT EXISTS stock_reservation_items (
    item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID NOT NULL REFERENCES stock_reservations(reservation_id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(product_id) ON DELETE RESTRICT,
    lot_id UUID NOT NULL REFERENCES stock_lot(lot_id) ON DELETE RESTRICT,
    unit_id UUID NOT NULL REFERENCES product_units(unit_id) ON DELETE RESTRICT,
    quantity DECIMAL(10,2) NOT NULL,
    CONSTRAINT ck_stock_reservation_items_quantity CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_stock_reservation_items_lot
    ON stock_reservation_items(lot_id);

CREATE INDEX IF NOT EXISTS idx_stock_reservation_items_reservation
    ON stock_reservation_items(reservation_id);
