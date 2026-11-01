# Rapport de validation et limites techniques

## Périmètre et environnement

Travail sur `phase-3/refactoring-frontend`, après synchronisation Git sans écraser
les cinq fichiers IntelliJ non suivis. PR #2 en brouillon vers `develop`, sans fusion.
La branche phase 4 n'a pas été créée puisque la phase 3 n'est pas intégrée.

Le backend garde Java 21 / Spring Boot 3.5.0, port 8082, `ddl-auto=update`, sans
Flyway. Angular conserve le port 4200 et `/api` via proxy vers 8082. PostgreSQL
applicatif reste sur 5441. Aucune version ni port fonctionnel n'a été remplacé.

Les six variables privées étaient absentes du terminal de l'agent au début. Le
helper `set-local-environment.ps1` fournit maintenant un chemin de configuration
sans stockage ni affichage des secrets. La vraie instance applicative doit être
lancée avec ses secrets connus, pas avec les secrets aléatoires des tests.

## Résultats locaux exacts

Exécution réussie du script Windows : `20261101_113616_794`, sur le serveur dédié
`atelierconnect-refactor-test-20261101`, port 5549. Les rapports détaillés restent
dans `.local/validation/20261101_113616_794/`, ignorés par Git.

| Contrôle | Résultat |
| --- | --- |
| Wrapper Maven `clean verify` | 152 tests Java ; 0 échec, 0 erreur, 0 ignoré ; JAR produit |
| PostgreSQL concurrent | Quatre scénarios réussis : sixième place, double paiement, paiement/annulation, sixième réservation privée |
| Vitest | 74 tests dans 18 fichiers ; aucun échec |
| Angular production | Build réussi |
| Installation | npm ci frontend et E2E exécutés ; lockfile E2E ajouté, sans --force ni --legacy-peer-deps |
| Chromium réel | Quatre tests réussis, zéro ignoré ; durée observée 44,8 s |
| Persistance | Empreinte des réservations identique avant/après redémarrage du serveur PostgreSQL dédié |
| Wrapper `spring-boot:run` sur copie restaurée | Démarrage réel ; ateliers publics, centres, salles et Swagger : HTTP 200 |
| Base applicative d'origine | Neuf tables comparées par nombre et empreinte agrégée ; données inchangées après les essais |

La première exécution complète a réalisé les installations et révélé deux échecs
des nouveaux tests navigateur. Après correction, le script a été relancé avec
`-SkipInstall` ; aucun test n'a été ignoré. Les environnements et bases de test sont
indépendants de `atelierconnect_db`.

Le script arrête uniquement les processus backend/frontend qu'il a démarrés. Les
bases et volumes de preuve sont conservés ; le serveur PostgreSQL de test reste
disponible après son contrôle de redémarrage. La base applicative est restée saine.

## Couverture navigateur

1. Atelier privé : identité, invitation, contrôles de propriétaire, paiement,
   annulation/remboursement simulé, consentement daté et persistant après actualisation.
2. Administration globale : créer, modifier, supprimer un centre temporaire,
   contrôler sa présence puis son absence via la véritable API.
3. Formulaire Angular : créer un atelier public, payer et annuler depuis les
   boutons, recharger et vérifier les états persistés ; réservation visiteur et
   doublon refusé avec un message métier.
4. Salles : créer/modifier/supprimer via Angular ; contrôler HTTP 404, Swagger,
   login invalide 400, écriture anonyme 403 et préflight CORS 200.

Cela ne remplace pas une recette de toutes les combinaisons d'acteurs et de dates.
Les scénarios bancaires restent simulés.

## Erreurs rencontrées et causes corrigées

| Problème | Preuve avant correction | Correction / preuve après |
| --- | --- | --- |
| Préflight protégé refusé | Test CORS : attendu 200, reçu 403 | Activation CORS Spring Security ; test OPTIONS et écriture anonyme, puis navigateur réussis |
| Modification de salle : 500 | Trace `LazyInitializationException` et test HTTP hors transaction : attendu 200, reçu 500 | Transaction métier sur `SalleServiceImpl.update`, centre conservé chargé ; test réseau CRUD réussi |
| Consentement visuel appliqué à tous les ateliers | Comparaison d'un identifiant avec lui-même dans le map Angular | Variable distincte ; nouveau test vérifiant qu'un deuxième privé reste sans accord |
| Nouveau sélecteur E2E trop strict | L'option affichait le nom de salle et son tarif ; attente du nom seul | Sélecteur préfixe adapté au vrai libellé, sans retirer le test |
| Avertissement natif traité comme erreur fatale PowerShell | JVM/Node écrivent des avertissements sur stderr | Lanceur contrôlant le vrai code de sortie et conservant les journaux ; exécution complète et rapport JSON réussis |
| Dépendances E2E non verrouillées | Installation précédente avec npm install sans lockfile | Lockfile E2E et npm ci dans le lanceur et la CI |

