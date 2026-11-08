# Simplification du code du 8 novembre 2026

L'examen porte sur les sources Java, Angular et les scripts locaux. Une recherche
de blocs identiques a été complétée par la lecture des paiements, réservations,
dettes, autorisations, sessions, API et configurations de validation.

## Changements

- `StatusPresentationService` centralise les traductions des statuts d'atelier,
  de réservation et de paiement, ainsi que les badges communs. Six pages
  utilisent ce service. La traduction est calculée avec la langue courante ;
  les couleurs particulières des espaces existants sont conservées.
- `CentreResponse` réutilise les champs de `CentreRequest` et ajoute son ID,
  sans changer les objets JSON échangés avec Spring Boot.
- `PaiementServiceImpl.pay` distingue le contrôle du paiement en attente,
  le contrôle de l'atelier verrouillé et l'enregistrement du règlement.
  Le contrôle du propriétaire est partagé entre les deux lectures de paiement.
  Une dépendance inutilisée vers `ParticipantRepository` a été retirée.
- `ParticipantAccessService` partage sa détection du rôle administrateur.
- `test-environment-functions.ps1` regroupe l'attente de PostgreSQL et la
  génération de secrets éphémères pour les deux procédures de validation.
- `IsolatedPostgres` protège les tests de dette et de sanction avant la création
  du contexte Spring. Il accepte les bases locales de validation et le service
  dédié de GitHub Actions, avec des tests des refus de la base applicative.
  Les classes de test ne dépendent plus l'une de l'autre pour cette protection.
- Le workflow contrôle aussi les pushes `fix/**` et les changements de scripts.
  Les tests déclenchent explicitement les échéances, sans course avec le cron.

Les modifications de dette et de suspension présentes avant cette revue sont
conservées dans la livraison : six places, imputation unique, historique,
règlement autonome et maintien d'une suspension active après règlement.

## Choix conservés

Les modèles de persistance, requêtes et réponses Java restent distincts : leurs
annotations de validation, leur exposition JSON et leur rôle ne sont pas les
mêmes. Les imports Angular et les descriptions OpenAPI restent explicites.
La ressemblance de ces déclarations ne justifie pas une classe générique qui
couplerait les écrans ou les contrats d'API.

Les transactions, l'ordre des verrous et les rafraîchissements Hibernate des
réservations sont conservés. L'annulation conserve aussi son flush avant le
rafraîchissement de l'atelier pour préserver l'état annulé/remboursé.

## Validation reproductible

Depuis `AtelierConnect/`, les serveurs applicatifs doivent être arrêtés pour
libérer 8082 et 4200 avant la recette sur une base dédiée :

```powershell
.\scripts\validate-fullstack.ps1 -SkipInstall -TestContainer atelierconnect-penalty-validation-db -PostgresPort 5550
```

La première exécution sans dépendances installées se fait sans `-SkipInstall`.
Les journaux et le résumé sont sous `.local/validation/<date>/`. Les bases de
validation et leurs volumes sont conservés ; la base applicative sur 5441 n'est
jamais utilisée pour ces tests.

Les configurations IntelliJ, les sauvegardes et les secrets locaux ne font pas
partie de la publication Git.

## Résultats locaux

Recette `20261108_031523_735` : compilation Maven, compilation Angular,
236 tests backend, 96 tests Angular et dix scénarios Chromium réussis.
Les dix tests supplémentaires de `IsolatedPostgresTest` ont passé avec :

```powershell
.\mvnw.cmd -B -ntp -Dtest=IsolatedPostgresTest test
```

Le total des rapports Surefire est donc de 246 tests distincts, sans échec,
erreur ni test ignoré. La persistance des réservations, soldes et mouvements
de dette a aussi été vérifiée après redémarrage de PostgreSQL isolé.
