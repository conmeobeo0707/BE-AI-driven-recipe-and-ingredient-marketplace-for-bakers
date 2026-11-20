-- source only repeated other columns: RECOMMENDATION means recomm_id is set, SEARCH means interaction_type = 'SEARCH'.
-- Nothing reads it, so it is dropped (its CHECK constraint goes with it).
ALTER TABLE recipe_interactions DROP CONSTRAINT IF EXISTS chk_recipe_interaction_source;
ALTER TABLE recipe_interactions DROP COLUMN IF EXISTS source;
