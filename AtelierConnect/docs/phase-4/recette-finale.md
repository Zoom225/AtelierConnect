# Rapport de recette finale de phase 3

Recette du 1er novembre 2026, sur `phase-3/refactoring-frontend`, sans modification
de `master` ou `develop`. La PR #2 n'est pas fusionnée. Les huit fichiers IntelliJ
non suivis présents au début de cette recette sont préservés.

## Résultats effectivement obtenus

Dernière exécution locale complète : `20261101_123446_496`, rapports privés dans
`.local/validation/20261101_123446_496/`. Les étapes natives contrôlent leurs codes
de sortie ; aucun test n'a été désactivé pour obtenir le résultat.

| Élément | Résultat |
| --- | --- |
| Java 21 / wrapper `clean verify` | **179 tests**, 0 échec, 0 erreur, 0 ignoré ; compilation et JAR réussis |
| Concurrence/créneaux PostgreSQL | **Sept scénarios** : quatre paiement/invitation et trois création/pause/déplacement |
| Angular / Vitest | **75 tests** dans 18 fichiers, tous réussis |
| Build Angular production | Réussi |
| Installations npm | `npm ci` frontend et E2E exécutés ; pas de --force ou --legacy-peer-deps |
| Chromium | **Six tests réussis**, zéro ignoré, 49,1 s localement |
| Données après redémarrage PostgreSQL de test | Empreinte des réservations identique |
| Base applicative d'origine | Neuf tables identiques à la copie sauvegardée, par nombre et empreinte agrégée |
| Configuration | Java 21, Spring Boot 3.5.0, Angular 21 ; Node 24 compatible avec les engines ; ports 5441 / 8082 / 4200 conservés |

La dernière relance utilisait `-SkipInstall` après les installations réussies de la
première chaîne. Elle a réexécuté Maven, Vitest, build, serveurs, Chromium et contrôle
de persistance. Ce paramètre ne désactive aucun test. Les avertissements stderr
de la JVM/Node sont conservés dans les journaux et ne remplacent pas les codes de sortie.

Les parcours testent l'accueil, les connexions, le choix centre/salle, la création
publique, l'invitation privée, les paiements et remboursements simulés, les annulations,
le consentement persistant, la gestion des centres/salles, Swagger, les refus HTTP,
la langue persistante FR/EN, le mobile et une navigation clavier de connexion.
Les endpoints passent par la vraie API et une vraie base PostgreSQL dédiée.

Le serveur applicatif PostgreSQL reste sain sur 5441. Le serveur de test est séparé
sur 5549. Les processus Java et Angular lancés pour la recette sont arrêtés à la
sortie ; leurs URL ont été vérifiées durant l'exécution, pas laissées actives.
`ddl-auto=update` est inchangé ; aucun Flyway, DROP, réinitialisation de base ou volume.

## Corrections issues de cette recette

| Anomalie / risque | Correction | Preuve |
| --- | --- | --- |
| Deux créations pouvaient valider le même créneau avant insertion | Verrou pessimiste de salle, validation et insertion dans la même transaction | Huit créations concurrentes : un succès, sept refus |
| Déplacements concurrents vers une salle | Verrou d'atelier puis salle cible, nouvelle vérification du créneau | Deux déplacements concurrents : un succès |
| Mutations d'atelier sur état potentiellement périmé | Verrou et rafraîchissement avant modification/consentement/conversion/compteur | Tests services, paiements concurrents et parcours réels |
| Filtre administratif fondé sur un nom de centre non unique | Filtrage par identifiants de centre et d'atelier | JWT SITE avec deux centres homonymes : pas de fuite |
| Lecture du paiement d'autrui renvoyait 500 | Transaction de lecture englobant le contrôle de propriétaire | Refus 4xx, paiement et réservation inchangés |
| Liste de fermetures d'un centre renvoyait 500 | Graphes JPA chargeant le centre pour les DTO | Tests JWT de listes et restrictions GLOBAL/SITE |
| ABONNE sans centre pouvait provoquer un NPE | Refus métier explicite, comme à la création d'atelier | Test dédié de réservation |
| Booléen de consentement seul accepté | Date de consentement également exigée ; atelier complet refusé | Régression sans timestamp et tests existants scheduler/navigateur |
| Un créneau de jour traversant minuit pouvait passer la validation | Refus de la fin après minuit pour les horaires de jour | Test de création à 22:30 |
| Prix historique de salle présenté comme tarif d'un nouvel atelier | Affichage de la constante atelier à 90 EUR | Test du select avec un prix de salle à 60 EUR |
| Accueil débordant à 390 px | Grille réductible et titre adapté au conteneur, sans masquer le débordement | Assertion de largeur et capture Chromium |
| Ancien texte anglais « club » / illustration de terrain | Texte des centres collaboratifs et illustration de tables | Contrôle de vocabulaire FR/EN et captures |

