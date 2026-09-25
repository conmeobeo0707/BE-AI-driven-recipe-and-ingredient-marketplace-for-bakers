ALTER TABLE refresh_tokens
    ADD COLUMN replaced_by_id UUID;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_replacement
        FOREIGN KEY (replaced_by_id) REFERENCES refresh_tokens(refresh_token_id),
    ADD CONSTRAINT ck_refresh_tokens_replacement
        CHECK (replaced_by_id IS NULL OR
            (replaced_by_id <> refresh_token_id AND revoked_at IS NOT NULL));

CREATE INDEX ix_refresh_tokens_replaced_by_id ON refresh_tokens(replaced_by_id);
