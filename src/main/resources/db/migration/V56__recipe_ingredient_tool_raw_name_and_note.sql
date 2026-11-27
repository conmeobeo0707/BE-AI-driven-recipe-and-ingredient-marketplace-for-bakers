DO $$
BEGIN
    IF to_regclass('recipe_ingredients') IS NOT NULL THEN
        ALTER TABLE recipe_ingredients ADD COLUMN IF NOT EXISTS raw_name VARCHAR(255);
        ALTER TABLE recipe_ingredients ADD COLUMN IF NOT EXISTS note TEXT;
        ALTER TABLE recipe_ingredients ALTER COLUMN ingredient_id DROP NOT NULL;
        ALTER TABLE recipe_ingredients ALTER COLUMN quantity DROP NOT NULL;

        UPDATE recipe_ingredients ri
        SET raw_name = i.name
        FROM ingredients i
        WHERE i.id = ri.ingredient_id AND ri.raw_name IS NULL;
    END IF;

    IF to_regclass('recipe_tools') IS NOT NULL THEN
        ALTER TABLE recipe_tools ADD COLUMN IF NOT EXISTS raw_name VARCHAR(255);
        ALTER TABLE recipe_tools ADD COLUMN IF NOT EXISTS note TEXT;
        ALTER TABLE recipe_tools ALTER COLUMN tool_id DROP NOT NULL;
        ALTER TABLE recipe_tools ALTER COLUMN quantity DROP NOT NULL;

        UPDATE recipe_tools rt
        SET raw_name = t.name
        FROM tools t
        WHERE t.id = rt.tool_id AND rt.raw_name IS NULL;
    END IF;
END $$;
