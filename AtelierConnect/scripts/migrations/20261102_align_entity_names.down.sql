-- Explicit rollback of physical names; no application data is rewritten.
-- Stop Spring Boot and verify a pg_dump backup before applying with psql -v ON_ERROR_STOP=1.
BEGIN;
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '120s';
SELECT pg_advisory_xact_lock(hashtext('atelierconnect-physical-naming'));

CREATE TEMP TABLE naming_rows (relation_oid oid PRIMARY KEY, row_count bigint, checksum text) ON COMMIT DROP;
CREATE TEMP TABLE naming_sequences (relation_oid oid PRIMARY KEY, last_value bigint, is_called boolean) ON COMMIT DROP;

DO $audit$
DECLARE r record; relation oid; n bigint; digest text; v bigint; called boolean;
BEGIN
    FOR r IN SELECT * FROM (VALUES
        ('ateliers', 'matches'),
        ('participants', 'membres'),
        ('centres', 'sites'),
        ('salles', 'terrains'),
        ('administrateurs', 'administrateurs'),
        ('jours_fermeture', 'jours_fermeture'),
        ('reservations', 'reservations'),
        ('paiements', 'paiements'),
        ('penalites', 'penalites')
    ) AS names(old_name, new_name) LOOP
        IF r.old_name <> r.new_name AND to_regclass(format('public.%I', r.old_name)) IS NOT NULL
           AND to_regclass(format('public.%I', r.new_name)) IS NOT NULL THEN
            RAISE EXCEPTION 'Both tables exist: % and %. Manual review required.', r.old_name, r.new_name;
        END IF;
        relation := coalesce(to_regclass(format('public.%I', r.old_name)), to_regclass(format('public.%I', r.new_name)));
        IF relation IS NULL THEN RAISE EXCEPTION 'Missing expected table: % / %', r.old_name, r.new_name; END IF;
        EXECUTE format('LOCK TABLE %s IN ACCESS EXCLUSIVE MODE', relation::regclass);
        EXECUTE format('SELECT count(*), md5(coalesce(string_agg(md5(ROW(t.*)::text), '''' ORDER BY t.id), '''')) FROM %s t', relation::regclass) INTO n, digest;
        INSERT INTO naming_rows VALUES (relation, n, digest);
    END LOOP;
    FOR r IN SELECT DISTINCT s.oid FROM pg_class s JOIN pg_depend d ON d.objid=s.oid AND d.classid='pg_class'::regclass
             WHERE s.relkind='S' AND d.refobjid IN (SELECT relation_oid FROM naming_rows) AND d.deptype IN ('a','i') LOOP
        EXECUTE format('SELECT last_value, is_called FROM %s', r.oid::regclass) INTO v, called;
        INSERT INTO naming_sequences VALUES (r.oid, v, called);
    END LOOP;
END $audit$;

CREATE TEMP TABLE naming_constraints ON COMMIT DROP AS
SELECT oid, conrelid, confrelid, contype, conkey, confkey, convalidated
FROM pg_constraint WHERE conrelid IN (SELECT relation_oid FROM naming_rows);

