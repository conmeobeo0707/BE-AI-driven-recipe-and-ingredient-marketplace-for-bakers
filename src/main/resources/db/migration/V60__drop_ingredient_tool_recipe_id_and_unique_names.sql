CREATE FUNCTION pg_temp.v60_merge_duplicate_names(target regclass) RETURNS void AS $$
DECLARE
    has_recipe_id BOOLEAN;
    fk RECORD;
    conflict RECORD;
    merged BIGINT;
BEGIN
    SELECT EXISTS (SELECT 1 FROM pg_attribute
                   WHERE attrelid = target AND attname = 'recipe_id' AND NOT attisdropped)
    INTO has_recipe_id;

    DROP TABLE IF EXISTS v60_map;
    CREATE TEMP TABLE v60_map (old_id UUID PRIMARY KEY, new_id UUID NOT NULL) ON COMMIT DROP;

    EXECUTE format(
        'INSERT INTO v60_map (old_id, new_id)
         SELECT id, first_value(id) OVER (PARTITION BY lower(btrim(name)) ORDER BY %s id)
         FROM %s
         WHERE name IS NOT NULL',
        CASE WHEN has_recipe_id THEN '(recipe_id IS NOT NULL),' ELSE '' END,
        target);
    DELETE FROM v60_map WHERE old_id = new_id;

    SELECT count(*) INTO merged FROM v60_map;
    IF merged = 0 THEN
        RETURN;
    END IF;

    IF target = 'ingredients'::regclass AND to_regclass('recipe_ingredients') IS NOT NULL THEN
        FOR conflict IN
            SELECT ri.recipe_id, m.new_id
            FROM recipe_ingredients ri
            JOIN v60_map m ON m.old_id = ri.ingredient_id
            WHERE EXISTS (SELECT 1 FROM recipe_ingredients k
                          WHERE k.recipe_id = ri.recipe_id AND k.ingredient_id = m.new_id)
               OR EXISTS (SELECT 1 FROM recipe_ingredients o
                          JOIN v60_map om ON om.old_id = o.ingredient_id
                          WHERE o.recipe_id = ri.recipe_id AND om.new_id = m.new_id AND o.id <> ri.id)
        LOOP
            RAISE EXCEPTION 'V60: recipe % uses ingredient % more than once under duplicate names, merge it manually first',
                conflict.recipe_id, conflict.new_id;
        END LOOP;
    END IF;

    IF target = 'tools'::regclass AND to_regclass('recipe_tools') IS NOT NULL THEN
        FOR conflict IN
            SELECT rt.recipe_id, m.new_id
            FROM recipe_tools rt
            JOIN v60_map m ON m.old_id = rt.tool_id
            WHERE EXISTS (SELECT 1 FROM recipe_tools k
                          WHERE k.recipe_id = rt.recipe_id AND k.tool_id = m.new_id)
               OR EXISTS (SELECT 1 FROM recipe_tools o
                          JOIN v60_map om ON om.old_id = o.tool_id
                          WHERE o.recipe_id = rt.recipe_id AND om.new_id = m.new_id AND o.id <> rt.id)
        LOOP
            RAISE EXCEPTION 'V60: recipe % uses tool % more than once under duplicate names, merge it manually first',
                conflict.recipe_id, conflict.new_id;
        END LOOP;
    END IF;

    FOR fk IN
        SELECT c.conrelid::regclass AS tbl, a.attname AS col
        FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
        WHERE c.contype = 'f' AND c.confrelid = target AND array_length(c.conkey, 1) = 1
    LOOP
        EXECUTE format('UPDATE %s t SET %I = m.new_id FROM v60_map m WHERE t.%I = m.old_id',
                       fk.tbl, fk.col, fk.col);
    END LOOP;

    EXECUTE format('DELETE FROM %s t USING v60_map m WHERE t.id = m.old_id', target);
    RAISE NOTICE 'V60: merged % duplicate rows of %', merged, target;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
    IF to_regclass('ingredients') IS NOT NULL THEN
        PERFORM pg_temp.v60_merge_duplicate_names('ingredients'::regclass);
    END IF;
    IF to_regclass('tools') IS NOT NULL THEN
        PERFORM pg_temp.v60_merge_duplicate_names('tools'::regclass);
    END IF;
    IF to_regclass('recipe_categories') IS NOT NULL THEN
        PERFORM pg_temp.v60_merge_duplicate_names('recipe_categories'::regclass);
    END IF;
END $$;

ALTER TABLE IF EXISTS ingredients DROP COLUMN IF EXISTS recipe_id;
ALTER TABLE IF EXISTS tools DROP COLUMN IF EXISTS recipe_id;

UPDATE ingredients SET name = btrim(name) WHERE name <> btrim(name);
UPDATE tools SET name = btrim(name) WHERE name <> btrim(name);
UPDATE recipe_categories SET name = btrim(name) WHERE name <> btrim(name);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ingredients_name_ci ON ingredients (lower(btrim(name)));
CREATE UNIQUE INDEX IF NOT EXISTS uk_tools_name_ci ON tools (lower(btrim(name)));
CREATE UNIQUE INDEX IF NOT EXISTS uk_recipe_categories_name_ci ON recipe_categories (lower(btrim(name)));
