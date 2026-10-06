-- V22__drop_all_tables_except_flyway_schema_history.sql
-- Drop all tables except flyway_schema_history

DO $$
DECLARE
    r RECORD;
BEGIN
    -- Drop all user tables in public schema except flyway history table
    FOR r IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
          AND tablename <> 'flyway_schema_history'
    LOOP
        EXECUTE format('DROP TABLE IF EXISTS public.%I CASCADE', r.tablename);
    END LOOP;
END $$;