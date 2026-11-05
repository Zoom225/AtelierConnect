# AtelierConnect — Réservation d’ateliers créatifs

Gestion et réservation d'ateliers collaboratifs multi-centres : **Java 21, Spring Boot 3.5, Spring Security/JWT, PostgreSQL 15 et Angular 21**.


Ce fichier est le **README unique du dépôt** : démarrage, compilation, tests,
connexions de démonstration et dépannage. Le backend est sous `AtelierConnect/`
et le frontend sous `AtelierConnect/frontend/`.

Dépôt : https://github.com/Zoom225/AtelierConnect
Branche de travail : `fix/5-nomenclature-espace-participant`, basée sur `feat/3-interface-readme-tracabilite`.
Suivi courant : [issue #5 — nomenclature et espace participant](https://github.com/Zoom225/AtelierConnect/issues/5).
Refonte et documentation initiales : [issue #3](https://github.com/Zoom225/AtelierConnect/issues/3).

## Sommaire

- [Prérequis et configuration](#prérequis-et-configuration)
- [Démarrer PostgreSQL, le backend et le frontend](#démarrer-en-local)
- [Comptes administrateurs et participants](#comptes-de-démonstration-locaux)
- [Compilation et tests](#tests-et-compilation)
- [Dépannage et arrêt](#dépannage-et-arrêt)
- [Documentation technique](#documentation-technique)

## État validé au 4 novembre 2026

- Entités Java et schéma PostgreSQL alignés : `ateliers`, `participants`, `centres`, `salles`.
- Migration transactionnelle testée sur sauvegarde restaurée, avec retour arrière.
- Démarrage Windows vérifié par JDBC et HTTP, Swagger public et API métier protégée.
- Formulaire Angular : dates/heures passées bloquées et centres filtrés pour les visiteurs.
- Connexion administrateur par e-mail ; mot de passe des comptes de démonstration dérivé de leur matricule et stocké en BCrypt.
- Recette complète du 4 novembre 2026 : **188 tests backend, 83 tests Angular et 8 tests Chromium réussis**, sans échec ni test ignoré ; persistance vérifiée après redémarrage du PostgreSQL de test.
- Créations publique et privée vérifiées en navigateur sur base isolée ; parcours visiteur autorisé et suspension vérifiés.

Le [dossier phase 4](AtelierConnect/docs/phase-4/index.md) contient les diagrammes et la préparation du TFE.
Le [rapport du 2 novembre](AtelierConnect/docs/validation-2026-11-02.md) documente la recette précédente.
Les [preuves du 1er novembre](AtelierConnect/docs/refactoring-final-2026-11-01.md) et la
[recette précédente](AtelierConnect/docs/phase-4/recette-finale.md) restent disponibles comme historique.

## Interface et identité visuelle

L'accueil et les espaces participant et administrateur partagent une identité
ivoire, vert forêt et terre cuite, avec trois illustrations d'ateliers créatifs
servies localement. Les pages s'adaptent aux écrans d'ordinateur et de téléphone ;
la navigation mobile dispose d'un menu accessible au clavier.

Le titre public précise l'usage : **AtelierConnect — Réservation d'ateliers créatifs**,
avec une version anglaise. Les cartes PARTENAIRE, ABONNE et VISITEUR affichent
uniquement les délais de réservation, sans les anciennes indications G/S/L + chiffres.

La [présentation de la refonte](AtelierConnect/docs/design-2026-11-02.md) décrit les 14 pages
contrôlées, les styles partagés et les visuels. Les règles métier et les données
existantes sont conservées.

## Prérequis et configuration

L'[audit des anciennes mentions](AtelierConnect/docs/audit-nomenclature-2026-11-04.md)
explique les corrections de Swagger et les références historiques conservées
pour les connexions, les sessions et les données existantes.

Les centres locaux s'appellent désormais **Centre Bruxelles / Centre Namur**, et les
salles **Salle A/B/C/1/2**, avec les mêmes identifiants et réservations.
Le [rapport de renommage](AtelierConnect/docs/resource-label-renaming.md) décrit
la migration réversible, sa répétition sur copie et les **192 tests backend**
réussis après adaptation de l'initialiseur. Les identifiants de connexion ci-dessous
restent inchangés.

Les [noms des villes belges](AtelierConnect/docs/belgian-centre-cities.md) ont été
choisis le 5 novembre 2026. Les anciens e-mails administrateurs sont conservés :
ils identifient les mêmes comptes, rattachés aux mêmes centres par leur identifiant.

Docker Desktop démarré, Java 21, Node.js 22 >= 22.12 ou Node.js 24, npm.
Depuis la racine Git, le backend se trouve dans le sous-dossier `AtelierConnect`.

| Variable requise | Usage |
| --- | --- |
| `POSTGRES_USER` | Rôle PostgreSQL existant |
| `POSTGRES_PASSWORD` | Mot de passe de ce rôle |
| `JWT_SECRET` | Clé de signature d'au moins 32 octets UTF-8 |
| `APP_DEMO_MEMBER_PASSWORD` | Mot de passe local des participants de démonstration |

`SPRING_DATASOURCE_URL` est facultatif : défaut `jdbc:postgresql://localhost:5441/atelierconnect_db`.
`APP_DEMO_ADMIN_PASSWORD` n'est plus requis par l'initialiseur : les administrateurs de démonstration utilisent leur matricule.

Le script charge les variables absentes depuis une configuration privée existante
ou les demande sans les afficher. Il ne crée pas de nouveau fichier de secrets :

```powershell
cd .\AtelierConnect
.\scripts\set-local-environment.ps1
```

La configuration privée existante `.run/AtelierConnect_Local.private.run.xml` est
ignorée par Git. L'ancienne option `-ConfigureIntelliJ` est désactivée.
Dans IntelliJ, sélectionner **AtelierConnect — Local** si elle existe, ou préparer
une configuration Run/Debug privée avec les variables requises.
Les variables d'un terminal ne sont pas transmises à une instance IntelliJ déjà démarrée.

Ne jamais publier les configurations privées, fichiers `.env`, clés, journaux ou sauvegardes.

## Démarrer en local

### Terminal 1 — PostgreSQL

Depuis un nouveau terminal PowerShell à la racine Git, entrer dans le backend :

```powershell
cd .\AtelierConnect
.\scripts\set-local-environment.ps1
docker compose up -d
docker compose ps
```

Compose démarre uniquement PostgreSQL sur le port Windows **5441** :
conteneur `atelierconnect-db`, base `atelierconnect_db`, volume persistant conservé.
Changer une variable Compose ne modifie pas le mot de passe d'un rôle déjà présent.

Pour vérifier l'authentification réelle depuis Windows :

```powershell
.\scripts\verify-postgres-login.ps1
```

### Terminal 2 — Backend

Depuis un nouveau terminal PowerShell à la racine Git, entrer dans le backend :

```powershell
cd .\AtelierConnect
.\scripts\run-local.ps1 -Component Backend -Build
```

Le lanceur vérifie le port **8082**, PostgreSQL et JDBC, compile si nécessaire,
démarre le JAR sans redémarrage DevTools et contrôle Swagger et l'API publique.
Les journaux filtrés restent sous `.local/diagnostics/`. Une vérification échouée
retourne une erreur ; après validation, le backend continue en arrière-plan.

Pour les lancements suivants, `-Build` est facultatif. Ne pas lancer une seconde
instance si IntelliJ utilise déjà 8082. Le lanceur désactive l'initialisation
automatique des données pour préserver la base existante.

Compilation seule, sans tests d'intégration sur la base applicative :

```powershell
.\mvnw.cmd -B -ntp -DskipTests clean package
```

### Terminal 3 — Frontend

Depuis la racine Git :

```powershell
cd .\AtelierConnect\frontend
npm ci
npm start
```

`npm ci` est nécessaire à la première installation ou après un changement du lockfile.

| Accès | URL |
| --- | --- |
| Angular | http://localhost:4200 |
| Connexion administrateur | http://localhost:4200/admin/login |
| Connexion participant | http://localhost:4200/member |
| Swagger | http://localhost:8082/swagger-ui/index.html |
| OpenAPI | http://localhost:8082/v3/api-docs |
| API publique | http://localhost:8082/api/ateliers/public |

Le proxy Angular transmet `/api` vers `localhost:8082`.

## Base existante : migration avant démarrage

Les anciens noms `matches`, `membres`, `sites`, `terrains` sont remplacés par
les noms AtelierConnect. **Hibernate update ne réalise pas ce renommage.**

Suivre [la procédure de migration](AtelierConnect/docs/schema-naming-migration.md) : arrêter le
backend, sauvegarder, vérifier une restauration sur base isolée, puis appliquer
le script `.up.sql`. Le script `.down.sql` permet le retour aux anciens noms avec
la version du backend correspondante.

La migration vérifie les empreintes de toutes les lignes, les séquences et les
relations. `SchemaNamingGuard` refuse un ancien schéma avant qu'Hibernate puisse
créer des tables parallèles vides. `spring.jpa.hibernate.ddl-auto=update` reste conservé.
Voir [les mappings des entités](AtelierConnect/docs/entities-compatibility.md).

## Comptes de démonstration locaux

Ces comptes correspondent à la base locale existante et ont été vérifiés le
4 novembre 2026. Les identifiants ci-dessous sont documentés à la demande du
propriétaire pour la démonstration locale. Les mots de passe PostgreSQL et la
clé JWT restent dans la configuration privée et ne figurent pas dans ce fichier.

### Administrateurs : e-mail + mot de passe

Ouvrir `/admin/login` sur l'adresse du frontend. Le mot de passe de chaque
administrateur de démonstration est son matricule, en majuscules.

| Périmètre | E-mail de connexion | Mot de passe |
| --- | --- | --- |
| Global | `admin@padel.com` | `ADMIN001` |
| Centre Bruxelles | `admin.lyon@padel.com` | `ADMIN002` |
| Centre Namur | `admin.paris@padel.com` | `ADMIN003` |

Les e-mails historiques en `padel.com` sont ceux réellement enregistrés dans la
base. Ils ne doivent pas être remplacés par une adresse inventée en `atelierconnect`.
Les trois connexions ont renvoyé HTTP 200.

### Participants : matricule + mot de passe

Ouvrir `/member`. **Saisir le matricule comme identifiant** ; l'e-mail est indiqué
ici pour reconnaître le compte. Le mot de passe commun de démonstration est
`P1234!` : P majuscule, puis 1234 et un point d'exclamation.

| Participant | Profil | Matricule de connexion | E-mail | Mot de passe |
| --- | --- | --- | --- | --- |
| Lucas Martin | PARTENAIRE | `G1001` | `lucas.martin@gmail.com` | `P1234!` |
| Emma Dubois | PARTENAIRE | `G1002` | `emma.dubois@gmail.com` | `P1234!` |
| Tom Bernard | ABONNE | `S10001` | `tom.bernard@gmail.com` | `P1234!` |
| Sarah Leroy | ABONNE | `S10002` | `sarah.leroy@gmail.com` | `P1234!` |
| Alex Petit | VISITEUR | `L10001` | `alex.petit@gmail.com` | `P1234!` |
| Demo Penalise | VISITEUR suspendu | `L10002` | `demo.penalise@gmail.com` | `P1234!` |

Les six connexions ont renvoyé HTTP 200. Le compte `L10002` peut se connecter,
mais sa suspension bloque volontairement la création d'ateliers. Utiliser
`L10001` pour la démonstration d'un visiteur autorisé et `G1001` pour un partenaire.
Les valeurs SQL historiques `GLOBAL`, `SITE`, `LIBRE` correspondent respectivement
aux profils PARTENAIRE, ABONNE et VISITEUR exposés par l'application.

Si le frontend affiche `http://localhost:56700` dans son terminal, utiliser
`http://localhost:56700/admin/login` ou `http://localhost:56700/member` ; le
frontend transmet toujours `/api` au backend sur 8082.

Les comptes existants ne sont pas recréés à chaque démarrage. Les scripts
`set-demo-admin-passwords.ps1` et `set-demo-participant-passwords.ps1` changent
explicitement les hashes des comptes existants : ils ne sont pas nécessaires
pour une connexion normale. La vérification de ce document n'a changé aucun mot de passe.

## Démonstration et règles métier

- Participants : connexion par matricule et mot de passe configuré localement.
- Administrateurs : connexion par e-mail et mot de passe correspondant au matricule.
- VISITEUR : jusqu'à 7 jours, uniquement dans les centres explicitement ouverts.
- ABONNE : jusqu'à 15 jours dans son centre de rattachement.
- PARTENAIRE : jusqu'à 30 jours.
- Atelier : 120 minutes, pause de 20 minutes, capacité de 6, prix total de 90 EUR.
- Soldes impayés, suspensions, fermetures et chevauchements restent bloquants.

`DataInitializer` prépare les données de démonstration quand son initialisation
est explicitement activée sur une base neuve. Les scripts de changement des
identifiants et d'ouverture aux visiteurs sont des opérations explicites,
jamais automatiques au démarrage.

Documentation : [participants](AtelierConnect/docs/participant-demo-login.md),
[administrateurs](AtelierConnect/docs/admin-demo-login.md),
[accès visiteur et refus pour suspension](AtelierConnect/docs/demo-visitor-access.md).

## Tests et compilation

Depuis `AtelierConnect/frontend` :

```powershell
npm test -- --maxWorkers=2
npm run build
```

Les tests backend utilisent une **base isolée**, jamais `atelierconnect_db`.
Avec le conteneur de validation PostgreSQL 15 préparé, étiqueté
`atelierconnect.validation=true` et publié sur 5549 :

```powershell
.\scripts\test-backend-isolated.ps1
```

Pour la recette complète, depuis le dossier backend, arrêter les serveurs locaux
sur 8082 et 4200, puis exécuter :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/validate-fullstack.ps1
```

Cette procédure prépare des bases dédiées, compile, exécute les tests backend et
Angular, lance Playwright sur les vrais serveurs puis vérifie la persistance après
redémarrage du PostgreSQL de test. Elle ne touche pas à la base applicative.
Après une première installation, `-SkipInstall` réutilise les dépendances installées.

Rapports locaux : `.local/validation/<date>/summary.json`, `target/surefire-reports/`,
`frontend/playwright-report/`. Voir le [guide Windows/IntelliJ](AtelierConnect/docs/phase-4/installation-windows.md).

## Dépannage et arrêt

### IntelliJ : `Could not resolve placeholder 'JWT_SECRET'`

Le programme a compilé, mais les variables de son processus de lancement sont
absentes. Dans la liste à côté du bouton Exécuter, choisir **AtelierConnect — Local**.
La configuration `AtelierConnectApplication` sans variables échoue au démarrage.
Vérifier les noms de variables de la section configuration sans afficher leurs valeurs.
Pour préserver les données existantes, utiliser `APP_DATA_INITIALIZE=false`.
Pendant le diagnostic, `SPRING_DEVTOOLS_RESTART_ENABLED=false` évite que DevTools
masque un échec par un code de sortie trompeur. Le script `run-local.ps1` gère ces options.
Ne pas lancer une seconde instance sur 8082 si le backend fonctionne déjà.

### Connexion administrateur HTTP 400

Vérifier l'e-mail et le mot de passe dans le tableau administrateurs. Ouvrir les
outils de développement du navigateur, onglet Réseau, puis la réponse JSON de
`POST /api/auth/login`. Le message « Identifiants invalides. » indique un refus
d'authentification ; le port du frontend dans l'URL est normal lorsque le proxy est actif.

### Création d'atelier HTTP 400

Consulter la réponse JSON de `POST /api/ateliers` et le message du formulaire.
Contrôler la date et l'heure futures, l'accès au centre, les disponibilités,
le solde et les suspensions. Le refus pour `L10002` est attendu. Ne pas désactiver
les validations ou la sécurité pour contourner ce refus.

### PostgreSQL ou port occupé

Depuis `AtelierConnect/` :

```powershell
docker compose ps
.\scripts\verify-postgres-login.ps1
Get-NetTCPConnection -State Listen -LocalPort 8082,4200 -ErrorAction SilentlyContinue
```

Le contrôle JDBC vérifie l'authentification depuis Windows. En cas de SQLState
`28P01`, vérifier le rôle existant et sa configuration privée. Modifier la variable
Compose ne change pas le mot de passe d'un rôle déjà créé.

### Arrêter sans perdre les données

Arrêter Angular avec `Ctrl+C` dans son terminal. Arrêter le backend avec le bouton
Stop d'IntelliJ s'il a été lancé dans l'IDE. Pour un backend lancé en arrière-plan,
identifier son PID sur 8082 et vérifier qu'il s'agit bien de cette application
avant de l'arrêter. Depuis `AtelierConnect/`, arrêter PostgreSQL sans supprimer son volume :

```powershell
docker compose stop
```

Conserver `spring.jpa.hibernate.ddl-auto=update`, les volumes Docker, les
configurations IntelliJ et les fichiers privés. Ne pas utiliser de suppression
ou de réinitialisation de base pour résoudre un problème de lancement.

## Documentation technique

Les documents spécialisés restent disponibles, avec un nom décrivant leur rôle :

- [Migration et sauvegarde du schéma](AtelierConnect/docs/schema-naming-migration.md).
- [Compatibilité des entités](AtelierConnect/docs/entities-compatibility.md).
- [Dossier TFE et matrice des thèmes](AtelierConnect/docs/phase-4/index.md).
- [Provenance des captures de recette](AtelierConnect/docs/phase-4/preuves/index.md).
- [Refonte graphique et recette du 4 novembre](AtelierConnect/docs/design-2026-11-02.md).
- [Provenance et prompts des illustrations](AtelierConnect/frontend/public/images/provenance.md).

Les rapports historiques gardent leurs dates et leurs anciens nombres de tests ;
la dernière recette complète réussie est celle du 4 novembre : 188 / 83 / 8.
