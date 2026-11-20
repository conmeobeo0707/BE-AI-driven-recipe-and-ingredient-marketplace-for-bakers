CREATE TABLE ingredient_catalog (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canonical_name VARCHAR(150) NOT NULL,
    normalized_name VARCHAR(150) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_ingredient_catalog_normalized_name
        UNIQUE (normalized_name),
    CONSTRAINT chk_ingredient_catalog_canonical_name_not_blank
        CHECK (length(trim(canonical_name)) > 0),
    CONSTRAINT chk_ingredient_catalog_normalized_name_not_blank
        CHECK (length(trim(normalized_name)) > 0),
    CONSTRAINT chk_ingredient_catalog_normalized_name_lowercase
        CHECK (normalized_name = lower(normalized_name))
);

CREATE TABLE ingredient_aliases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ingredient_id UUID NOT NULL,
    alias VARCHAR(150) NOT NULL,
    normalized_alias VARCHAR(150) NOT NULL,
    language_code VARCHAR(10),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ingredient_aliases_ingredient_catalog
        FOREIGN KEY (ingredient_id)
            REFERENCES ingredient_catalog (id)
            ON DELETE RESTRICT,
    CONSTRAINT uq_ingredient_aliases_normalized_alias
        UNIQUE (normalized_alias),
    CONSTRAINT chk_ingredient_aliases_alias_not_blank
        CHECK (length(trim(alias)) > 0),
    CONSTRAINT chk_ingredient_aliases_normalized_alias_not_blank
        CHECK (length(trim(normalized_alias)) > 0),
    CONSTRAINT chk_ingredient_aliases_normalized_alias_lowercase
        CHECK (normalized_alias = lower(normalized_alias))
);

CREATE INDEX idx_ingredient_aliases_ingredient_id
    ON ingredient_aliases (ingredient_id);

ALTER TABLE ingredients
    ADD COLUMN catalog_ingredient_id UUID;

ALTER TABLE ingredients
    ADD CONSTRAINT fk_ingredients_catalog_ingredient
        FOREIGN KEY (catalog_ingredient_id)
            REFERENCES ingredient_catalog (id)
            ON DELETE RESTRICT;

CREATE INDEX idx_ingredients_catalog_ingredient_id
    ON ingredients (catalog_ingredient_id);
