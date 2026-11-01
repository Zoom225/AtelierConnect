# Refactoring AtelierConnect — rapport du 1er novembre 2026

## 1. Périmètre et fichiers

Travail exclusivement sur `phase-3/refactoring-frontend`, depuis `8c7be9d`.
Le dépôt était initialement sur `master` ; récupération des commits distants puis
création de la branche locale suivant la branche distante existante. Les cinq
fichiers IntelliJ non suivis de l'utilisateur ont été préservés.

L'[inventaire complet](refactoring-files-2026-11-01.md) liste les fichiers modifiés,
supprimés et renommés. Il couvre les services Java, entités, DTO, mappers,
repositories, contrôleurs, scheduler, autorisations, tests Java et Angular,
modèles, services HTTP, formulaires, routes, traductions et styles.

Le fichier anciennement indiqué `com/padell/padell/service/impl/MatchServiceImpl.java`
était déjà devenu `com/atelierconnect/service/impl/AtelierServiceImpl.java` sur la
branche distante. Ses propriétés et méthodes internes ont maintenant été alignées.

## 2. Noms supprimés du domaine actif

- `matchRepository`, `membreRepository`, `terrainService`, `siteRepository`, anciens
  noms des mappers, services, paramètres et variables : vocabulaire AtelierConnect.
- `createMatch`, `updateMatch`, `cancelMatch`, `findAllMatches`,
  `getMatchEntityById` : méthodes Atelier correspondantes, interfaces et appels actualisés.
- Relations Java : `atelier`, `participant`, `salle`, `centre` ; JPQL, graphes JPA,
  `mappedBy` et méthodes dérivées Spring Data suivent les nouvelles propriétés.
- `nbJoueursActuels`, `prixParJoueur`, `typeMatch` : propriétés métier canoniques.
- Modèles Angular `Atelier*`, `Participant*`, `Centre*`, `Salle*`, composants et
  fichiers correspondants ; usages des anciens services HTTP remplacés.
- Quatre réexportations HTTP historiques supprimées après vérification de l'absence
  de consommateurs dans le dépôt. Les alias REST restent disponibles.
- Classes CSS `padel-*` et `*-match-*`, marque résiduelle dans `app.html`, ancienne
  phrase annonçant quatre participants : harmonisées.
- Catégories natives Angular : `PARTENAIRE`, `ABONNE`, `VISITEUR` dans les formulaires,
  filtres et délais. Les rôles administrateurs restent `GLOBAL` et `SITE`.

## 3. Compatibilité volontaire

| Élément conservé | Justification |
| --- | --- |
| Tables `matches`, `membres`, `terrains`, `sites` | Données PostgreSQL existantes |
| FK `terrain_id`, `match_id`, `membre_id`, `site_id` | Relations SQL existantes |
| Colonnes `type_match`, `nb_joueurs_actuels`, `prix_par_joueur`, `type_membre`, `duree_match_minutes`, `duree_entre_match_minutes` | Mappings explicites pour empêcher la création de colonnes de remplacement par Hibernate |
| Valeurs SQL participantes `GLOBAL`, `SITE`, `LIBRE` | Convertisseur SQL et DTO historiques ; lecture également des valeurs natives |
| Anciennes routes REST et anciens champs JSON | Clients externes non inventoriés ; réponses doubles et alias de désérialisation |
| Anciennes routes Angular `matches`, `matches/new`, `members`, `sites`, `terrains` | Liens enregistrés ; protections conservées et testées |
| Clés de stockage `padel_*`, `padelplay_language` | Sessions et préférence de langue existantes ; lecture des anciens `siteId`, `siteNom`, `typeMembre` |
| Claims JWT et `ROLE_MEMBER`, variables `APP_DEMO_MEMBER_PASSWORD` | Contrats d'authentification et configuration de déploiement existants |
| Identifiants Jackson de références bidirectionnelles | Paires cohérentes conservées ; pas des propriétés métier ni des colonnes |
| Adresse historique `admin@padel.com` dans l'initialiseur | Compatibilité avec un compte préexistant, sans changement forcé de son mot de passe |

Les méthodes techniques Java `anyMatch`, `allMatch`, `String.matches` et
`PasswordEncoder.matches`, ainsi que les composants Angular Material `MatChips`,
ne désignent pas des matchs sportifs.

`spring.jpa.hibernate.ddl-auto=update` est inchangé. Aucun Flyway, aucune migration
SQL destructive, aucun volume supprimé, aucune base réinitialisée.

