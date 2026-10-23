CREATE TABLE recipe_interactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    recipe_id UUID REFERENCES recipe(id) ON DELETE RESTRICT,
    interaction_type VARCHAR(20) NOT NULL,
    rating_score INTEGER,
    search_query VARCHAR(255),
    source VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_recipe_interaction_type
        CHECK (interaction_type IN ('VIEW', 'BOOKMARK', 'RATING', 'SEARCH')),
    CONSTRAINT chk_recipe_interaction_target
        CHECK (
            (interaction_type = 'SEARCH' AND recipe_id IS NULL)
            OR (interaction_type <> 'SEARCH' AND recipe_id IS NOT NULL)
        ),
    CONSTRAINT chk_recipe_interaction_rating
        CHECK (
            (interaction_type = 'RATING' AND rating_score BETWEEN 1 AND 5)
            OR (interaction_type <> 'RATING' AND rating_score IS NULL)
        ),
    CONSTRAINT chk_recipe_interaction_search
        CHECK (
            (interaction_type = 'SEARCH' AND search_query IS NOT NULL AND length(trim(search_query)) > 0)
            OR (interaction_type <> 'SEARCH' AND search_query IS NULL)
        ),
    CONSTRAINT chk_recipe_interaction_source
        CHECK (source IN ('DIRECT', 'RECOMMENDATION', 'SEARCH', 'OTHER'))
);

CREATE INDEX idx_recipe_interactions_customer_time
    ON recipe_interactions(customer_id, occurred_at DESC);

CREATE INDEX idx_recipe_interactions_customer_type_time
    ON recipe_interactions(customer_id, interaction_type, occurred_at DESC);

INSERT INTO recipe_interactions (customer_id, recipe_id, interaction_type, source, occurred_at)
SELECT customer_id, recipe_id, 'BOOKMARK', 'DIRECT', COALESCE(created_at::timestamptz, CURRENT_TIMESTAMP)
FROM bookmarks
WHERE customer_id IS NOT NULL AND recipe_id IS NOT NULL;

INSERT INTO recipe_interactions (customer_id, recipe_id, interaction_type, rating_score, source, occurred_at)
SELECT customer_id, recipe_id, 'RATING', score, 'DIRECT', COALESCE(created_at::timestamptz, CURRENT_TIMESTAMP)
FROM ratings
WHERE customer_id IS NOT NULL
  AND recipe_id IS NOT NULL
  AND score BETWEEN 1 AND 5;
