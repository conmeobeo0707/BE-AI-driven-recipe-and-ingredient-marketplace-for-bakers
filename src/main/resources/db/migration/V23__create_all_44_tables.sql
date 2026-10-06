-- V23__create_all_44_tables.sql
-- PostgreSQL
-- Creates the 44 tables based on the provided schema.

CREATE TABLE IF NOT EXISTS users (
                                     id UUID PRIMARY KEY,
                                     email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    full_name VARCHAR(255),
    phone VARCHAR(50),
    address VARCHAR(512),
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    role VARCHAR(50),
    gender VARCHAR(50),
    date_of_birth VARCHAR(255),
    updated_at TIMESTAMP,
    avatar_url VARCHAR(1024)
    );

CREATE TABLE IF NOT EXISTS suppliers (
                                         id UUID PRIMARY KEY,
                                         name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    email VARCHAR(255),
    address VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS product_categories (
                                                  id UUID PRIMARY KEY,
                                                  name VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS products (
                                        id UUID PRIMARY KEY,
                                        sku VARCHAR(255) UNIQUE,
    name VARCHAR(255),
    description VARCHAR(255),
    category_id UUID,
    discount_percent DECIMAL,
    img_url VARCHAR(255),
    storage_guide VARCHAR(255),
    min_stock DECIMAL,
    max_stock DECIMAL,
    status VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    brand VARCHAR(255),
    CONSTRAINT fk_products_category
    FOREIGN KEY (category_id) REFERENCES product_categories(id)
    );

CREATE TABLE IF NOT EXISTS units (
                                     id UUID PRIMARY KEY,
                                     product_id UUID,
                                     conversion_factor DECIMAL,
                                     name VARCHAR(255),
    is_base BOOLEAN,
    is_default_purchase BOOLEAN,
    is_default_sale BOOLEAN,
    selling_price DECIMAL,
    status VARCHAR(50),
    CONSTRAINT fk_units_product
    FOREIGN KEY (product_id) REFERENCES products(id)
    );

CREATE TABLE IF NOT EXISTS product_price_history (
                                                     id UUID PRIMARY KEY,
                                                     changed_by UUID,
                                                     price DECIMAL,
                                                     uom_id UUID,
                                                     created_at TIMESTAMP,
                                                     CONSTRAINT fk_product_price_history_changed_by
                                                     FOREIGN KEY (changed_by) REFERENCES users(id),
    CONSTRAINT fk_product_price_history_uom
    FOREIGN KEY (uom_id) REFERENCES units(id)
    );

CREATE TABLE IF NOT EXISTS children_products (
                                                 id UUID PRIMARY KEY,
                                                 parent_product_id UUID,
                                                 child_product_id UUID,
                                                 quantity DECIMAL,
                                                 CONSTRAINT fk_children_products_parent
                                                 FOREIGN KEY (parent_product_id) REFERENCES products(id),
    CONSTRAINT fk_children_products_child
    FOREIGN KEY (child_product_id) REFERENCES products(id)
    );

CREATE TABLE IF NOT EXISTS stock_in_notes (
                                              id UUID PRIMARY KEY,
                                              code VARCHAR(255),
    supplier_id UUID,
    status VARCHAR(50),
    total_price DECIMAL,
    paid_amount DECIMAL,
    debt_amount DECIMAL,
    payment_status VARCHAR(50),
    note VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_stock_in_notes_supplier
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
    );

CREATE TABLE IF NOT EXISTS stock_in_items (
                                              id UUID PRIMARY KEY,
                                              stock_in_note_id UUID,
                                              product_id UUID,
                                              product_unite_id UUID,
                                              quantity DECIMAL,
                                              unit_price DECIMAL,
                                              total_price DECIMAL,
                                              created_at TIMESTAMP,
                                              CONSTRAINT fk_stock_in_items_note
                                              FOREIGN KEY (stock_in_note_id) REFERENCES stock_in_notes(id),
    CONSTRAINT fk_stock_in_items_product
    FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_stock_in_items_unit
    FOREIGN KEY (product_unite_id) REFERENCES units(id)
    );

CREATE TABLE IF NOT EXISTS stock_lot (
                                         id UUID PRIMARY KEY,
                                         product_id UUID,
                                         stock_in_note_id UUID,
                                         lot_code VARCHAR(255),
    import_price DECIMAL,
    initial_quantity DECIMAL,
    current_quantity DECIMAL,
    expiry_date TIMESTAMP,
    created_at TIMESTAMP,
    CONSTRAINT fk_stock_lot_product
    FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_stock_lot_stock_in_note
    FOREIGN KEY (stock_in_note_id) REFERENCES stock_in_notes(id)
    );

CREATE TABLE IF NOT EXISTS stock_out_notes (
                                               id UUID PRIMARY KEY,
                                               code VARCHAR(255),
    type VARCHAR(255),
    total_amount DECIMAL,
    total_cogs DECIMAL,
    note VARCHAR(255),
    created_by UUID,
    created_at TIMESTAMP,
    CONSTRAINT fk_stock_out_notes_created_by
    FOREIGN KEY (created_by) REFERENCES users(id)
    );

CREATE TABLE IF NOT EXISTS stock_out_items (
                                               id UUID PRIMARY KEY,
                                               stock_out_note_id UUID,
                                               product_id UUID,
                                               lot_id UUID,
                                               unit_product_id UUID,
                                               base_quantity DECIMAL,
                                               unit_price DECIMAL,
                                               import_price VARCHAR(255),
    cogs_amount DECIMAL,
    total_price DECIMAL,
    created_at TIMESTAMP,
    CONSTRAINT fk_stock_out_items_note
    FOREIGN KEY (stock_out_note_id) REFERENCES stock_out_notes(id),
    CONSTRAINT fk_stock_out_items_product
    FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_stock_out_items_lot
    FOREIGN KEY (lot_id) REFERENCES stock_lot(id),
    CONSTRAINT fk_stock_out_items_unit
    FOREIGN KEY (unit_product_id) REFERENCES units(id)
    );

CREATE TABLE IF NOT EXISTS stocktakings (
                                            id UUID PRIMARY KEY,
                                            code VARCHAR(255) UNIQUE,
    stocktake_date DATE,
    created_by UUID,
    created_at TIMESTAMP,
    CONSTRAINT fk_stocktakings_created_by
    FOREIGN KEY (created_by) REFERENCES users(id)
    );

CREATE TABLE IF NOT EXISTS stocktaking_details (
                                                   id UUID PRIMARY KEY,
                                                   stocktaking_id UUID,
                                                   product_id UUID,
                                                   lot_id UUID,
                                                   system_base_quantity DECIMAL,
                                                   actual_base_quantity DECIMAL,
                                                   diff_quantity DECIMAL,
                                                   reason VARCHAR(255),
    cost_amount DECIMAL,
    note VARCHAR(255),
    CONSTRAINT fk_stocktaking_details_stocktaking
    FOREIGN KEY (stocktaking_id) REFERENCES stocktakings(id),
    CONSTRAINT fk_stocktaking_details_product
    FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_stocktaking_details_lot
    FOREIGN KEY (lot_id) REFERENCES stock_lot(id)
    );

CREATE TABLE IF NOT EXISTS stock_transactions (
                                                  id UUID PRIMARY KEY,
                                                  stock_lot_id UUID,
                                                  product_id UUID,
                                                  transaction_type VARCHAR(255),
    quantity_change DECIMAL,
    balance_after DECIMAL,
    ref_type VARCHAR(255),
    ref_id UUID,
    created_by UUID,
    created_at TIMESTAMP,
    CONSTRAINT fk_stock_transactions_lot
    FOREIGN KEY (stock_lot_id) REFERENCES stock_lot(id),
    CONSTRAINT fk_stock_transactions_product
    FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_stock_transactions_created_by
    FOREIGN KEY (created_by) REFERENCES users(id)
    );

CREATE TABLE IF NOT EXISTS receipt_categories (
                                                  id UUID PRIMARY KEY,
                                                  name VARCHAR(255),
    type VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS receipt_vouchers (
                                                id UUID PRIMARY KEY,
                                                code VARCHAR(255) UNIQUE,
    receipt_category_id UUID,
    amount DECIMAL,
    created_at TIMESTAMP,
    CONSTRAINT fk_receipt_vouchers_category
    FOREIGN KEY (receipt_category_id) REFERENCES receipt_categories(id)
    );

CREATE TABLE IF NOT EXISTS supplier_payments (
                                                 id UUID PRIMARY KEY,
                                                 code VARCHAR(255),
    supplier_id UUID,
    amount DECIMAL,
    created_at TIMESTAMP,
    created_by UUID,
    CONSTRAINT fk_supplier_payments_supplier
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_supplier_payments_created_by
    FOREIGN KEY (created_by) REFERENCES users(id)
    );

CREATE TABLE IF NOT EXISTS supplier_payment_details (
                                                        id UUID PRIMARY KEY,
                                                        supplier_payment_id UUID,
                                                        stock_in_note_id UUID,
                                                        amount DECIMAL,
                                                        CONSTRAINT fk_supplier_payment_details_payment
                                                        FOREIGN KEY (supplier_payment_id) REFERENCES supplier_payments(id),
    CONSTRAINT fk_supplier_payment_details_stock_in_note
    FOREIGN KEY (stock_in_note_id) REFERENCES stock_in_notes(id)
    );

CREATE TABLE IF NOT EXISTS expense_categories (
                                                  id UUID PRIMARY KEY,
                                                  name VARCHAR(255),
    type VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS expense_vouchers (
                                                id UUID PRIMARY KEY,
                                                code VARCHAR(255) UNIQUE,
    expense_category_id UUID,
    amount DECIMAL,
    reason VARCHAR(255),
    created_by UUID,
    created_at TIMESTAMP,
    CONSTRAINT fk_expense_vouchers_category
    FOREIGN KEY (expense_category_id) REFERENCES expense_categories(id),
    CONSTRAINT fk_expense_vouchers_created_by
    FOREIGN KEY (created_by) REFERENCES users(id)
    );

CREATE TABLE IF NOT EXISTS hobbies (
                                       id UUID PRIMARY KEY,
                                       name VARCHAR(255),
    description VARCHAR(255),
    is_active BOOLEAN
    );

CREATE TABLE IF NOT EXISTS customers (
                                         customer_id UUID PRIMARY KEY,
                                         user_id UUID,
                                         hobby_id UUID,
                                         skill_level VARCHAR(255),
    is_confirmed BOOLEAN,
    CONSTRAINT fk_customers_user
    FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_customers_hobby
    FOREIGN KEY (hobby_id) REFERENCES hobbies(id)
    );

CREATE TABLE IF NOT EXISTS customer_hobbies (
                                                customer_id UUID,
                                                hobby_id UUID,
                                                name VARCHAR(255),
    created_at TIMESTAMP,
    PRIMARY KEY (customer_id, hobby_id),
    CONSTRAINT fk_customer_hobbies_customer
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    CONSTRAINT fk_customer_hobbies_hobby
    FOREIGN KEY (hobby_id) REFERENCES hobbies(id)
    );

CREATE TABLE IF NOT EXISTS request_quote (
                                             id UUID PRIMARY KEY,
                                             customer_id UUID,
                                             content VARCHAR(255),
    status VARCHAR(255),
    created_at TIMESTAMP,
    CONSTRAINT fk_request_quote_customer
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
    );

CREATE TABLE IF NOT EXISTS request_quote_detail (
                                                    id UUID PRIMARY KEY,
                                                    product_id UUID,
                                                    request_quote_id UUID,
                                                    quantity DECIMAL,
                                                    CONSTRAINT fk_request_quote_detail_product
                                                    FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_request_quote_detail_quote
    FOREIGN KEY (request_quote_id) REFERENCES request_quote(id)
    );

CREATE TABLE IF NOT EXISTS carts (
                                     id UUID PRIMARY KEY,
                                     user_id UUID,
                                     created_at TIMESTAMP,
                                     CONSTRAINT fk_carts_user
                                     FOREIGN KEY (user_id) REFERENCES users(id)
    );

CREATE TABLE IF NOT EXISTS cart_items (
                                          id UUID PRIMARY KEY,
                                          cart_id UUID,
                                          product_id UUID,
                                          uom_id UUID,
                                          quantity DECIMAL,
                                          added_at TIMESTAMP,
                                          CONSTRAINT fk_cart_items_cart
                                          FOREIGN KEY (cart_id) REFERENCES carts(id),
    CONSTRAINT fk_cart_items_product
    FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_cart_items_uom
    FOREIGN KEY (uom_id) REFERENCES units(id)
    );

CREATE TABLE IF NOT EXISTS orders (
                                      id UUID PRIMARY KEY,
                                      customer_id UUID,
                                      status VARCHAR(255),
    products_price DECIMAL,
    shipping_fee DECIMAL,
    discount_amount DECIMAL,
    total DECIMAL,
    revenue DECIMAL,
    note VARCHAR(255),
    created_at TIMESTAMP,
    CONSTRAINT fk_orders_customer
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
    );

CREATE TABLE IF NOT EXISTS order_items (
                                           id UUID PRIMARY KEY,
                                           order_id UUID,
                                           product_id UUID,
                                           uom_id UUID,
                                           quantity DECIMAL,
                                           unit_price DECIMAL,
                                           CONSTRAINT fk_order_items_order
                                           FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_product
    FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_order_items_uom
    FOREIGN KEY (uom_id) REFERENCES units(id)
    );

CREATE TABLE IF NOT EXISTS payments (
                                        id UUID PRIMARY KEY,
                                        order_id UUID,
                                        method VARCHAR(255),
    amount DECIMAL,
    expected_net_amount DECIMAL,
    settle_amount DECIMAL,
    status VARCHAR(255),
    external_id VARCHAR(255),
    paid_at TIMESTAMP,
    settled_at TIMESTAMP,
    created_at TIMESTAMP,
    CONSTRAINT fk_payments_order
    FOREIGN KEY (order_id) REFERENCES orders(id)
    );

CREATE TABLE IF NOT EXISTS payment_transactions (
                                                    id UUID PRIMARY KEY,
                                                    ref_type VARCHAR(255),
    ref_id UUID,
    direction VARCHAR(255),
    amount DECIMAL,
    method VARCHAR(255),
    external_id VARCHAR(255),
    note VARCHAR(255),
    created_by UUID,
    created_at TIMESTAMP,
    CONSTRAINT fk_payment_transactions_created_by
    FOREIGN KEY (created_by) REFERENCES users(id)
    );

CREATE TABLE IF NOT EXISTS shipments (
                                         id UUID PRIMARY KEY,
                                         order_id UUID,
                                         provider VARCHAR(255),
    tracking_code VARCHAR(255),
    external_id VARCHAR(255),
    status VARCHAR(255),
    shipping_fee DECIMAL,
    cod_amount DECIMAL,
    recipient_name VARCHAR(255),
    recipient_phone VARCHAR(50),
    recipient_address VARCHAR(255),
    weight DECIMAL,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_shipments_order
    FOREIGN KEY (order_id) REFERENCES orders(id)
    );

CREATE TABLE IF NOT EXISTS recipe_categories (
                                                 id UUID PRIMARY KEY,
                                                 name VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS recipe (
                                      id UUID PRIMARY KEY,
                                      user_id UUID,
                                      category_id UUID,
                                      title VARCHAR(255),
    description VARCHAR(255),
    video_url VARCHAR(255),
    difficulty VARCHAR(255),
    prep_time_min INTEGER,
    cook_time_min INTEGER,
    servings INTEGER,
    status VARCHAR(255),
    created_at TIMESTAMP,
    CONSTRAINT fk_recipe_user
    FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_recipe_category
    FOREIGN KEY (category_id) REFERENCES recipe_categories(id)
    );

CREATE TABLE IF NOT EXISTS recipe_steps (
                                            id UUID PRIMARY KEY,
                                            recipe_id UUID,
                                            step_number INTEGER,
                                            instruction VARCHAR(255),
    image_url VARCHAR(255),
    CONSTRAINT fk_recipe_steps_recipe
    FOREIGN KEY (recipe_id) REFERENCES recipe(id)
    );

CREATE TABLE IF NOT EXISTS tools (
                                     id UUID PRIMARY KEY,
                                     recipe_id UUID,
                                     name VARCHAR(255),
    CONSTRAINT fk_tools_recipe
    FOREIGN KEY (recipe_id) REFERENCES recipe(id)
    );

CREATE TABLE IF NOT EXISTS ingredients (
                                           id UUID PRIMARY KEY,
                                           recipe_id UUID,
                                           name VARCHAR(255),
    unit VARCHAR(255),
    quantity DECIMAL,
    CONSTRAINT fk_ingredients_recipe
    FOREIGN KEY (recipe_id) REFERENCES recipe(id)
    );

CREATE TABLE IF NOT EXISTS bookmarks (
                                         id UUID PRIMARY KEY,
                                         recipe_id UUID,
                                         customer_id UUID,
                                         created_at TIMESTAMP,
                                         CONSTRAINT fk_bookmarks_recipe
                                         FOREIGN KEY (recipe_id) REFERENCES recipe(id),
    CONSTRAINT fk_bookmarks_customer
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
    );

CREATE TABLE IF NOT EXISTS ratings (
                                       id UUID PRIMARY KEY,
                                       recipe_id UUID,
                                       customer_id UUID,
                                       score INTEGER,
                                       created_at TIMESTAMP,
                                       CONSTRAINT fk_ratings_recipe
                                       FOREIGN KEY (recipe_id) REFERENCES recipe(id),
    CONSTRAINT fk_ratings_customer
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
    );

CREATE TABLE IF NOT EXISTS comments (
                                        id UUID PRIMARY KEY,
                                        recipe_id UUID,
                                        user_id UUID,
                                        content VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    parent_id UUID,
    CONSTRAINT fk_comments_recipe
    FOREIGN KEY (recipe_id) REFERENCES recipe(id),
    CONSTRAINT fk_comments_user
    FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_comments_parent
    FOREIGN KEY (parent_id) REFERENCES comments(id)
    );

CREATE TABLE IF NOT EXISTS verification_codes (
                                                  id UUID PRIMARY KEY,
                                                  user_id UUID,
                                                  code_hash VARCHAR(255),
    expires_at TIMESTAMP,
    created_at TIMESTAMP,
    purpose VARCHAR(255),
    CONSTRAINT fk_verification_codes_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    );

CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id UUID PRIMARY KEY,
                                              user_id UUID,
                                              token_hash VARCHAR(255),
    expires_at TIMESTAMP,
    created_at TIMESTAMP,
    revoked_at TIMESTAMP,
    replaced_by_id UUID,
    CONSTRAINT fk_refresh_tokens_user
    FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_refresh_tokens_replaced_by
    FOREIGN KEY (replaced_by_id) REFERENCES refresh_tokens(id)
    );

CREATE TABLE IF NOT EXISTS notification (
                                            id UUID PRIMARY KEY,
                                            content VARCHAR(255),
    user_id UUID,
    ref_type VARCHAR(255),
    ref_id UUID,
    CONSTRAINT fk_notification_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    );

-- 44th table-related indexes
CREATE INDEX IF NOT EXISTS idx_products_category_id
    ON products(category_id);

CREATE INDEX IF NOT EXISTS idx_units_product_id
    ON units(product_id);

CREATE INDEX IF NOT EXISTS idx_stock_lot_product_id
    ON stock_lot(product_id);

CREATE INDEX IF NOT EXISTS idx_stock_in_items_note_id
    ON stock_in_items(stock_in_note_id);

CREATE INDEX IF NOT EXISTS idx_stock_out_items_note_id
    ON stock_out_items(stock_out_note_id);

CREATE INDEX IF NOT EXISTS idx_orders_customer_id
    ON orders(customer_id);

CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id
    ON cart_items(cart_id);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_created_by
    ON payment_transactions(created_by);

CREATE INDEX IF NOT EXISTS idx_notification_user_id
    ON notification(user_id);
