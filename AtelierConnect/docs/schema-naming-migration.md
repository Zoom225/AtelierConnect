# Alignement des noms PostgreSQL avec les entités Java

La demande de renommage physique remplace la conservation provisoire des anciens
noms documentée pendant les phases 2 et 3. Les anciennes routes HTTP restent
compatibles ; les identifiants de connexion et les valeurs métier restent inchangés.

| Ancienne table | Nouvelle table |
| --- | --- |
| matches | ateliers |
| membres | participants |
| sites | centres |
| terrains | salles |

Quatorze colonnes sont renommées : les clés étrangères `terrain_id`, `match_id`,
`membre_id`, `site_id` deviennent respectivement `salle_id`, `atelier_id`,
`participant_id`, `centre_id` dans leurs tables respectives. Les colonnes de type,
nombre de participants, prix par participant et durées d'atelier sont également
alignées avec les champs Java. Quatre séquences et sept contraintes nommées sont
renommées. Les autres tables et contraintes restent en place.

## Migration d'une base existante

1. Arrêter le backend et vérifier que le port 8082 est libre. Ne pas recompiler
   une instance DevTools encore active pendant cette transition.
2. Créer une sauvegarde `pg_dump -Fc` et vérifier sa restauration sur une base
   isolée. Ne pas restaurer par-dessus la base applicative.
3. Depuis le répertoire `AtelierConnect`, appliquer le script :

   ```powershell
   .\scripts\set-local-environment.ps1
   Get-Content .\scripts\migrations\20261102_align_entity_names.up.sql -Raw |
       docker exec -i atelierconnect-db psql -X -v ON_ERROR_STOP=1 -U $env:POSTGRES_USER -d atelierconnect_db
   if ($LASTEXITCODE -ne 0) { throw 'Migration non appliquee : consulter la sortie SQL.' }
   ```

4. Compiler et démarrer le backend avec les nouveaux mappings. Conserver
   `spring.jpa.hibernate.ddl-auto=update`.
5. Vérifier Swagger, l'API publique et les connexions, puis actualiser le schéma
   de la source PostgreSQL dans IntelliJ avec **Synchronize / Refresh**.

Le script utilise une transaction, un délai maximal pour les verrous et des
renommages `ALTER ... RENAME`. Avant et après, il compare les empreintes de toutes
les lignes, les OID des tables, l'état des séquences et les identités/relations
des contraintes. Un écart ou une ambiguïté provoque l'annulation de la transaction.
Il ne supprime aucune table, colonne ou ligne et ne recopie pas les données.
La réexécution sur le schéma migré est acceptée.

`SchemaNamingGuard` refuse un schéma contenant les anciennes tables avant la mise
à jour Hibernate pour éviter la création silencieuse de tables vides parallèles.
Une base isolée neuve peut toujours être initialisée par Hibernate.

## Retour aux anciens noms

Arrêter le backend puis appliquer `20261102_align_entity_names.down.sql` de la
même manière, et utiliser ensuite une version du backend avec les anciens
mappings. Ce script effectue uniquement les renommages inverses, avec les mêmes
contrôles d'intégrité. Il ne faut pas lancer les nouveaux mappings sur l'ancien
schéma. Aucune restauration destructive n'est nécessaire pour ce retour de noms.

Les tests backend passent exclusivement par `scripts/test-backend-isolated.ps1`.
La répétition et le retour arrière de la migration sont vérifiés sur une copie
restaurée de la sauvegarde, séparée de la base utilisée par ces tests.

## Validation locale du 2 novembre 2026

La migration a été appliquée à `atelierconnect_db` après répétition sur une copie
restaurée. La sauvegarde locale est
`.local/backups/atelierconnect-before-naming-20261102-203538.dump` (ignorée par Git,
accès Windows restreint). Son SHA-256 est
`6CF21F36F49D742FD98D4DCF46331D1A643B1FB4CDE5B0634AE8C406B7AC8509`.

Les nombres de lignes et empreintes de toutes les lignes des neuf tables sont
identiques à la copie restaurée. Les neuf séquences et les 29 contraintes ont
été préservées. Il ne reste aucune ancienne table ni colonne répertoriée dans
la migration. Les 188 tests backend ont réussi sur une base isolée neuve.
Le démarrage sur la copie migrée puis sur la base applicative, Swagger, l'API
publique et les connexions des trois administrateurs et six participants ont
également été vérifiés. Le schéma de la source PostgreSQL IntelliJ a été actualisé.
