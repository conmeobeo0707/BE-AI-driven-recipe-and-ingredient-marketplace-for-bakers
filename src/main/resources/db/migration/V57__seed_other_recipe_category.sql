DO $$
BEGIN
    IF to_regclass('recipe_categories') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM recipe_categories WHERE lower(btrim(name)) IN ('khác', 'khac')) THEN
        INSERT INTO recipe_categories (id, name) VALUES (gen_random_uuid(), 'Khác');
    END IF;
END $$;
