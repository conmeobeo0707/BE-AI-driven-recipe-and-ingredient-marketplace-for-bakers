-- The unit belongs to how much of an ingredient a recipe uses, not to the ingredient itself.
-- Each recipe_ingredients row takes the unit its ingredient had. Old per-recipe ingredient rows
-- (recipe_id IS NOT NULL) have no recipe_ingredients row, so their unit is dropped with the column.

-- recipe_ingredients is created by Hibernate, so it may not exist on a fresh database.
DO $$
BEGIN
    IF to_regclass('recipe_ingredients') IS NULL THEN
        RETURN;
    END IF;

    ALTER TABLE recipe_ingredients ADD COLUMN IF NOT EXISTS unit VARCHAR(255);

    UPDATE recipe_ingredients ri
    SET unit = i.unit
    FROM ingredients i
    WHERE i.id = ri.ingredient_id AND ri.unit IS NULL;

    IF EXISTS (
        SELECT 1 FROM recipe_ingredients ri JOIN ingredients i ON i.id = ri.ingredient_id
        WHERE ri.unit IS DISTINCT FROM i.unit
    ) THEN
        RAISE EXCEPTION 'V53: some recipe_ingredients rows did not get their ingredient''s unit';
    END IF;
END $$;

ALTER TABLE ingredients DROP COLUMN unit;
