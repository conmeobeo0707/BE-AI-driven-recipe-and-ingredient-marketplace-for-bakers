-- ==============================================================================
-- Tự động đồng bộ recipe.average_rating theo AVG(score) của các dòng trong ratings.
-- Lý do: trước đây average_rating chỉ được cập nhật thủ công qua
-- RecipeRatingServiceImpl.refreshRecipeAverageRating() mỗi khi app xử lý tạo/sửa/xóa rating.
-- Nếu có ai insert/update/delete thẳng vào ratings (seed data, fix tay, migrate dữ liệu...)
-- mà không đi qua service đó, average_rating sẽ bị lệch so với dữ liệu rating thực tế.
-- Trigger này đảm bảo average_rating luôn = AVG(score) của các rating thuộc recipe đó,
-- bất kể ai/cách nào thay đổi bảng ratings. Khi UPDATE đổi recipe_id, cả recipe cũ và
-- recipe mới đều được tính lại (giống logic previousRecipeId trong service).
-- ==============================================================================

CREATE OR REPLACE FUNCTION recalc_recipe_average_rating() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        UPDATE recipe
        SET average_rating = COALESCE((SELECT AVG(score) FROM ratings WHERE recipe_id = OLD.recipe_id), 0)
        WHERE id = OLD.recipe_id;
    END IF;

    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        UPDATE recipe
        SET average_rating = COALESCE((SELECT AVG(score) FROM ratings WHERE recipe_id = NEW.recipe_id), 0)
        WHERE id = NEW.recipe_id;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_ratings_recalc_average_rating ON ratings;

CREATE TRIGGER trg_ratings_recalc_average_rating
    AFTER INSERT OR UPDATE OR DELETE ON ratings
    FOR EACH ROW
    EXECUTE FUNCTION recalc_recipe_average_rating();

-- Backfill: đồng bộ lại dữ liệu hiện có (recipe có average_rating lệch do ratings
-- bị thay đổi trực tiếp trước khi có trigger này).
UPDATE recipe r
SET average_rating = COALESCE((SELECT AVG(x.score) FROM ratings x WHERE x.recipe_id = r.id), 0);
