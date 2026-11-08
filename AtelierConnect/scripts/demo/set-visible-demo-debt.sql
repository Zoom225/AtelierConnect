-- Application explicite d'un solde de demonstration : jamais lance au demarrage.
-- Six places impayees : 90 EUR. Le journal empeche une recreation apres reglement.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL TIME ZONE 'Europe/Paris';

DO $$
BEGIN
    IF current_database() <> 'atelierconnect_db'
       AND current_database() !~ '^atelierconnect_(test|browser)_[0-9]{8}_[0-9]{6}_[0-9]{3}$' THEN
        RAISE EXCEPTION 'Base AtelierConnect applicative ou de validation attendue';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM participants
                   WHERE matricule = 'L10002' AND email = 'demo.penalise@gmail.com') THEN
        RAISE EXCEPTION 'Compte de demonstration L10002 attendu : aucune modification';
    END IF;
END $$;

WITH cible AS (
    SELECT id FROM participants WHERE matricule='L10002'
      AND email='demo.penalise@gmail.com' AND solde=0.0 FOR UPDATE
), journal AS (
    INSERT INTO mouvements_dette (participant_id,nature,montant,reference_source,description,date_operation)
    SELECT id,'SOLDE_INITIAL',90.0,'solde-demo:' || id,
           'Solde de demonstration : six places impayees',LOCALTIMESTAMP FROM cible
    ON CONFLICT (reference_source) DO NOTHING RETURNING participant_id
)
UPDATE participants p SET solde=90.0 FROM journal j WHERE p.id=j.participant_id;

SELECT matricule, solde FROM participants WHERE matricule = 'L10002';
COMMIT;
