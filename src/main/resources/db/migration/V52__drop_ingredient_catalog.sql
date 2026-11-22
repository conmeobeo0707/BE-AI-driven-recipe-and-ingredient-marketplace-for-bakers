
CREATE TEMP TABLE v52_before ON COMMIT DROP AS
SELECT (SELECT count(*) FROM ingredient_aliases) AS aliases,
       (SELECT count(*) FROM ingredients WHERE recipe_id IS NOT NULL) AS old_rows,
       (SELECT count(*) FROM ingredient_catalog) AS catalogs;

DROP TRIGGER IF EXISTS trg_guard_ingredient_alias_name ON ingredient_aliases;
DROP TRIGGER IF EXISTS trg_guard_ingredient_catalog_name ON ingredient_catalog;
DROP FUNCTION IF EXISTS guard_ingredient_catalog_alias_name();


CREATE TEMP TABLE v52_usage (ingredient_id UUID PRIMARY KEY, uses BIGINT NOT NULL) ON COMMIT DROP;
DO $$
BEGIN
    IF to_regclass('recipe_ingredients') IS NOT NULL THEN
        INSERT INTO v52_usage SELECT ingredient_id, count(*) FROM recipe_ingredients GROUP BY ingredient_id;
    END IF;
END $$;

CREATE TEMP TABLE v52_keeper (catalog_id UUID PRIMARY KEY, ingredient_id UUID NOT NULL) ON COMMIT DROP;
INSERT INTO v52_keeper (catalog_id, ingredient_id)
SELECT DISTINCT ON (i.catalog_ingredient_id) i.catalog_ingredient_id, i.id
FROM ingredients i
LEFT JOIN v52_usage u ON u.ingredient_id = i.id
WHERE i.catalog_ingredient_id IS NOT NULL AND i.recipe_id IS NULL
ORDER BY i.catalog_ingredient_id, coalesce(u.uses, 0) DESC, i.id;

INSERT INTO ingredients (id, name, unit, catalog_ingredient_id)
SELECT gen_random_uuid(),
       c.canonical_name,
       (SELECT i.unit FROM ingredients i
        WHERE i.catalog_ingredient_id = c.id AND i.unit IS NOT NULL
        GROUP BY i.unit ORDER BY count(*) DESC, i.unit LIMIT 1),
       c.id
FROM ingredient_catalog c
WHERE NOT EXISTS (SELECT 1 FROM v52_keeper k WHERE k.catalog_id = c.id);

INSERT INTO v52_keeper (catalog_id, ingredient_id)
SELECT i.catalog_ingredient_id, i.id
FROM ingredients i
WHERE i.catalog_ingredient_id IS NOT NULL AND i.recipe_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM v52_keeper k WHERE k.catalog_id = i.catalog_ingredient_id);

CREATE TEMP TABLE v52_map (old_id UUID PRIMARY KEY, new_id UUID NOT NULL) ON COMMIT DROP;
INSERT INTO v52_map (old_id, new_id)
SELECT i.id, k.ingredient_id
FROM ingredients i
JOIN v52_keeper k ON k.catalog_id = i.catalog_ingredient_id
WHERE i.recipe_id IS NULL;

DO $$
DECLARE
    merged_group RECORD;
    links_before BIGINT;
    merged BIGINT;
BEGIN
    IF to_regclass('recipe_ingredients') IS NULL THEN
        RETURN;
    END IF;
    SELECT count(*) INTO links_before FROM recipe_ingredients;

    CREATE TEMP TABLE v52_ri ON COMMIT DROP AS
    SELECT ri.recipe_id AS owner_id,
           m.new_id,
           count(*) AS links,
           count(DISTINCT coalesce(i.unit, '')) AS units,
           sum(ri.quantity) AS total,
           (array_agg(ri.quantity ORDER BY (ri.ingredient_id = m.new_id) DESC, ri.id))[1] AS kept_quantity,
           (array_agg(ri.id ORDER BY (ri.ingredient_id = m.new_id) DESC, ri.id))[1] AS kept_id
    FROM recipe_ingredients ri
    JOIN v52_map m ON m.old_id = ri.ingredient_id
    JOIN ingredients i ON i.id = ri.ingredient_id
    GROUP BY ri.recipe_id, m.new_id;

    FOR merged_group IN SELECT * FROM v52_ri WHERE links > 1 LOOP
        RAISE NOTICE 'V52 recipe %: % links merged into ingredient % (%)',
            merged_group.owner_id, merged_group.links, merged_group.new_id,
            CASE WHEN merged_group.units = 1 THEN 'quantities added' ELSE 'units differ, kept one quantity' END;
    END LOOP;

    DELETE FROM recipe_ingredients ri
    USING v52_map m, v52_ri g
    WHERE m.old_id = ri.ingredient_id AND g.owner_id = ri.recipe_id AND g.new_id = m.new_id AND ri.id <> g.kept_id;

    UPDATE recipe_ingredients ri
    SET ingredient_id = g.new_id,
        quantity = CASE WHEN g.units = 1 THEN g.total ELSE g.kept_quantity END
    FROM v52_ri g
    WHERE ri.id = g.kept_id;

    SELECT coalesce(sum(links - 1), 0) INTO merged FROM v52_ri;
    IF (SELECT count(*) FROM recipe_ingredients) <> links_before - merged THEN
        RAISE EXCEPTION 'V52: recipe_ingredients count is wrong after merging';
    END IF;