Les premières chaînes ont échoué sur les vrais défauts de lecture protégée et de
mobile. Elles restent dans les rapports locaux. Les corrections précèdent la suite
complète verte ci-dessus ; les échecs n'ont pas été convertis en tests ignorés.

## Matrices et preuves

- [Règles métier, bornes et tests précis](matrice-regles-tests.md)
- [Autorisations et cinq tests avec vrais JWT](matrice-autorisations.md)
- [Inventaire et plan individuel des données historiques](historique-plan.md)
- [Trois captures publiques et provenance](preuves/README.md)
- [Matrice des onze thèmes EPHEC](README.md)

Code testé : `bd7edf0` ; [CI 41302973553](https://github.com/Zoom225/AtelierConnect/actions/runs/41302973553) :
les trois jobs backend, frontend et navigateur sont réussis, y compris l'export
des captures publiques. Les documents et captures ajoutés ensuite ne modifient
pas le code exécuté.

## Données historiques : décision encore attendue

Les quatre ateliers du 2 au 5 novembre 2026 sont futurs au moment de l'audit. Ils
durent 90 min, totalisent 60 EUR, comptent 0/1/2/4 participants confirmés et sept
paiements validés au total. L'un est COMPLET à quatre places. Les compteurs ne sont
pas faux ; les changer seuls détruirait la signification des données héritées.

Le plan propose une décision par atelier, la conservation des paiements de 105 EUR
validés, les nouveaux horaires à vérifier et un choix explicite avant de rouvrir
deux places sur #4. Les noms « Padel Club » et « Court » des référentiels sont des
données à renommer uniquement après approbation. Aucun de ces changements n'est exécuté.

## Contrôles manuels ou fonctionnalités non validés

| Contrôle | État réel |
| --- | --- |
| Accueil FR/EN, mobile 390 px, parcours Tab du login | Automatisés et captures inspectées par agent ; pas de recette humaine indépendante |
| Tous écrans et erreurs dans les deux langues | Recette humaine complète restante ; les messages métier du backend restent principalement français |
| Lecteur d'écran, autres résolutions, téléphone physique | Non effectués ; le test de largeur n'est pas une certification d'accessibilité |
| Tous parcours d'administration SITE dans le navigateur | Autorisations HTTP prouvées par JWT ; recette humaine de l'ergonomie restante |
| Déclenchement du scheduler à minuit | Traitement métier testé, attente réelle en environnement cible non effectuée |
| Impayés J−1 et dernier jour de suspension | Implémentation historique documentée ; arbitrage métier restant |
| Reprise des quatre ateliers et des sept noms hérités | Plan prêt, aucune autorisation d'exécution reçue |
| Paiement bancaire, notification, refresh/révocation JWT | Non implémentés ; paiement et remboursement seulement applicatifs simulés |
| Charge, restauration de production automatisée, déploiement continu | Non validés par cette recette ; sauvegarde/restauration locale de copie réalisée |

## Avis motivé sur l'intégration

**Phase 3 techniquement prête pour une revue finale, mais pas déclarée terminée
sans réserve ni à fusionner automatiquement.** Les tests et parcours obligatoires
automatisés sont conformes et les données sont préservées. Il reste une décision
explicite pour les séances héritées futures et leurs noms, ainsi qu'une acceptation
de la recette humaine restante et des limites documentées. Conserver la PR #2 en
brouillon jusqu'à cette revue ; attendre l'autorisation explicite avant toute fusion.

Le dossier des onze thèmes est une collection de preuves techniques et documentaires,
pas une validation officielle des acquis par l'EPHEC.