## CI et commits

- `c3e3118` : correctifs CORS et transaction de modification de salle, régressions Java.
- `e01cc2e` : consentement Angular limité à l'atelier sélectionné, régression Vitest.
- `9a4c27e` : script Windows isolé, configuration privée, quatre parcours Chromium,
  rapports JSON, verrouillage des dépendances E2E et installation CI reproductible.

CI de ce code : [workflow 41298775712](https://github.com/Zoom225/AtelierConnect/actions/runs/41298775712).
Les trois jobs sont **réussis**. Leurs journaux ont été lus : 152 tests Java sans
échec/erreur/ignoré, 74 tests Vitest, build Angular réussi, quatre tests Playwright
réussis en 19,1 secondes sur Linux. Les commits documentaires ultérieurs ne changent
pas le comportement exécutable.

## Données historiques et exceptions de compatibilité

Audit en lecture seule de `atelierconnect_db` : quatre ateliers historiques, aucune
capacité supérieure à six ; un atelier COMPLET avec un compteur inférieur à six ;
quatre durées différentes de 120 minutes et quatre tarifs différents de 90/15.
Cinq salles ont également un prix historique différent de 90. Ces lignes n'ont pas
été réécrites. Les constantes d'un nouvel atelier restent 120/20/90/15/six places.

Les noms SQL, champs JSON/routes historiques, codes de conversion de catégories,
claims JWT, variables de configuration et clés de stockage compatibles sont
documentés dans le [rapport de refactoring](../refactoring-final-2026-11-01.md).
Un tarif stocké sur une salle ne remplace pas la règle de prix d'un nouvel atelier.
Ne pas présenter l'historique comme déjà migré au nouveau modèle métier.

## Limites et fonctionnalités encore manquantes

| Sujet | État réel / prochaine décision |
| --- | --- |
| Reprise de l'historique | Décider archivage ou migration métier des anciennes durées, prix et statuts ; sauvegarde et validation nécessaires |
| Impayés J−1 | Imputation à l'organisateur conservée ; règle à faire approuver dans le CDC |
| Suspension | Dernier jour inclus/exclu à arbitrer ; implémentation actuelle strictement après aujourd'hui |
| Horaires | Traversée de minuit, fuseau multi-pays et changements d'heure non couverts exhaustivement |
| Créations simultanées de créneaux | Tests actuels centrés sur paiements/invitations ; ne pas annoncer une exclusion SQL universelle des chevauchements |
| Déplacement de salle vers un autre centre | Non réalisé par le service update ; le nom seul est modifié |
| Prix et durées configurables | Champs historiques conservés, règles d'atelier fixes ; clarifier leur présentation et leur usage futur |
| Paiements | Simulés ; prestataire, capture, remboursement et rapprochement bancaire absents |
| Comptes | Pas d'inscription autonome, récupération de mot de passe/email ou gestion de notification complète |
| JWT | Pas de refresh/revocation centralisée ; stockage navigateur et clé privée à protéger |
| HTTP | 400/403/404 observés ; normalisation 401/409 non réalisée |
| Suppression | Cascades JPA existantes ; stratégie d'archivage métier à définir avant exploitation élargie |
| CORS production | Politique existante large ; liste d'origines à configurer lors du déploiement |
| Exploitation | Pas de déploiement continu, supervision/SLA, tests de charge ni restauration automatisée de production validés |
| Ergonomie | Recette manuelle FR/EN, clavier, lecteurs d'écran et mobile encore nécessaire |

## Recette avant intégration de phase 3

Vérifier un administrateur SITE dans et hors de son centre ; créer/réserver avec
chaque catégorie aux bornes 30/15/7 jours ; fermetures ; dette/suspension ; annulation
selon l'état ; consentement de plusieurs privés ; déclenchement réel du scheduler
à J−2 ; sessions historiques et erreurs FR/EN. Conserver les captures anonymisées,
le commit testé et les observations. Aucune validation officielle EPHEC n'en découle.

