-- ==============================================================================
-- 1. XÓA CÁC BẢNG & ENUM NẾU ĐÃ TỒN TẠI (Dùng CASCADE để tránh lỗi khóa ngoại)
-- ==============================================================================
DROP TABLE IF EXISTS children_products CASCADE;
DROP TABLE IF EXISTS unit_price_history CASCADE;
DROP TABLE IF EXISTS product_units CASCADE;
DROP TABLE IF EXISTS stock_out_item CASCADE;
DROP TABLE IF EXISTS stock_out_notes CASCADE;
DROP TABLE IF EXISTS stocktake_items CASCADE;
DROP TABLE IF EXISTS stocktake_notes CASCADE;
DROP TABLE IF EXISTS stock_lot CASCADE;
DROP TABLE IF EXISTS stock_in_item CASCADE;
DROP TABLE IF EXISTS stock_in_notes CASCADE;
DROP TABLE IF EXISTS suppliers CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS product_categories CASCADE;

DROP TYPE IF EXISTS product_status_enum CASCADE;
DROP TYPE IF EXISTS unit_status_enum CASCADE;
DROP TYPE IF EXISTS payment_status_enum CASCADE;
DROP TYPE IF EXISTS note_status_enum CASCADE;
DROP TYPE IF EXISTS stock_out_type_enum CASCADE;
DROP TYPE IF EXISTS lot_status_enum CASCADE;
DROP TYPE IF EXISTS stocktake_status_enum CASCADE;

-- ==============================================================================
-- 2. TẠO CÁC KIỂU ENUM
-- ==============================================================================
CREATE TYPE product_status_enum AS ENUM ('ACTIVE', 'INACTIVE', 'DISCONTINUE');
CREATE TYPE unit_status_enum AS ENUM ('ACTIVE', 'INACTIVE');
CREATE TYPE payment_status_enum AS ENUM ('UNPAID', 'PARTIAL', 'PAID');
CREATE TYPE note_status_enum AS ENUM ('PENDING', 'COMPLETED', 'CANCELLED');
CREATE TYPE stock_out_type_enum AS ENUM ('SALE', 'DISPOSAL');
CREATE TYPE lot_status_enum AS ENUM ('STORAGE', 'EXPIRED', 'CANCELED', 'EMPTY');
CREATE TYPE stocktake_status_enum AS ENUM ('DRAFT', 'COMPLETED', 'CANCELLED');

-- ==============================================================================
-- 3. TẠO BẢNG (TABLES)
-- ==============================================================================

