-- Renomme les adresses historiques des trois comptes de demonstration uniquement.
-- Aucun mot de passe, matricule, role ou rattachement a un centre n'est modifie.
BEGIN;
SET LOCAL lock_timeout = '5s';

DO $$
BEGIN
    IF current_database() <> 'atelierconnect_db'
       AND current_database() !~ '^atelierconnect_test_[0-9]{8}_[0-9]{6}_[0-9]{3}$' THEN
        RAISE EXCEPTION 'Application or isolated AtelierConnect database expected';
    END IF;

    PERFORM id FROM administrateurs WHERE matricule IN ('ADMIN001','ADMIN002','ADMIN003') FOR UPDATE;
    IF (SELECT count(*) FROM administrateurs WHERE matricule IN ('ADMIN001','ADMIN002','ADMIN003')) <> 3 THEN
        RAISE EXCEPTION 'The three demonstration administrators must exist; no data changed';
    END IF;

    IF EXISTS (
        SELECT 1 FROM (VALUES
            ('ADMIN001','admin@padel.com','admin@atelierconnect.example'),
            ('ADMIN002','admin.lyon@padel.com','admin.bruxelles@atelierconnect.example'),
            ('ADMIN003','admin.paris@padel.com','admin.namur@atelierconnect.example')
        ) AS mapping(matricule,old_email,new_email)
        JOIN administrateurs cible ON cible.matricule=mapping.matricule
        JOIN administrateurs autre ON lower(autre.email)=mapping.new_email AND autre.id<>cible.id
        WHERE lower(cible.email)=mapping.old_email
    ) THEN
        RAISE EXCEPTION 'A target email is already in use; no data changed';
    END IF;
END $$;

UPDATE administrateurs a SET email=mapping.new_email,updated_at=LOCALTIMESTAMP
FROM (VALUES
    ('ADMIN001','admin@padel.com','admin@atelierconnect.example'),
    ('ADMIN002','admin.lyon@padel.com','admin.bruxelles@atelierconnect.example'),
    ('ADMIN003','admin.paris@padel.com','admin.namur@atelierconnect.example')
) AS mapping(matricule,old_email,new_email)
WHERE a.matricule=mapping.matricule AND lower(a.email)=mapping.old_email;

SELECT matricule,email FROM administrateurs
WHERE matricule IN ('ADMIN001','ADMIN002','ADMIN003') ORDER BY matricule;
COMMIT;
