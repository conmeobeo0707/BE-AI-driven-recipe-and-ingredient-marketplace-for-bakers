-- V25__create_wallet_and_payment_tables.sql
-- PostgreSQL
-- Tạo các bảng cho module Wallet và Payment (payout, topup_transaction, wallet, wallet_transaction)

-- 1. Bảng wallet (Ví của cửa hàng)
CREATE TABLE IF NOT EXISTS wallet (
                                      store_wallet_id UUID PRIMARY KEY,
                                      store_id UUID NOT NULL UNIQUE,
                                      total_balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
    locked_balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
    available_balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
    );

-- 2. Bảng wallet_transaction (Lịch sử giao dịch ví)
CREATE TABLE IF NOT EXISTS wallet_transaction (
                                                  wallet_transaction_id UUID PRIMARY KEY,
                                                  store_wallet_id UUID NOT NULL,
                                                  amount NUMERIC(19, 2) NOT NULL,
    description VARCHAR(255),
    transaction_type VARCHAR(50) NOT NULL,
    reference_id UUID,
    balance_before NUMERIC(19, 2) NOT NULL,
    balance_after NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_transaction_wallet
    FOREIGN KEY (store_wallet_id) REFERENCES wallet(store_wallet_id)
    );

-- 3. Bảng payout (Lịch sử rút tiền/thanh toán)
CREATE TABLE IF NOT EXISTS payout (
                                      payout_id UUID PRIMARY KEY,
                                      store_id UUID NOT NULL,
                                      amount NUMERIC(19, 2) NOT NULL,
    bank_transaction_id VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    processed_by UUID,
    reject_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,
    rejected_at TIMESTAMP,
    completed_at TIMESTAMP,
    failed_at TIMESTAMP,
    updated_at TIMESTAMP
    );

-- 4. Bảng topup_transaction (Lịch sử nạp tiền vào hệ thống)
CREATE TABLE IF NOT EXISTS topup_transaction (
                                                 topup_id UUID PRIMARY KEY,
                                                 store_id UUID NOT NULL,
                                                 amount NUMERIC(19, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    external_transaction_id VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
    );

-- Tạo Index để tối ưu hiệu suất truy vấn theo store_id
CREATE INDEX IF NOT EXISTS idx_wallet_store_id ON wallet(store_id);
CREATE INDEX IF NOT EXISTS idx_payout_store_id ON payout(store_id);
CREATE INDEX IF NOT EXISTS idx_topup_store_id ON topup_transaction(store_id);