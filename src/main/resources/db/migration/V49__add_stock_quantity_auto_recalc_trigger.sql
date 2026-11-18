-- ==============================================================================
-- Tự động đồng bộ products.stock_quantity theo tổng current_quantity của stock_lot.
-- Lý do: trước đây stock_quantity chỉ được cập nhật thủ công qua
-- ProductServiceImpl.adjustInventoryStock() mỗi khi app xử lý phiếu nhập/xuất/kiểm kê.
-- Nếu có ai insert/update/delete thẳng vào stock_lot (seed data, fix tay, sửa lỗi...)
-- mà quên gọi lại service, stock_quantity sẽ bị lệch so với dữ liệu lô thực tế.
-- Trigger này đảm bảo stock_quantity luôn = SUM(current_quantity) của các lô thuộc
-- sản phẩm đó, bất kể ai/cách nào thay đổi stock_lot.
-- Sản phẩm combo (is_combo = true) không có stock_lot riêng nên không bị ảnh hưởng;
-- tồn kho combo tiếp tục được tính động ở ProductServiceImpl.calculateStock().
-- ==============================================================================

CREATE OR REPLACE FUNCTION recalc_product_stock_quantity() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        UPDATE products
        SET stock_quantity = (
                SELECT COALESCE(SUM(current_quantity), 0) FROM stock_lot WHERE product_id = OLD.product_id
            ),
            updated_at = CURRENT_TIMESTAMP
        WHERE product_id = OLD.product_id;
    END IF;

    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        UPDATE products
        SET stock_quantity = (
                SELECT COALESCE(SUM(current_quantity), 0) FROM stock_lot WHERE product_id = NEW.product_id
            ),
            updated_at = CURRENT_TIMESTAMP
        WHERE product_id = NEW.product_id;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_stock_lot_recalc_stock_quantity ON stock_lot;

CREATE TRIGGER trg_stock_lot_recalc_stock_quantity
    AFTER INSERT OR UPDATE OR DELETE ON stock_lot
    FOR EACH ROW
    EXECUTE FUNCTION recalc_product_stock_quantity();

-- Backfill: đồng bộ lại dữ liệu hiện có (ví dụ sản phẩm được seed thẳng
-- stock_quantity mà không có stock_lot tương ứng sẽ bị đưa về đúng giá trị thực).
UPDATE products p
SET stock_quantity = COALESCE((
        SELECT SUM(l.current_quantity) FROM stock_lot l WHERE l.product_id = p.product_id
    ), 0)
WHERE p.is_combo = FALSE;
