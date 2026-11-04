-- Explicit, reversible label migration for the existing Lyon/Paris demo data.
-- Does not rename tables/columns, change identifiers, or initialize demo data.
BEGIN;
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '60s';

DO $migration$
DECLARE
    mappings jsonb := '[{"table_name":"centres","id":1,"centre_id":null,"source_name":"Padel Club Lyon","target_name":"Centre Lyon"},{"table_name":"centres","id":2,"centre_id":null,"source_name":"Padel Club Paris","target_name":"Centre Paris"},{"table_name":"salles","id":1,"centre_id":1,"source_name":"Court A","target_name":"Salle A"},{"table_name":"salles","id":2,"centre_id":1,"source_name":"Court B","target_name":"Salle B"},{"table_name":"salles","id":3,"centre_id":1,"source_name":"Court C","target_name":"Salle C"},{"table_name":"salles","id":4,"centre_id":2,"source_name":"Court 1","target_name":"Salle 1"},{"table_name":"salles","id":5,"centre_id":2,"source_name":"Court 2","target_name":"Salle 2"}]'::jsonb;
    item record;
    table_name text;
    current_name text;
    current_centre_id bigint;
    row_exists boolean;
    collision boolean;
    phase integer;
    row_count bigint;
    digest text;
    snapshot jsonb;
    before_snapshot jsonb;
    before_sequences jsonb;
BEGIN
    -- Freeze writes while allowing reads. Any error rolls back the transaction.
    FOR table_name IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename LOOP
        EXECUTE format('LOCK TABLE public.%I IN SHARE ROW EXCLUSIVE MODE', table_name);
    END LOOP;

    FOR item IN SELECT * FROM jsonb_to_recordset(mappings)
        AS m(table_name text, id bigint, centre_id bigint, source_name text, target_name text)
    LOOP
        current_name := NULL;
        EXECUTE format('SELECT nom FROM public.%I WHERE id = $1', item.table_name)
            INTO current_name USING item.id;
        row_exists := current_name IS NOT NULL;
        IF NOT row_exists OR current_name NOT IN (item.source_name, item.target_name) THEN
            RAISE EXCEPTION 'Unexpected label for %. ID %; no changes committed.', item.table_name, item.id;
        END IF;
        IF item.table_name = 'salles' THEN
            EXECUTE 'SELECT centre_id FROM public.salles WHERE id = $1' INTO current_centre_id USING item.id;
            IF current_centre_id IS DISTINCT FROM item.centre_id THEN
                RAISE EXCEPTION 'Unexpected centre for room %. No changes committed.', item.id;
            END IF;
        END IF;
        EXECUTE format('SELECT EXISTS (SELECT 1 FROM public.%I WHERE nom = $1 AND id <> $2)', item.table_name)
            INTO collision USING item.target_name, item.id;
        IF collision THEN
            RAISE EXCEPTION 'Target label already used in %. No changes committed.', item.table_name;
        END IF;
    END LOOP;

    SELECT COALESCE(jsonb_agg(to_jsonb(s) ORDER BY sequencename), '[]'::jsonb)
        INTO before_sequences FROM pg_sequences s WHERE schemaname = 'public';

    FOR phase IN 1..2 LOOP
        IF phase = 2 THEN
            FOR item IN SELECT * FROM jsonb_to_recordset(mappings)
                AS m(table_name text, id bigint, centre_id bigint, source_name text, target_name text)
            LOOP
                EXECUTE format('UPDATE public.%I SET nom = $1 WHERE id = $2 AND nom = $3', item.table_name)
                    USING item.target_name, item.id, item.source_name;
            END LOOP;
        END IF;

        snapshot := '{}'::jsonb;
        FOR table_name IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename LOOP
            -- Only the seven authorized name fields are excluded from the digest.
            IF table_name = 'centres' THEN
                EXECUTE 'SELECT count(*), md5(COALESCE(string_agg((CASE WHEN id IN (1,2) THEN to_jsonb(r) - ''nom'' ELSE to_jsonb(r) END)::text, '''' ORDER BY id), '''')) FROM public.centres r'
                    INTO row_count, digest;
            ELSIF table_name = 'salles' THEN
                EXECUTE 'SELECT count(*), md5(COALESCE(string_agg((CASE WHEN id IN (1,2,3,4,5) THEN to_jsonb(r) - ''nom'' ELSE to_jsonb(r) END)::text, '''' ORDER BY id), '''')) FROM public.salles r'
                    INTO row_count, digest;
            ELSE
                EXECUTE format('SELECT count(*), md5(COALESCE(string_agg(to_jsonb(r)::text, '''' ORDER BY id), '''')) FROM public.%I r', table_name)
                    INTO row_count, digest;
            END IF;
            snapshot := snapshot || jsonb_build_object(table_name, jsonb_build_object('count', row_count, 'digest', digest));
        END LOOP;
        IF phase = 1 THEN
            before_snapshot := snapshot;
        ELSIF snapshot IS DISTINCT FROM before_snapshot THEN
            RAISE EXCEPTION 'Data outside the seven authorized labels changed. Rolling back.';
        END IF;
    END LOOP;

    IF before_sequences IS DISTINCT FROM
        (SELECT COALESCE(jsonb_agg(to_jsonb(s) ORDER BY sequencename), '[]'::jsonb)
         FROM pg_sequences s WHERE schemaname = 'public') THEN
        RAISE EXCEPTION 'Sequence state changed. Rolling back.';
    END IF;

    FOR item IN SELECT * FROM jsonb_to_recordset(mappings)
        AS m(table_name text, id bigint, centre_id bigint, source_name text, target_name text)
    LOOP
        EXECUTE format('SELECT nom FROM public.%I WHERE id = $1', item.table_name) INTO current_name USING item.id;
        IF current_name IS DISTINCT FROM item.target_name THEN
            RAISE EXCEPTION 'Target label verification failed. Rolling back.';
        END IF;
    END LOOP;
    RAISE NOTICE 'Labels verified: 2 centres, 5 rooms. All other data and sequences preserved.';
END
$migration$;
COMMIT;
