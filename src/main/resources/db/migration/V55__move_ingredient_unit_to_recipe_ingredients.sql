DO $$
DECLARE
    has_recipe_ingredients BOOLEAN;
    has_ingredient_unit BOOLEAN;
BEGIN
    SELECT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = current_schema() AND table_name = 'recipe_ingredients')
    INTO has_recipe_ingredients;

    SELECT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = current_schema() AND table_name = 'ingredients'
                     AND column_name = 'unit')
    INTO has_ingredient_unit;

    IF has_recipe_ingredients THEN
        ALTER TABLE recipe_ingredients ADD COLUMN IF NOT EXISTS unit VARCHAR(50);

        IF has_ingredient_unit THEN
            UPDATE recipe_ingredients ri
            SET unit = LEFT(NULLIF(BTRIM(i.unit), ''), 50)
            FROM ingredients i
            WHERE ri.ingredient_id = i.id
              AND ri.unit IS NULL;
        END IF;
    END IF;

    IF has_ingredient_unit THEN
        ALTER TABLE ingredients DROP COLUMN unit;
    END IF;
END $$;
