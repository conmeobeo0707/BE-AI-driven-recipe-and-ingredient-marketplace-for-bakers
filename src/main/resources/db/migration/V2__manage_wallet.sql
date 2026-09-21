CREATE TABLE payout
(
    payout_id    UUID           NOT NULL,
    store_id     UUID           NOT NULL,
    amount       DECIMAL(19, 2) NOT NULL,
    status       VARCHAR(255)   NOT NULL,
    processed_by UUID,
    created_at   TIMESTAMP WITHOUT TIME ZONE,
    updated_at   TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_payout PRIMARY KEY (payout_id)
);

CREATE TABLE topup_transaction
(
    topup_id                UUID           NOT NULL,
    store_id                UUID           NOT NULL,
    amount                  DECIMAL(19, 2) NOT NULL,
    payment_method          VARCHAR(255)   NOT NULL,
    external_transaction_id VARCHAR(255),
    status                  VARCHAR(255)   NOT NULL,
    created_at              TIMESTAMP WITHOUT TIME ZONE,
    updated_at              TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_topup_transaction PRIMARY KEY (topup_id)
);

CREATE TABLE wallet
(
    store_wallet_id   UUID           NOT NULL,
    store_id          UUID           NOT NULL,
    total_balance     DECIMAL(19, 2) NOT NULL,
    locked_balance    DECIMAL(19, 2) NOT NULL,
    available_balance DECIMAL(19, 2) NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE,
    updated_at        TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_wallet PRIMARY KEY (store_wallet_id)
);

CREATE TABLE wallet_transaction
(
    wallet_transaction_id UUID           NOT NULL,
    store_wallet_id       UUID           NOT NULL,
    amount                DECIMAL(19, 2) NOT NULL,
    description           VARCHAR(255),
    transaction_type      VARCHAR(255)   NOT NULL,
    reference_id          UUID,
    balance_before        DECIMAL(19, 2) NOT NULL,
    balance_after         DECIMAL(19, 2) NOT NULL,
    created_at            TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_wallet_transaction PRIMARY KEY (wallet_transaction_id)
);

ALTER TABLE wallet
    ADD CONSTRAINT uc_wallet_store UNIQUE (store_id);

ALTER TABLE wallet_transaction
    ADD CONSTRAINT FK_WALLET_TRANSACTION_ON_STORE_WALLET FOREIGN KEY (store_wallet_id) REFERENCES wallet (store_wallet_id);