-- Bảng Danh mục sản phẩm
CREATE TABLE product_categories (
    product_category_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Nhà cung cấp
CREATE TABLE suppliers (
    supplier_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Sản phẩm chính
CREATE TABLE products (
    product_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category_id UUID REFERENCES product_categories(product_category_id) ON DELETE RESTRICT,
    img_url TEXT,
    discount_percent DECIMAL(5,2) DEFAULT 0,
    storage_guide TEXT,
    min_stock DECIMAL(10,2) DEFAULT 0,
    max_stock DECIMAL(10,2),
    stock_quantity DECIMAL(10,2) DEFAULT 0,
    status product_status_enum DEFAULT 'ACTIVE',
    brand VARCHAR(255),
    is_combo BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Sản phẩm con (Dành cho Combo)
CREATE TABLE children_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_product_id UUID REFERENCES products(product_id) ON DELETE CASCADE,
    children_product_id UUID REFERENCES products(product_id) ON DELETE RESTRICT,
    quantity DECIMAL(10,2) NOT NULL
);

-- Bảng Đơn vị tính của sản phẩm
CREATE TABLE product_units (
    unit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID REFERENCES products(product_id) ON DELETE CASCADE,
    unit_name VARCHAR(100) NOT NULL,
    conversion_factor DECIMAL(10,2) NOT NULL,
    is_base BOOLEAN DEFAULT FALSE,
    is_sale BOOLEAN DEFAULT TRUE,
    selling_price DECIMAL(15,2) NOT NULL,
    status unit_status_enum DEFAULT 'ACTIVE'
);

-- Bảng Lịch sử giá bán
CREATE TABLE unit_price_history (
    unit_history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID REFERENCES product_units(unit_id) ON DELETE CASCADE,
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL, 
    selling_price DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Phiếu nhập kho
CREATE TABLE stock_in_notes (
    note_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) UNIQUE NOT NULL,
    supplier_id UUID REFERENCES suppliers(supplier_id) ON DELETE RESTRICT,
    receipt_date TIMESTAMP NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    paid_amount DECIMAL(15,2) DEFAULT 0,
    debt_amount DECIMAL(15,2) NOT NULL, 
    payment_status payment_status_enum DEFAULT 'UNPAID',
    note TEXT,
    status note_status_enum DEFAULT 'PENDING',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Chi tiết phiếu nhập kho
CREATE TABLE stock_in_item (
    item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    note_id UUID REFERENCES stock_in_notes(note_id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(product_id) ON DELETE RESTRICT,
    unit_id UUID REFERENCES product_units(unit_id) ON DELETE RESTRICT,
    quantity DECIMAL(10,2) NOT NULL,
    conversion_factor DECIMAL(10,2) NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    total_price DECIMAL(15,2) NOT NULL, 
    expiry_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Quản lý Lô (Lot)
CREATE TABLE stock_lot (
    lot_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID REFERENCES products(product_id) ON DELETE RESTRICT,
    stock_in_note_id UUID REFERENCES stock_in_notes(note_id) ON DELETE CASCADE,
    lot_code VARCHAR(100) NOT NULL,
    initial_quantity DECIMAL(10,2) NOT NULL,
    current_quantity DECIMAL(10,2) NOT NULL,
    unit_cost DECIMAL(15,2) NOT NULL,
    expiry_date DATE,
    status lot_status_enum DEFAULT 'STORAGE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Phiếu xuất kho
CREATE TABLE stock_out_notes (
    note_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) UNIQUE NOT NULL,
    type stock_out_type_enum NOT NULL,
    reference_order_id UUID, 
    total_cost DECIMAL(15,2) NOT NULL, 
    note TEXT,
    status note_status_enum DEFAULT 'PENDING',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Chi tiết phiếu xuất kho
CREATE TABLE stock_out_item (
    item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    note_id UUID REFERENCES stock_out_notes(note_id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(product_id) ON DELETE RESTRICT,
    lot_id UUID REFERENCES stock_lot(lot_id) ON DELETE RESTRICT,
    unit_id UUID REFERENCES product_units(unit_id) ON DELETE RESTRICT,
    quantity DECIMAL(10,2) NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL, 
    total_price DECIMAL(15,2) NOT NULL, 
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Phiếu kiểm kho
CREATE TABLE stocktake_notes (
    note_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) UNIQUE NOT NULL,
    stocktake_date TIMESTAMP NOT NULL,
    handle_by UUID REFERENCES public.users(id) ON DELETE SET NULL,
    created_by UUID REFERENCES public.users(id) ON DELETE SET NULL,
    status stocktake_status_enum NOT NULL DEFAULT 'DRAFT',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Chi tiết kiểm kho: một dòng cho mỗi lô trong phiếu.
-- actual_quantity/diff_quantity/cost_amount để NULL cho đến khi nhân viên nhập số đếm
-- và phiếu được hoàn tất.
CREATE TABLE stocktake_items (
    item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    note_id UUID NOT NULL REFERENCES stocktake_notes(note_id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(product_id) ON DELETE RESTRICT,
    lot_id UUID NOT NULL REFERENCES stock_lot(lot_id) ON DELETE RESTRICT,
    system_quantity DECIMAL(10,2) NOT NULL,
    actual_quantity DECIMAL(10,2),
    diff_quantity DECIMAL(10,2),
    unit_cost DECIMAL(15,2) NOT NULL,
    cost_amount DECIMAL(15,2),
    reason VARCHAR(255),
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_stocktake_note_lot UNIQUE (note_id, lot_id),
    CONSTRAINT ck_stocktake_system_quantity CHECK (system_quantity >= 0),
    CONSTRAINT ck_stocktake_actual_quantity CHECK (actual_quantity IS NULL OR actual_quantity >= 0),
    CONSTRAINT ck_stocktake_unit_cost CHECK (unit_cost >= 0),
    CONSTRAINT ck_stocktake_diff CHECK (
        diff_quantity IS NULL OR (actual_quantity IS NOT NULL AND diff_quantity = actual_quantity - system_quantity)
    ),
    CONSTRAINT ck_stocktake_cost_amount CHECK (
        cost_amount IS NULL OR (diff_quantity IS NOT NULL AND cost_amount = ROUND(diff_quantity * unit_cost, 2))
    ),
    CONSTRAINT ck_stocktake_reason CHECK (
        diff_quantity IS NULL OR diff_quantity = 0 OR reason IS NOT NULL
    )
);

CREATE INDEX idx_stocktake_notes_status ON stocktake_notes(status);
CREATE INDEX idx_stocktake_items_product_lot ON stocktake_items(product_id, lot_id);
