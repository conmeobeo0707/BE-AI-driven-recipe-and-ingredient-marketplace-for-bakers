-- The recommender no longer uses recent searches as a feature (training gave it weight 0),
-- so trained models no longer store a search weight.
ALTER TABLE recommendation_models DROP COLUMN IF EXISTS weight_search;