## 4. Règles métier et preuves

| Règle | Implémentation / validation |
| --- | --- |
| PARTENAIRE 30 jours et plusieurs centres | `AtelierRules`, services Atelier et Réservation, formulaires et tests |
| ABONNE 15 jours et centre de rattachement | Même chaîne ; contrôles de centre à la création et à l'inscription |
| VISITEUR 7 jours et centre ouvert aux visiteurs | Même chaîne ; refus testé si centre fermé aux visiteurs |
| Six places, 120 min, pause 20 min, 90 EUR / 15 EUR | Constantes backend/frontend ; validation des créneaux et tests |
| PUBLIC / PRIVE ; organisateur et cinq invités | Six réservations privées actives maximum sous verrou d'atelier, y compris celles en attente ; refus unitaire et course PostgreSQL testés |
| Place après paiement validé uniquement | Inscription EN_ATTENTE ; confirmation refuse un paiement absent ou non PAYE ; incrément sous verrou lors du paiement |
| Première validation de paiement attribue la place | Sérialisation PostgreSQL, rafraîchissement d'état sous verrou, tests de dernière place et double paiement |
| Dette ou suspension : refus d'inscription | Contrôles conservés à la création et à l'inscription |
| Ouverture automatique à J−2 | Atelier privé planifié, incomplet, consentement explicite daté ; tests du scheduler, absence d'accord, compteur historique nul ; parcours navigateur vérifiant la persistance de l'accord |

Le scheduler quotidien est testé par appel métier ; l'attente réelle jusqu'à minuit
en environnement de déploiement reste une vérification d'exploitation.

## 5. Vérifications réellement exécutées

| Vérification locale | Résultat |
| --- | --- |
| Maven `clean verify`, Java 21 | 149 tests, zéro échec, zéro erreur, zéro ignoré ; JAR construit |
| Concurrence PostgreSQL 15.19 | Quatre scénarios réussis : dernière place, double paiement, paiement/annulation, dernière invitation privée |
| Contrat des mappings SQL | Test de réflexion vérifiant noms physiques des tables, colonnes et FK renommées |
| `npm ci` | Exécuté avec le lockfile existant |
| Vitest `npm test -- --maxWorkers=2` | 73 tests dans 18 fichiers, tous réussis |
| `npm run build` | Build Angular de production réussi |
| Playwright / Chromium | Deux parcours réussis ; rapport HTML : total=2, expected=2, unexpected=0, skipped=0, ok=true |

Tests PostgreSQL exclusivement sur `atelierconnect_refactor_test` et
`atelierconnect_refactor_browser`, serveur de test sur `127.0.0.1:5549`, conteneur
`atelierconnect-refactor-test-20261101`. Aucun accès requis à la base applicative.
Les processus backend/frontend de vérification sont arrêtés après exécution.
Le conteneur de test est arrêté et conservé ; aucun volume n'est supprimé.

Le lanceur PowerShell de Playwright a renvoyé une erreur sur l'avertissement Node
`NO_COLOR` / `FORCE_COLOR`. Le résultat de test est confirmé séparément en ouvrant
le JSON intégré au rapport HTML et `.last-run.json` ; ce n'est pas un succès déduit
du code de sortie du lanceur. La CI Linux donne une vérification indépendante.

