DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM ingredient_catalog c
        JOIN ingredient_aliases a ON a.normalized_alias = c.normalized_name
        WHERE a.ingredient_id <> c.id
    ) THEN
        RAISE EXCEPTION 'Resolve conflicting catalog and alias names before applying V40';
    END IF;
END $$;

CREATE FUNCTION guard_ingredient_catalog_alias_name()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(39, 1);

    IF TG_TABLE_NAME = 'ingredient_catalog' THEN
        IF EXISTS (
            SELECT 1 FROM ingredient_aliases a
            WHERE a.normalized_alias = NEW.normalized_name
              AND a.ingredient_id <> NEW.id
        ) THEN
            RAISE EXCEPTION USING
                ERRCODE = '23505',
                CONSTRAINT = 'uq_ingredient_catalog_alias_name',
                MESSAGE = 'Normalized catalog name belongs to another ingredient alias';
        END IF;
    ELSE
        IF EXISTS (
            SELECT 1 FROM ingredient_catalog c
            WHERE c.normalized_name = NEW.normalized_alias
              AND c.id <> NEW.ingredient_id
        ) THEN
            RAISE EXCEPTION USING
                ERRCODE = '23505',
                CONSTRAINT = 'uq_ingredient_catalog_alias_name',
                MESSAGE = 'Normalized alias belongs to another canonical ingredient';
        END IF;
    END IF;

    RETURN NEW;
END $$;

CREATE TRIGGER trg_guard_ingredient_catalog_name
BEFORE INSERT OR UPDATE OF normalized_name ON ingredient_catalog
FOR EACH ROW EXECUTE FUNCTION guard_ingredient_catalog_alias_name();

CREATE TRIGGER trg_guard_ingredient_alias_name
BEFORE INSERT OR UPDATE OF normalized_alias, ingredient_id ON ingredient_aliases
FOR EACH ROW EXECUTE FUNCTION guard_ingredient_catalog_alias_name();
