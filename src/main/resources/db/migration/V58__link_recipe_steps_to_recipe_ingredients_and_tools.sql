DO $$
DECLARE
    added BIGINT;
BEGIN
    IF to_regclass('recipe_step_ingredients') IS NULL OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'recipe_step_ingredients' AND column_name = 'ingredient_id') THEN
        RETURN;
    END IF;

    ALTER TABLE recipe_step_ingredients ADD COLUMN IF NOT EXISTS recipe_ingredient_id UUID;

    INSERT INTO recipe_ingredients (id, recipe_id, ingredient_id, raw_name, quantity)
    SELECT gen_random_uuid(), rs.recipe_id, s.ingredient_id, c.name, SUM(s.quantity)
    FROM recipe_step_ingredients s
    JOIN recipe_steps rs ON rs.id = s.recipe_step_id
    JOIN ingredients c ON c.id = s.ingredient_id
    WHERE NOT EXISTS (SELECT 1 FROM recipe_ingredients p WHERE p.recipe_id = rs.recipe_id AND p.ingredient_id = s.ingredient_id)
    GROUP BY rs.recipe_id, s.ingredient_id, c.name;
    GET DIAGNOSTICS added = ROW_COUNT;
    IF added > 0 THEN
        RAISE NOTICE 'V58: added % recipe_ingredients rows for ingredient steps whose ingredient was missing from the recipe', added;
    END IF;

    UPDATE recipe_step_ingredients s
    SET recipe_ingredient_id = p.id
    FROM recipe_steps rs, recipe_ingredients p
    WHERE rs.id = s.recipe_step_id AND p.recipe_id = rs.recipe_id AND p.ingredient_id = s.ingredient_id;

    IF EXISTS (SELECT 1 FROM recipe_step_ingredients WHERE recipe_ingredient_id IS NULL) THEN
        RAISE EXCEPTION 'V58: some recipe_step_ingredients rows could not be linked to recipe_ingredients';
    END IF;

    ALTER TABLE recipe_step_ingredients DROP COLUMN ingredient_id;
    ALTER TABLE recipe_step_ingredients ALTER COLUMN recipe_ingredient_id SET NOT NULL;
    ALTER TABLE recipe_step_ingredients ALTER COLUMN quantity DROP NOT NULL;
    ALTER TABLE recipe_step_ingredients
        ADD CONSTRAINT fk_recipe_step_ingredients_recipe_ingredient
            FOREIGN KEY (recipe_ingredient_id) REFERENCES recipe_ingredients (id) ON DELETE CASCADE;
    ALTER TABLE recipe_step_ingredients
        ADD CONSTRAINT uk_recipe_step_ingredients_step_recipe_ingredient UNIQUE (recipe_step_id, recipe_ingredient_id);
END $$;

DO $$
DECLARE
    added BIGINT;
BEGIN
    IF to_regclass('recipe_step_tools') IS NULL OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'recipe_step_tools' AND column_name = 'tool_id') THEN
        RETURN;
    END IF;

    ALTER TABLE recipe_step_tools ADD COLUMN IF NOT EXISTS recipe_tool_id UUID;

    INSERT INTO recipe_tools (id, recipe_id, tool_id, raw_name, quantity)
    SELECT gen_random_uuid(), rs.recipe_id, s.tool_id, c.name, SUM(s.quantity)
    FROM recipe_step_tools s
    JOIN recipe_steps rs ON rs.id = s.recipe_step_id
    JOIN tools c ON c.id = s.tool_id
    WHERE NOT EXISTS (SELECT 1 FROM recipe_tools p WHERE p.recipe_id = rs.recipe_id AND p.tool_id = s.tool_id)
    GROUP BY rs.recipe_id, s.tool_id, c.name;
    GET DIAGNOSTICS added = ROW_COUNT;
    IF added > 0 THEN
        RAISE NOTICE 'V58: added % recipe_tools rows for tool steps whose tool was missing from the recipe', added;
    END IF;

    UPDATE recipe_step_tools s
    SET recipe_tool_id = p.id
    FROM recipe_steps rs, recipe_tools p
    WHERE rs.id = s.recipe_step_id AND p.recipe_id = rs.recipe_id AND p.tool_id = s.tool_id;

    IF EXISTS (SELECT 1 FROM recipe_step_tools WHERE recipe_tool_id IS NULL) THEN
        RAISE EXCEPTION 'V58: some recipe_step_tools rows could not be linked to recipe_tools';
    END IF;

    ALTER TABLE recipe_step_tools DROP COLUMN tool_id;
    ALTER TABLE recipe_step_tools ALTER COLUMN recipe_tool_id SET NOT NULL;
    ALTER TABLE recipe_step_tools ALTER COLUMN quantity DROP NOT NULL;
    ALTER TABLE recipe_step_tools
        ADD CONSTRAINT fk_recipe_step_tools_recipe_tool
            FOREIGN KEY (recipe_tool_id) REFERENCES recipe_tools (id) ON DELETE CASCADE;
    ALTER TABLE recipe_step_tools
        ADD CONSTRAINT uk_recipe_step_tools_step_recipe_tool UNIQUE (recipe_step_id, recipe_tool_id);
END $$;
