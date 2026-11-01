# AtelierConnect — Audit de la phase 3 et preuves pour le TFE

Actualisation du 1er novembre 2026 : voir le [rapport de refactoring](refactoring-final-2026-11-01.md)
et l'[inventaire des fichiers](refactoring-files-2026-11-01.md). Les nombres et résultats
de tests cités plus bas décrivent les versions historiques indiquées.

**Branche de travail :** `phase-3/refactoring-frontend`  
**PR de revue :** https://github.com/Zoom225/AtelierConnect/pull/2 (brouillon vers `develop`)  
**Contrainte validée :** conserver `spring.jpa.hibernate.ddl-auto=update`, sans Flyway.

## Travaux vérifiés dans le code

- Backend Java 21 / Spring Boot : `com.atelierconnect`, entités `Participant`, `Atelier`, `Salle`, `Centre`, `Reservation`, `Paiement`, services et repositories.
- API `/api/ateliers`, `/api/participants`, `/api/salles`, `/api/centres`. Les anciens noms JSON/routes restent partiellement disponibles pendant la transition Angular.
- Frontend Angular 21 : services `AteliersApiService`, `ParticipantsApiService`, `SallesApiService`, `CentresApiService`, écrans participant/administrateur, traductions FR/EN.
- Règles : 3 catégories (30/15/7 jours), 6 places, séance de 120 min, pause de 20 min, 90 € au total, consentement à l'ouverture publique.
- Sécurité : JWT, Spring Security, contrôles d'accès côté backend. Les contrôles Angular ne sont pas la source des autorisations.
- Tests : Maven/PostgreSQL, Vitest, Playwright sur un vrai navigateur Chromium avec backend et base temporaire dans GitHub Actions.
- Paiements : **validation applicative simulée**, pas de capture bancaire ni de remboursement financier réel.

## Correspondance avec les 11 acquis d'apprentissage à démontrer

Ce tableau identifie des **pistes de preuve**, pas une validation officielle par l'EPHEC.

| Acquis | Preuve technique ou documentaire à présenter | À finaliser |
| --- | --- | --- |
| AA1 — Analyse du besoin | Définition des acteurs, trois catégories, centres, ateliers et processus de réservation | Rédiger analyse AtelierConnect propre au nouveau domaine |
| AA2 — Cahier des charges | `docs/phase-2-audit.md`, `docs/phase-3-frontend-transition.md` | Produire le CDC TFE AtelierConnect complet, validé et traçable |
| AA3 — Modèle conceptuel puis relationnel | Entités JPA `Participant`, `Centre`, `Salle`, `Atelier`, `Reservation`, `Paiement` | Ajouter diagramme entité-association et passage au relationnel |
| AA4 — Base de données et contraintes | PostgreSQL, associations JPA et colonnes SQL conservées, `ddl-auto=update` | Documenter PK/FK, cardinalités, unicité et scénarios d'intégrité |
| AA5 — Architecture trois couches | Packages `controller/`, `service/`, `repository/` (DTO et Mapper complémentaires) | Schéma d'architecture et exemples de parcours complets |
| AA6 — Authentification/autorisation | `SecurityConfig`, `JwtAuthenticationFilter`, `JwtConfig`, contrôles de rôles | Expliquer acteurs, JWT, secrets et cas 401/403 |
| AA7 — API sécurisée | Contrôleurs REST, Swagger, `AdminAuthorizationService`, `CurrentParticipantService` | Matrice routes/rôles, exemples requêtes/réponses et contrôle des accès |
| AA8 — Interface utilisateur | Pages Angular participant/admin, formulaires réactifs, guards, services API, langues FR/EN | Vérifications manuelles des parcours, ergonomie et accessibilité |
| AA9 — Règles métier et erreurs | `AtelierRules`, `AtelierServiceImpl`, `ReservationServiceImpl`, `PaiementServiceImpl`, `GlobalExceptionHandler` | Matrice règles → méthodes → tests ; messages français compréhensibles |
| AA10 — Tests, qualité et intégration continue | Tests JUnit/PostgreSQL, Vitest, Playwright et `.github/workflows/atelierconnect-backend.yml` | Documenter tests réussis, captures, exclusions et couverture des cas limites |
| AA11 — Documentation et défense | README, audit technique, transitions API et rapport CI | Rédiger manuel utilisateur, guide installation, bilan et démonstration orale |

## Validation automatisée existante

Au commit `4a2247a` (31 octobre 2026), le workflow
https://github.com/Zoom225/AtelierConnect/actions/runs/41204179194
affiche **trois jobs réussis** : backend-tests, frontend-compatibility et browser-integration.

Le document `docs/phase-3-frontend-transition.md` indique pour cette version 143 tests Java, 70 tests Angular et un parcours Playwright. Les modifications apportées **après** ce commit nécessitent leur propre résultat CI avant validation.

Un nouveau scénario Playwright vérifie l'authentification administrateur et la création / modification / suppression d'un centre, avec contrôle via la véritable API PostgreSQL. **Ne pas le considérer réussi avant le verdict CI correspondant.**

## Risques et étapes avant fusion

1. Vérifier le nouveau test du parcours administrateur sur le workflow associé au dernier commit, puis corriger toute régression.
2. Vérifier la conversion effective d'un atelier privé à J−2 par le scheduler, avec consentement et sans accord.
3. Réaliser la revue manuelle FR/EN, clavier, erreurs de formulaires, écrans d'administration et sécurité des autorisations.
4. Préparer des chemins métier et noms internes canoniques sans supprimer brutalement les alias consommés par les clients existants.
5. Confirmer la rotation de toute clé JWT précédemment publiée ; la modification du fichier actuel n'efface pas les secrets de l'historique Git.
6. Vérifier que les paiements sont clairement présentés comme **simulés** aux utilisateurs et au jury.

**Décision :** PR #2 reste en brouillon ; aucune fusion vers `develop` ou `master` sans validation finale.