Échec antérieur identifié : workflow [41291955708](https://github.com/Zoom225/AtelierConnect/actions/runs/41291955708),
sur `8c7be9d`, assertion attendant « propre site » alors que le message était
« centre de rattachement ». Assertion corrigée ; suite locale complète réussie.
Les erreurs transitoires de compilation, de renommage et de restauration de session
rencontrées pendant ce travail ont été corrigées avant les validations ci-dessus.

CI finale du code `2b1c988` : [workflow 41294695133](https://github.com/Zoom225/AtelierConnect/actions/runs/41294695133),
**trois jobs réussis** (`backend-tests`, `frontend-compatibility`, `browser-integration`).
Les journaux ont été lus : 149 tests Java sans échec/erreur/ignoré, 73 tests Vitest,
build Angular réussi et deux tests Playwright réussis en 6,9 secondes. Le commit
documentaire suivant ne modifie aucun fichier exécutable.

## 6. Ambiguïtés et anomalies à arbitrer

- La règle existante à J−1 annule les réservations impayées et impute un montant à
  l'organisateur. La récupération d'une dette au paiement suivant existe également.
  Ces règles non définies dans la demande ont été conservées, sans arbitrage nouveau.
- La fin de suspension est testée par une date strictement postérieure à aujourd'hui.
  Le caractère inclusif du dernier jour n'est pas défini dans la demande.
- Les durées configurables des centres restent stockées pour compatibilité ; la
  logique d'atelier utilise les constantes 120/20. Leur rôle futur doit être précisé.
- Fuseau horaire de référence et traitement des ateliers traversant minuit à revoir
  avant généralisation à des centres ouverts la nuit ; aucun nouveau comportement décidé.
- Les anciennes catégories de matricules `G/S/L` restent valides : aucun nouveau
  format de matricule n'a été défini.
- Les paiements et remboursements restent des validations applicatives simulées,
  sans capture ni restitution bancaire réelle.

## 7. Commits et PR

- `f8ee2a8` — refactoring des identifiants du domaine backend, validé sur 145 tests.
- `00d3fca` — plafond d'invités sous verrou, confirmation après paiement,
  compatibilité SQL et tests complémentaires ; suite finale de 149 tests réussie.
- `0307842` — modèles, composants, catégories, formulaires et contrats Angular.
- `2b1c988` — derniers messages de diagnostic et commentaire frontend.
- Un commit documentaire complète le rapport et l'inventaire.

Commits poussés sur `phase-3/refactoring-frontend`. La
[PR #2](https://github.com/Zoom225/AtelierConnect/pull/2) reste en brouillon vers
`develop`. Aucune fusion ni modification directe de `master`/`develop`.

## 8. Risques PostgreSQL et sécurité

La compatibilité physique est testée, mais aucune copie de la base réelle n'a été
fournie pour comparer son schéma et ses données. `ddl-auto=update` peut effectuer
des évolutions additives au démarrage : vérifier sur une copie sauvegardée avant
déploiement. Les nouveaux noms Java ne constituent pas une migration SQL validée.

JWT, Spring Security, contrôles de propriété et rôles administratifs, transactions,
verrous PostgreSQL et rafraîchissement après verrou ont été conservés. Aucun secret
applicatif n'a été commité. La rotation d'éventuels anciens secrets présents dans
l'historique Git reste à confirmer par le propriétaire du déploiement.

## 9. Recette manuelle restante

Vérifier FR/EN, navigation clavier et mobile, erreurs de formulaire, création et
modification d'atelier par chaque catégorie, administration limitée à son centre,
fermetures, annulation/remboursement applicatif, refus avec dette/suspension,
restauration d'anciennes sessions, clients REST historiques et déclenchement réel
du scheduler à J−2. Refaire la recette sur une copie de données représentatives.

## 10. Phase 4 et acquis TFE

Le socle de phase 4 est préparé par le domaine cohérent et les preuves automatisées.
La CI est verte. La validation finale de phase 3 reste conditionnée à la recette ; la PR
reste donc en brouillon. Les ambiguïtés ci-dessus doivent entrer au backlog.

L'[audit TFE existant](phase-3-audit-tfe.md) recense onze pistes de preuve.
Ce tableau n'est pas une validation officielle des acquis EPHEC :

| Acquis documenté dans le dépôt | État de préparation |
| --- | --- |
| AA1 analyse du besoin | Règles tracées ; analyse TFE à formaliser |
| AA2 cahier des charges | Ambiguïtés identifiées ; CDC à faire valider |
| AA3 modèle conceptuel/relationnel | Entités cohérentes ; diagrammes à finaliser |
| AA4 base et contraintes | Mappings physiques et concurrence testés ; preuve sur copie représentative à ajouter |
| AA5 architecture en couches | Contrats services/implémentations et accès JPA validés par compilation/tests |
| AA6 authentification/autorisation | JWT et rôles conservés/testés ; matrice d'accès et rotation des secrets à documenter |
| AA7 API sécurisée | API canonique et alias conservés ; exemples et contrat de dépréciation à compléter |
| AA8 interface utilisateur | Angular compilé, 73 tests, deux parcours Chromium ; accessibilité et recette à compléter |
| AA9 règles et erreurs | Constantes, paiements, invitations et scheduler testés ; arbitrages métier listés |
| AA10 tests et CI | Preuves locales et trois jobs CI réussis ; liens et résultats ci-dessus |
| AA11 documentation/défense | Rapport et inventaire disponibles ; manuel, captures et scénario de soutenance à finaliser |