END $$;

DO $$
DECLARE
    merged_group RECORD;
    links_before BIGINT;
    merged BIGINT;
BEGIN
    IF to_regclass('recipe_step_ingredients') IS NULL THEN
        RETURN;
    END IF;
    SELECT count(*) INTO links_before FROM recipe_step_ingredients;

    CREATE TEMP TABLE v52_rsi ON COMMIT DROP AS
    SELECT rsi.recipe_step_id AS owner_id,
           m.new_id,
           count(*) AS links,
           count(DISTINCT coalesce(i.unit, '')) AS units,
           sum(rsi.quantity) AS total,
           (array_agg(rsi.quantity ORDER BY (rsi.ingredient_id = m.new_id) DESC, rsi.id))[1] AS kept_quantity,
           (array_agg(rsi.id ORDER BY (rsi.ingredient_id = m.new_id) DESC, rsi.id))[1] AS kept_id
    FROM recipe_step_ingredients rsi
    JOIN v52_map m ON m.old_id = rsi.ingredient_id
    JOIN ingredients i ON i.id = rsi.ingredient_id
    GROUP BY rsi.recipe_step_id, m.new_id;

    FOR merged_group IN SELECT * FROM v52_rsi WHERE links > 1 LOOP
        RAISE NOTICE 'V52 recipe step %: % links merged into ingredient % (%)',
            merged_group.owner_id, merged_group.links, merged_group.new_id,
            CASE WHEN merged_group.units = 1 THEN 'quantities added' ELSE 'units differ, kept one quantity' END;
    END LOOP;

    DELETE FROM recipe_step_ingredients rsi
    USING v52_map m, v52_rsi g
    WHERE m.old_id = rsi.ingredient_id AND g.owner_id = rsi.recipe_step_id AND g.new_id = m.new_id
      AND rsi.id <> g.kept_id;

    UPDATE recipe_step_ingredients rsi
    SET ingredient_id = g.new_id,
        quantity = CASE WHEN g.units = 1 THEN g.total ELSE g.kept_quantity END
    FROM v52_rsi g
    WHERE rsi.id = g.kept_id;

    SELECT coalesce(sum(links - 1), 0) INTO merged FROM v52_rsi;
    IF (SELECT count(*) FROM recipe_step_ingredients) <> links_before - merged THEN
        RAISE EXCEPTION 'V52: recipe_step_ingredients count is wrong after merging';
    END IF;
END $$;

DELETE FROM ingredients i
USING v52_map m
WHERE m.old_id = i.id AND m.old_id <> m.new_id;

ALTER TABLE ingredient_aliases DROP CONSTRAINT fk_ingredient_aliases_ingredient_catalog;

UPDATE ingredient_aliases a
SET ingredient_id = k.ingredient_id
FROM v52_keeper k
WHERE a.ingredient_id = k.catalog_id;

ALTER TABLE ingredient_aliases
    ADD CONSTRAINT fk_ingredient_aliases_ingredient
        FOREIGN KEY (ingredient_id) REFERENCES ingredients (id) ON DELETE CASCADE;

DROP INDEX IF EXISTS idx_ingredients_catalog_ingredient_id;
ALTER TABLE ingredients DROP COLUMN catalog_ingredient_id;
DROP TABLE ingredient_catalog;

DO $$
DECLARE
    b v52_before%ROWTYPE;
BEGIN
    SELECT * INTO b FROM v52_before;
    IF (SELECT count(*) FROM v52_keeper) <> b.catalogs THEN
        RAISE EXCEPTION 'V52: % catalog entries but % kept ingredients', b.catalogs, (SELECT count(*) FROM v52_keeper);
    END IF;
    IF EXISTS (SELECT 1 FROM v52_keeper k WHERE NOT EXISTS (SELECT 1 FROM ingredients i WHERE i.id = k.ingredient_id)) THEN
        RAISE EXCEPTION 'V52: a kept ingredient was deleted';
    END IF;
    IF (SELECT count(*) FROM ingredient_aliases) <> b.aliases THEN
        RAISE EXCEPTION 'V52: alias count changed from % to %', b.aliases, (SELECT count(*) FROM ingredient_aliases);
    END IF;
    IF (SELECT count(*) FROM ingredients WHERE recipe_id IS NOT NULL) <> b.old_rows THEN
        RAISE EXCEPTION 'V52: old per-recipe ingredient rows changed';
    END IF;
END $$;
