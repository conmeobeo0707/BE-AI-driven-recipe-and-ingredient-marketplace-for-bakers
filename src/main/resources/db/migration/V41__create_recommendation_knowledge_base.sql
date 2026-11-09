-- Knowledge base and trained models for the in-house recipe recommender.

-- Which recipe categories a hobby is related to (managed by ADMIN).
CREATE TABLE hobby_recipe_categories (
    hobby_id    UUID         NOT NULL,
    category_id UUID         NOT NULL,
    weight      NUMERIC(3, 2) NOT NULL DEFAULT 1.00,
    CONSTRAINT pk_hobby_recipe_categories PRIMARY KEY (hobby_id, category_id),
    CONSTRAINT fk_hobby_recipe_categories_hobby
        FOREIGN KEY (hobby_id) REFERENCES hobbies (id) ON DELETE CASCADE,
    CONSTRAINT fk_hobby_recipe_categories_category
        FOREIGN KEY (category_id) REFERENCES recipe_categories (id) ON DELETE CASCADE,
    CONSTRAINT ck_hobby_recipe_categories_weight CHECK (weight > 0 AND weight <= 1)
);

CREATE INDEX idx_hobby_recipe_categories_category_id
    ON hobby_recipe_categories (category_id);

-- How well a recipe difficulty suits a customer skill level (managed by ADMIN).
CREATE TABLE skill_difficulty_fit (
    skill_level VARCHAR(20)   NOT NULL,
    difficulty  VARCHAR(20)   NOT NULL,
    fit         NUMERIC(3, 2) NOT NULL,
    CONSTRAINT pk_skill_difficulty_fit PRIMARY KEY (skill_level, difficulty),
    CONSTRAINT ck_skill_difficulty_fit_skill CHECK (skill_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT ck_skill_difficulty_fit_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT ck_skill_difficulty_fit_value CHECK (fit >= 0 AND fit <= 1)
);

INSERT INTO skill_difficulty_fit (skill_level, difficulty, fit) VALUES
    ('BEGINNER', 'EASY', 1.00),
    ('BEGINNER', 'MEDIUM', 0.50),
    ('BEGINNER', 'HARD', 0.10),
    ('INTERMEDIATE', 'EASY', 0.70),
    ('INTERMEDIATE', 'MEDIUM', 1.00),
    ('INTERMEDIATE', 'HARD', 0.60),
    ('ADVANCED', 'EASY', 0.40),
    ('ADVANCED', 'MEDIUM', 0.80),
    ('ADVANCED', 'HARD', 1.00);

-- Feature weights learned by logistic regression, with their offline evaluation.
CREATE TABLE recommendation_models (
    id                   UUID PRIMARY KEY,
    version              INT              NOT NULL,
    weight_content       DOUBLE PRECISION NOT NULL,
    weight_skill         DOUBLE PRECISION NOT NULL,
    weight_hobby         DOUBLE PRECISION NOT NULL,
    weight_collaborative DOUBLE PRECISION NOT NULL,
    weight_quality       DOUBLE PRECISION NOT NULL,
    weight_search        DOUBLE PRECISION NOT NULL,
    training_samples     INT              NOT NULL,
    positive_samples     INT              NOT NULL,
    test_customers       INT              NOT NULL,
    eval_k               INT              NOT NULL,
    baseline_hit_rate    DOUBLE PRECISION NOT NULL,
    baseline_mrr         DOUBLE PRECISION NOT NULL,
    baseline_coverage    DOUBLE PRECISION NOT NULL,
    default_hit_rate     DOUBLE PRECISION NOT NULL,
    default_mrr          DOUBLE PRECISION NOT NULL,
    default_coverage     DOUBLE PRECISION NOT NULL,
    trained_hit_rate     DOUBLE PRECISION NOT NULL,
    trained_mrr          DOUBLE PRECISION NOT NULL,
    trained_coverage     DOUBLE PRECISION NOT NULL,
    final_loss           DOUBLE PRECISION NOT NULL,
    is_active            BOOLEAN          NOT NULL DEFAULT FALSE,
    trained_at           TIMESTAMPTZ      NOT NULL,
    trained_by           UUID,
    activated_at         TIMESTAMPTZ,
    CONSTRAINT uk_recommendation_models_version UNIQUE (version)
);

-- At most one active model at a time.
CREATE UNIQUE INDEX ux_recommendation_models_active
    ON recommendation_models (is_active)
    WHERE is_active;
