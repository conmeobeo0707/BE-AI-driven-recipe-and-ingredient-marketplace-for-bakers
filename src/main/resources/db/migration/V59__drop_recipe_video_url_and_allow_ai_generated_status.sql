DO $$
DECLARE
    moved BIGINT;
    status_check RECORD;
BEGIN
    IF to_regclass('recipe') IS NULL THEN
        RETURN;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'recipe' AND column_name = 'video_url') THEN
        IF to_regclass('media') IS NOT NULL THEN
            CREATE TEMP TABLE v59_orphan_videos ON COMMIT DROP AS
            SELECT r.id AS recipe_id, gen_random_uuid() AS media_id, r.video_url
            FROM recipe r
            WHERE r.video_id IS NULL AND NULLIF(BTRIM(r.video_url), '') IS NOT NULL;

            INSERT INTO media (id, status, type, url, version)
            SELECT media_id, 'ATTACHED', 'VIDEO', video_url, 0 FROM v59_orphan_videos;

            UPDATE recipe r
            SET video_id = o.media_id
            FROM v59_orphan_videos o
            WHERE r.id = o.recipe_id;
            GET DIAGNOSTICS moved = ROW_COUNT;
            IF moved > 0 THEN
                RAISE NOTICE 'V59: moved % recipe.video_url values without video_id into media', moved;
            END IF;
        END IF;

        ALTER TABLE recipe DROP COLUMN video_url;
    END IF;

    FOR status_check IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
        WHERE c.conrelid = 'recipe'::regclass AND c.contype = 'c' AND a.attname = 'status'
    LOOP
        EXECUTE format('ALTER TABLE recipe DROP CONSTRAINT %I', status_check.conname);
    END LOOP;
END $$;
