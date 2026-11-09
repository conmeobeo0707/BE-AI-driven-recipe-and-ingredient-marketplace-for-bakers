-- recipe.average_rating was added to the entity without a migration. Hibernate cannot add a NOT NULL column
-- to a table that already has rows, so databases without it fail on every recipe query.
-- IF NOT EXISTS keeps this safe where the column is already there.
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS average_rating NUMERIC(10, 4) NOT NULL DEFAULT 0;

-- Recompute every recipe from its ratings: existing rows were left at 0.
UPDATE recipe r
SET average_rating = COALESCE((SELECT AVG(x.score) FROM ratings x WHERE x.recipe_id = r.id), 0);
