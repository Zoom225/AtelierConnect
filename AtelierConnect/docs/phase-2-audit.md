# Phase 2 — audit du backend

## Dépendances avant renommage

| Domaine actuel | Dépendances directes | Données SQL conservées pendant la transition |
| --- | --- | --- |
| `Membre` | `TypeMembre`, réservations, pénalités, organisateur de match, authentification JWT, repositories, services, DTO, mappers, contrôleur et tests | `membres`, `type_membre`, `site_id`, `membre_id` |
| `Match` | `TypeMatch`, `StatutMatch`, terrain, organisateur, réservations, paiements, scheduler, repositories, services, DTO, mappers, contrôleur et tests | `matches`, `terrain_id`, `match_id` |
| `Terrain` | site, matchs, horaires, contrôle de chevauchement, repositories, services, DTO, mappers, contrôleur et tests | `terrains`, `site_id` |
| `Site` | terrains, membres, administrateurs, jours de fermeture, horaires, repositories, services, DTO, mappers, contrôleur et tests | `sites`, `site_id` |

La base `atelierconnect_db` contient déjà des membres, matchs, réservations et paiements. Un simple renommage Java ne doit donc pas modifier les noms des tables et colonnes : avec `ddl-auto=update`, Hibernate créerait de nouvelles colonnes ou tables au lieu de déplacer les données. Une migration physique éventuelle exige une sauvegarde vérifiée, un script SQL transactionnel et un contrôle du résultat avant le démarrage de la nouvelle version. Aucune réinitialisation de volume n'est prévue.

## Dépendances fonctionnelles à préserver

- Les réservations passent de `EN_ATTENTE` à `CONFIRMEE` seulement après paiement. Le paiement verrouille le match avec `PESSIMISTIC_WRITE` avant d'attribuer la place.
- Une annulation transforme un paiement payé en `REMBOURSE` et un paiement en attente en `ANNULE` ; seule une réservation confirmée libère une place.
- Les catégories actuelles `GLOBAL`, `SITE`, `LIBRE` sont persistées comme chaînes et doivent être converties de façon compatible vers `PARTENAIRE`, `ABONNE`, `VISITEUR`.
- Les routes `/api/membres`, `/api/matches`, `/api/terrains`, `/api/sites` et les clés JSON associées sont consommées par Angular 21 ; elles nécessitent une période de compatibilité documentée.
- Le scheduler actuel convertit les matchs privés incomplets à J-1 et pénalise automatiquement l'organisateur. Cette conversion doit cesser tant qu'un consentement explicite et traçable à J-2 n'est pas enregistré.

## Sécurité à vérifier pendant le refactoring

Les identifiants transmis dans les URL et les corps ne sont pas une preuve d'identité. Les contrôleurs doivent comparer les ressources avec le membre authentifié ou l'administrateur autorisé. La lecture des réservations d'un match et le détail d'un match privé nécessitent une vérification de portée.