DO $rename$
DECLARE r record; old_exists boolean; new_exists boolean;
BEGIN
    FOR r IN SELECT * FROM (VALUES
        ('ateliers', 'matches'),
        ('participants', 'membres'),
        ('centres', 'sites'),
        ('salles', 'terrains')
    ) AS names(old_name, new_name) LOOP
        IF to_regclass(format('public.%I', r.old_name)) IS NOT NULL THEN
            EXECUTE format('ALTER TABLE public.%I RENAME TO %I', r.old_name, r.new_name);
        END IF;
    END LOOP;
    FOR r IN SELECT * FROM (VALUES
        ('matches', 'salle_id', 'terrain_id'),
        ('matches', 'type_atelier', 'type_match'),
        ('matches', 'nb_participants_actuels', 'nb_joueurs_actuels'),
        ('matches', 'prix_par_participant', 'prix_par_joueur'),
        ('membres', 'type_participant', 'type_membre'),
        ('membres', 'centre_id', 'site_id'),
        ('sites', 'duree_atelier_minutes', 'duree_match_minutes'),
        ('sites', 'duree_entre_atelier_minutes', 'duree_entre_match_minutes'),
        ('terrains', 'centre_id', 'site_id'),
        ('administrateurs', 'centre_id', 'site_id'),
        ('jours_fermeture', 'centre_id', 'site_id'),
        ('reservations', 'atelier_id', 'match_id'),
        ('reservations', 'participant_id', 'membre_id'),
        ('penalites', 'participant_id', 'membre_id')
    ) AS names(table_name, old_name, new_name) LOOP
        SELECT EXISTS(SELECT 1 FROM pg_attribute WHERE attrelid=to_regclass(format('public.%I',r.table_name)) AND attname=r.old_name AND NOT attisdropped),
               EXISTS(SELECT 1 FROM pg_attribute WHERE attrelid=to_regclass(format('public.%I',r.table_name)) AND attname=r.new_name AND NOT attisdropped)
        INTO old_exists, new_exists;
        IF old_exists AND NOT new_exists THEN
            EXECUTE format('ALTER TABLE public.%I RENAME COLUMN %I TO %I', r.table_name, r.old_name, r.new_name);
        ELSIF old_exists OR NOT new_exists THEN
            RAISE EXCEPTION 'Ambiguous or missing columns: %.% / %', r.table_name, r.old_name, r.new_name;
        END IF;
    END LOOP;
    FOR r IN SELECT * FROM (VALUES
        ('ateliers_id_seq', 'matches_id_seq'),
        ('participants_id_seq', 'membres_id_seq'),
        ('centres_id_seq', 'sites_id_seq'),
        ('salles_id_seq', 'terrains_id_seq')
    ) AS names(old_name, new_name) LOOP
        IF to_regclass(format('public.%I', r.old_name)) IS NOT NULL THEN
            IF to_regclass(format('public.%I', r.new_name)) IS NOT NULL THEN
                RAISE EXCEPTION 'Both sequences exist: % / %', r.old_name, r.new_name;
            END IF;
            EXECUTE format('ALTER SEQUENCE public.%I RENAME TO %I', r.old_name, r.new_name);
        END IF;
    END LOOP;
    FOR r IN SELECT * FROM (VALUES
        ('matches', 'ateliers_pkey', 'matches_pkey'),
        ('membres', 'participants_pkey', 'membres_pkey'),
        ('sites', 'centres_pkey', 'sites_pkey'),
        ('terrains', 'salles_pkey', 'terrains_pkey'),
        ('matches', 'ateliers_statut_check', 'matches_statut_check'),
        ('matches', 'ateliers_type_atelier_check', 'matches_type_match_check'),
        ('membres', 'participants_type_participant_check', 'membres_type_membre_check')
    ) AS names(table_name, old_name, new_name) LOOP
        IF EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid=to_regclass(format('public.%I',r.table_name)) AND conname=r.old_name) THEN
            IF EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid=to_regclass(format('public.%I',r.table_name)) AND conname=r.new_name) THEN
                RAISE EXCEPTION 'Both constraints exist: % / %', r.old_name, r.new_name;
            END IF;
            EXECUTE format('ALTER TABLE public.%I RENAME CONSTRAINT %I TO %I', r.table_name, r.old_name, r.new_name);
        END IF;
    END LOOP;
END $rename$;

DO $verify$
DECLARE r record; n bigint; digest text; v bigint; called boolean;
BEGIN
    FOR r IN SELECT * FROM naming_rows LOOP
        EXECUTE format('SELECT count(*), md5(coalesce(string_agg(md5(ROW(t.*)::text), '''' ORDER BY t.id), '''')) FROM %s t', r.relation_oid::regclass) INTO n, digest;
        IF n <> r.row_count OR digest <> r.checksum THEN
            RAISE EXCEPTION 'Data changed for relation OID %; migration rolled back.', r.relation_oid;
        END IF;
    END LOOP;
    FOR r IN SELECT * FROM naming_sequences LOOP
        EXECUTE format('SELECT last_value, is_called FROM %s', r.relation_oid::regclass) INTO v, called;
        IF v <> r.last_value OR called <> r.is_called THEN
            RAISE EXCEPTION 'Sequence state changed; migration rolled back.';
        END IF;
    END LOOP;
    IF EXISTS (
        (SELECT oid, conrelid, confrelid, contype, conkey, confkey, convalidated FROM pg_constraint WHERE conrelid IN (SELECT relation_oid FROM naming_rows)
         EXCEPT SELECT * FROM naming_constraints)
        UNION ALL
        (SELECT * FROM naming_constraints EXCEPT
         SELECT oid, conrelid, confrelid, contype, conkey, confkey, convalidated FROM pg_constraint WHERE conrelid IN (SELECT relation_oid FROM naming_rows))
    ) THEN RAISE EXCEPTION 'Constraint identity or relationships changed; migration rolled back.'; END IF;
    RAISE NOTICE 'Verified: % tables unchanged, % sequences unchanged, constraints and relationships preserved.',
        (SELECT count(*) FROM naming_rows), (SELECT count(*) FROM naming_sequences);
END $verify$;
COMMIT;
