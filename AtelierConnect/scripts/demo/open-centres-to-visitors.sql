-- Explicit demonstration configuration; never executed automatically at startup.
-- Only visitor access changes. Existing bookings, penalties and balances stay intact.
BEGIN;
SET LOCAL lock_timeout = '5s';
DO $check$
BEGIN
    IF current_database() <> 'atelierconnect_db' THEN
        RAISE EXCEPTION 'This demonstration script is restricted to atelierconnect_db.';
    END IF;
    IF (SELECT count(*) FROM public.centres WHERE id IN (1, 2)) <> 2 THEN
        RAISE EXCEPTION 'Expected demonstration centres 1 and 2; review the configuration first.';
    END IF;
END $check$;
UPDATE public.centres
SET ouvert_visiteurs = TRUE
WHERE id IN (1, 2) AND ouvert_visiteurs IS DISTINCT FROM TRUE;
SELECT id, nom, ouvert_visiteurs FROM public.centres WHERE id IN (1, 2) ORDER BY id;
COMMIT;
