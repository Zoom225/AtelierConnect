# Entités AtelierConnect et compatibilité SQL

Le modèle Java utilise `Centre`, `Salle`, `Atelier` et `Participant`. Les
références Jackson suivent le même vocabulaire : `centre-salles`,
`centre-joursFermeture`, `salle-ateliers`, `atelier-reservations`.

Les noms physiques sont maintenant alignés avec le modèle Java :

| Entité Java | Table SQL actuelle |
| --- | --- |
| Centre | centres |
| Salle | salles |
| Atelier | ateliers |
| Participant | participants |

Les colonnes associées utilisent `centre_id`, `salle_id`, `atelier_id`,
`participant_id`, `type_atelier`, `type_participant`, `nb_participants_actuels`,
`prix_par_participant`, `duree_atelier_minutes` et `duree_entre_atelier_minutes`.
La migration explicite est décrite dans [schema-naming-migration.md](schema-naming-migration.md).
`ddl-auto=update` est conservé ; il ne remplace pas cette migration.
`TypeParticipantConverter` préserve les codes SQL GLOBAL/SITE/LIBRE tout en
exposant PARTENAIRE/ABONNE/VISITEUR dans le modèle métier.

Les collections `Salle.ateliers` et `Atelier.reservations` sont initialisées,
y compris avec les builders Lombok. Les hashes des mots de passe des
participants et administrateurs sont exclus de la sérialisation JSON.
Les contrôleurs continuent d'utiliser leurs DTO ; les routes de compatibilité
restent disponibles.

`DataInitializer` utilise les noms Bruxelles/Namur et des variables de salles.
La recherche des anciennes salles de démonstration conserve les noms `Court`
pour retrouver les données déjà présentes, sans les renommer en base.

Les tests `PhysicalSchemaNamingTest`, `TypeParticipantConverterTest` et
`EntitySerializationTest` vérifient ces garanties. Les tests d'intégration
doivent être exécutés via `scripts/test-backend-isolated.ps1`, jamais sur la
base applicative.
