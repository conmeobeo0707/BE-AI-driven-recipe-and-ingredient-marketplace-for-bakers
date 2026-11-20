-- Searches are no longer recorded: the recommender does not use them (V43) and nothing else reads them.
-- Every remaining interaction is about a recipe, so recipe_id becomes required.
DELETE FROM recipe_interactions WHERE interaction_type = 'SEARCH';

ALTER TABLE recipe_interactions DROP CONSTRAINT IF EXISTS chk_recipe_interaction_search;
ALTER TABLE recipe_interactions DROP CONSTRAINT IF EXISTS chk_recipe_interaction_target;
ALTER TABLE recipe_interactions DROP CONSTRAINT IF EXISTS chk_recipe_interaction_type;
ALTER TABLE recipe_interactions DROP COLUMN IF EXISTS search_query;
ALTER TABLE recipe_interactions ALTER COLUMN recipe_id SET NOT NULL;
ALTER TABLE recipe_interactions ADD CONSTRAINT chk_recipe_interaction_type
    CHECK (interaction_type IN ('VIEW', 'BOOKMARK', 'RATING'));
