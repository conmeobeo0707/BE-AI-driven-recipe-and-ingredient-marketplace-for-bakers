ALTER TABLE recipe ALTER COLUMN prep_time_min DROP NOT NULL;
ALTER TABLE recipe ALTER COLUMN cook_time_min DROP NOT NULL;
ALTER TABLE recipe ALTER COLUMN servings DROP NOT NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'recipe_steps'
                 AND column_name = 'duration_minutes') THEN
        ALTER TABLE recipe_steps ALTER COLUMN duration_minutes DROP NOT NULL;
        ALTER TABLE recipe_steps ALTER COLUMN duration_minutes DROP DEFAULT;
    END IF;
END $$;

UPDATE recipe_steps SET instruction = '' WHERE instruction IS NULL;
ALTER TABLE recipe_steps ALTER COLUMN instruction TYPE TEXT;
ALTER TABLE recipe_steps ALTER COLUMN instruction SET NOT NULL;
