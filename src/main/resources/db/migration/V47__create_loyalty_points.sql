
CREATE TABLE ai_usages (
    id              UUID PRIMARY KEY,
    customer_id     UUID        NOT NULL,
    feature         VARCHAR(30) NOT NULL,
    points_charged  INT         NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'CHARGED',
    refund_order_id UUID,
    used_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refunded_at     TIMESTAMPTZ,
    CONSTRAINT fk_ai_usages_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_usages_refund_order
        FOREIGN KEY (refund_order_id) REFERENCES orders (id),
    CONSTRAINT ck_ai_usages_feature CHECK (feature IN ('INGREDIENT_IMAGE', 'RECIPE_VIDEO')),
    CONSTRAINT ck_ai_usages_status CHECK (status IN ('CHARGED', 'REFUNDED_ERROR', 'REFUNDED_PURCHASE')),
    CONSTRAINT ck_ai_usages_points_charged CHECK (points_charged >= 0),
    CONSTRAINT ck_ai_usages_refund CHECK (
        (status = 'CHARGED' AND refunded_at IS NULL AND refund_order_id IS NULL)
        OR (status = 'REFUNDED_ERROR' AND refunded_at IS NOT NULL AND refund_order_id IS NULL)
        OR (status = 'REFUNDED_PURCHASE' AND refunded_at IS NOT NULL AND refund_order_id IS NOT NULL)
    )
);

CREATE INDEX idx_ai_usages_customer_used_at
    ON ai_usages (customer_id, used_at DESC);

CREATE UNIQUE INDEX ux_ai_usages_refund_order
    ON ai_usages (refund_order_id)
    WHERE refund_order_id IS NOT NULL;

CREATE TABLE loyalty_wallets (
    customer_id UUID PRIMARY KEY,
    balance     INT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loyalty_wallets_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT ck_loyalty_wallets_balance CHECK (balance >= 0)
);

CREATE TABLE loyalty_transactions (
    id            UUID PRIMARY KEY,
    customer_id   UUID        NOT NULL,
    type          VARCHAR(30) NOT NULL,
    points        INT         NOT NULL,
    balance_after INT         NOT NULL,
    order_id      UUID,
    ai_usage_id   UUID,
    note          VARCHAR(255),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loyalty_transactions_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT fk_loyalty_transactions_order
        FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_loyalty_transactions_ai_usage
        FOREIGN KEY (ai_usage_id) REFERENCES ai_usages (id) ON DELETE CASCADE,
    CONSTRAINT ck_loyalty_transactions_type
        CHECK (type IN ('WELCOME', 'ORDER_EARN', 'AI_SPEND', 'AI_REFUND_ERROR', 'AI_REFUND_PURCHASE')),
    CONSTRAINT ck_loyalty_transactions_points
        CHECK ((type = 'AI_SPEND' AND points < 0) OR (type <> 'AI_SPEND' AND points > 0)),
    CONSTRAINT ck_loyalty_transactions_balance_after CHECK (balance_after >= 0)
);

CREATE INDEX idx_loyalty_transactions_customer_created_at
    ON loyalty_transactions (customer_id, created_at DESC);

CREATE UNIQUE INDEX ux_loyalty_transactions_order_type
    ON loyalty_transactions (order_id, type)
    WHERE order_id IS NOT NULL;

CREATE UNIQUE INDEX ux_loyalty_transactions_welcome
    ON loyalty_transactions (customer_id)
    WHERE type = 'WELCOME';

CREATE UNIQUE INDEX ux_loyalty_transactions_ai_usage_type
    ON loyalty_transactions (ai_usage_id, type)
    WHERE ai_usage_id IS NOT NULL;
