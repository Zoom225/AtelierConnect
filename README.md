# AtelierConnect — Réservation d’ateliers créatifs

Gestion et réservation d'ateliers collaboratifs multi-centres : **Java 21, Spring Boot 3.5, Spring Security/JWT, PostgreSQL 15 et Angular 21**.


Ce fichier est le **README unique du dépôt** : démarrage, compilation, tests,
connexions de démonstration et dépannage. Le backend est sous `AtelierConnect/`
et le frontend sous `AtelierConnect/frontend/`.

Dépôt : https://github.com/Zoom225/AtelierConnect
Branche de travail : `fix/6-demonstration-automatique`, basée sur `fix/5-nomenclature-espace-participant`.
Suivi courant : [issue #6 — démonstration automatique et guide de démarrage](https://github.com/Zoom225/AtelierConnect/issues/6).
Nomenclature et espace participant : [issue #5](https://github.com/Zoom225/AtelierConnect/issues/5), sur la branche basée sur `feat/3-interface-readme-tracabilite`.
Refonte et documentation initiales : [issue #3](https://github.com/Zoom225/AtelierConnect/issues/3).

## Sommaire

- [Prérequis et configuration](#prérequis-et-configuration)
- [Commandes simples : Docker, backend et frontend](#démarrer-en-local)
- [Comptes administrateurs et participants](#comptes-de-démonstration-locaux)
- [Compilation et tests](#tests-et-compilation)
- [Compiler le frontend seulement](#compiler-le-frontend-seulement)
- [Dépannage et arrêt](#dépannage-et-arrêt)
- [Documentation technique](#documentation-technique)

## État validé au 8 novembre 2026

- Entités Java et schéma PostgreSQL alignés : `ateliers`, `participants`, `centres`, `salles`.
- Migration transactionnelle testée sur sauvegarde restaurée, avec retour arrière.
- Démarrage Windows vérifié par JDBC et HTTP, Swagger public et API métier protégée.
- Formulaire Angular : dates/heures passées bloquées et centres filtrés pour les visiteurs.
- Connexion administrateur par e-mail ; mot de passe des comptes de démonstration dérivé de leur matricule et stocké en BCrypt.
- Recette complète du 4 novembre 2026 : **188 tests backend, 83 tests Angular et 8 tests Chromium réussis**, sans échec ni test ignoré ; persistance vérifiée après redémarrage du PostgreSQL de test.
- Créations publique et privée vérifiées en navigateur sur base isolée ; parcours visiteur autorisé et suspension vérifiés.
- Contrôle du 7 novembre : **198 tests backend et 88 tests Angular réussis** ; suspension bloquante jusqu'à sa date de fin incluse et message automatique vérifiés en navigateur. Huit scénarios Chromium ont réussi lors de la recette ; le neuvième a été validé en relance ciblée après correction des attentes de chargement du test d'administration.
- Validation courante : **246 tests backend, 96 tests Angular et 10 scénarios Chromium réussis**. Les tests nécessitant PostgreSQL utilisent une base isolée. Ils couvrent notamment le coût des six places, les règlements concurrents, les relances sans double paiement et le maintien d'une suspension active après règlement.
- Code partagé simplifié : statuts et badges de six pages Angular, contrôles des paiements et fonctions des scripts de validation. Les détails figurent dans le [rapport de refactorisation du 8 novembre](AtelierConnect/docs/refactoring-2026-11-08.md).

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

Les commandes de démarrage chargent les variables depuis la configuration privée
existante ou les demandent sans les afficher. Aucun fichier de secrets supplémentaire
n'est nécessaire.

La configuration privée existante `.run/AtelierConnect_Local.private.run.xml` est
ignorée par Git. L'ancienne option `-ConfigureIntelliJ` est désactivée.
Dans IntelliJ, sélectionner **AtelierConnect — Local** si elle existe, ou préparer
une configuration Run/Debug privée avec les variables requises.
Les variables d'un terminal ne sont pas transmises à une instance IntelliJ déjà démarrée.

Ne jamais publier les configurations privées, fichiers `.env`, clés, journaux ou sauvegardes.

## Démarrer en local

Ouvrir **Docker Desktop**, puis trois terminaux **PowerShell**.
Suivre les étapes dans l'ordre et attendre le résultat indiqué avant de passer
à la suivante. Chaque bloc inclut le chemin du projet pour faciliter le copier-coller.

| Étape | Terminal | Action | Résultat attendu |
| --- | --- | --- | --- |
| 1 | Terminal 1 | Démarrer PostgreSQL | Conteneur `atelierconnect-db` **healthy**, port **5441**. |
| 2 | Terminal 2 | Compiler et démarrer le backend | **`Backend verified`**, port **8082**. |
| 3 | Terminal 3 | Compiler et démarrer le frontend | Application disponible sur **http://localhost:4200**. |

### 1. Terminal 1 — Docker / PostgreSQL

Copier et exécuter les commandes suivantes **une par une**. Si une commande
échoue, corriger l'erreur avant d'exécuter la suivante.

```powershell
cd C:\Users\user\IdeaProjects\AtelierConnect
.\AtelierConnect\scripts\set-local-environment.ps1
docker compose -f .\AtelierConnect\docker-compose.yml up -d
docker compose -f .\AtelierConnect\docker-compose.yml ps
```

Attendre que `atelierconnect-db` affiche **healthy**. Si l'état est encore
`starting`, relancer seulement :

```powershell
docker compose -f .\AtelierConnect\docker-compose.yml ps
```

### 2. Terminal 2 — Backend

Copier ce bloc dans le **deuxième terminal** :

```powershell
cd C:\Users\user\IdeaProjects\AtelierConnect
.\AtelierConnect\scripts\run-local.ps1 -Component Backend -Build -Restart
```

Attendre la compilation Maven réussie, puis **`Backend verified`**.
Le backend, l'API publique et Swagger sont alors accessibles sur le port **8082**.

### 3. Terminal 3 — Frontend

Si Angular tourne déjà, l'arrêter avec **Ctrl+C** dans son terminal.
Copier ensuite ce bloc dans le **troisième terminal** :

```powershell
cd C:\Users\user\IdeaProjects\AtelierConnect
.\AtelierConnect\scripts\run-local.ps1 -Component Frontend -Build
```

Attendre la compilation Angular réussie et l'adresse **http://localhost:4200**,
puis ouvrir cette adresse dans le navigateur. **Laisser ce terminal ouvert.**

### Fonctionnement des lanceurs

La première commande charge les variables privées requises ou les demande sans
afficher les secrets. Docker utilise PostgreSQL 15 ; aucune compilation Docker
n'est nécessaire pour ce service. Le volume et les données existants sont conservés.

Le lanceur backend recharge ses variables dans son propre terminal, vérifie la
connexion JDBC, compile sans tests sur la base applicative et vérifie les réponses
HTTP. `-Restart` redémarre uniquement l'instance Java de ce projet si elle occupe
déjà 8082. Il conserve `APP_DATA_INITIALIZE=false` et `ddl-auto=update`.
Les journaux locaux sont dans `AtelierConnect/.local/diagnostics/`.

Le lanceur frontend réutilise les dépendances installées et lance `npm ci`
uniquement si nécessaire. Le proxy transmet `/api` au backend sur **8082**.
En cas d'installation incomplète, suivre la
[réparation frontend](#réparer-les-dépendances-frontend).

### Compiler le backend seulement

```powershell
cd C:\Users\user\IdeaProjects\AtelierConnect\AtelierConnect
.\mvnw.cmd -DskipTests package
```

Cette commande compile sans lancer le backend.
JAR : `AtelierConnect/target/atelierconnect-backend-0.0.1-SNAPSHOT.jar`.

### Compiler le frontend seulement

Depuis la racine Git, avec les dépendances frontend déjà installées :

```powershell
npm.cmd --prefix .\AtelierConnect\frontend run build
```

Cette commande compile sans démarrer le serveur Angular.
Résultat : `AtelierConnect/frontend/dist/frontend/`.
`npm ci` n'est pas nécessaire à chaque compilation.

### Démarrer le backend avec la flèche verte IntelliJ

Utiliser cette méthode à la place du lancement backend dans PowerShell :

1. Démarrer PostgreSQL et arrêter l'autre instance du backend sur 8082.
2. Dans la liste **en haut à côté de ▶**, sélectionner **AtelierConnect — Local**.
3. Vérifier `APP_DATA_INITIALIZE=false` dans cette configuration privée, puis
   cliquer sur **▶ dans la barre du haut**.

La configuration `AtelierConnectApplication` créée à côté de `main()` ne contient
pas les variables PostgreSQL/JWT et échoue avec `JWT_SECRET` manquant.
`Ctrl+F9` (*Build > Build Project*) compile uniquement. Ne pas lancer le backend
simultanément par IntelliJ et par PowerShell.

### Réparer les dépendances frontend

Uniquement pour une première installation, un changement du lockfile ou une
installation incomplète. Arrêter les serveurs Angular et tâches de surveillance
du projet avant d'exécuter :

```powershell
cd C:\Users\user\IdeaProjects\AtelierConnect\AtelierConnect
.\scripts\run-local.ps1 -Component Frontend -Build -InstallDependencies
```

Ce lanceur installe avec `npm ci`, compile et démarre Angular. Il refuse
l'installation si le port 4200 ou un processus esbuild du frontend est encore
actif. Il ne faut pas exécuter `npm install` ou `npm start` dans le dossier
backend : son fichier de projet est `pom.xml`, pas `package.json`.

### Ouvrir l'application

| Accès | URL |
| --- | --- |
| Frontend | http://localhost:4200 |
| Connexion administrateur | http://localhost:4200/admin/login |
| Connexion participant | http://localhost:4200/member |
| Swagger | http://localhost:8082/swagger-ui/index.html |
| OpenAPI | http://localhost:8082/v3/api-docs |
| API publique | http://localhost:8082/api/ateliers/public |

Un `BUILD SUCCESS` confirme la compilation, pas le démarrage réel de Spring Boot.
Le lanceur backend vérifie aussi les réponses HTTP. En cas d'erreur, ne pas
continuer avec les commandes suivantes ; consulter le [dépannage](#dépannage-et-arrêt).

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
| Demo Penalise | VISITEUR avec dette et suspension réactivées à chaque connexion | `L10002` | `demo.penalise@gmail.com` | `P1234!` |

Les six connexions ont renvoyé HTTP 200 lors de la recette précédente.
Le compte `L10002` dispose désormais d'un **scénario de démonstration automatique** :
à chaque connexion réussie avec son matricule et son mot de passe, son solde est
remonté à **90 EUR** et sa suspension de démonstration est renouvelée jusqu'à
**la date de connexion + 7 jours incluse**. Un solde déjà supérieur à 90 EUR est conservé.
Ce cas illustre six places impayées à 15 EUR ; la dette reste distincte de la suspension.

Pour la démonstration : se connecter avec `L10002`, consulter le solde et la
suspension, puis régler la dette dans **Mes paiements**. Le solde passe à zéro,
mais la suspension reste active. Se déconnecter puis se reconnecter remet
automatiquement la dette à 90 EUR : aucune manipulation SQL n'est nécessaire.
Les connexions répétées ne cumulent pas les 90 EUR ni les suspensions de démonstration.
L'historique des règlements est conservé et chaque complément de dette est journalisé.

Ce comportement est activé par défaut pour le seul compte VISITEUR `L10002`
avec l'e-mail `demo.penalise@gmail.com`. Pour le désactiver, définir
`APP_DEMO_RESET_PENALTY_ON_LOGIN=false` avant de démarrer le backend.
Dans ce cas, le solde réglé reste à zéro et la suspension expire normalement.
Un redémarrage seul ne réactive pas le scénario : il faut une nouvelle connexion.
L'ancien script `scripts/demo/set-visible-demo-debt.sql` reste une opération
manuelle distincte, jamais lancée automatiquement.

Validation de cette évolution : **52 tests ciblés** d'authentification,
de démonstration et d'initialisation, puis **22 tests** de service et de contrôleur
d'ateliers réussis, dont trois contrôles ciblant l'autorisation de modification.
Compilation Maven et redémarrage local réussis ; deux connexions API de `L10002`
ont confirmé un solde de 90 EUR, une suspension active et l'absence de cumul.

Une suspension active bloque la création d'ateliers publics et privés.
La date de fin est incluse : une suspension qui se termine aujourd'hui reste
active jusqu'à la fin de cette journée. Le profil et les formulaires affichent
automatiquement un message explicatif ; le backend refuse aussi une création
directe par l'API. Après expiration, la création redevient possible si les autres
règles métier sont respectées. Aucun atelier existant n'est supprimé. Utiliser
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
- À J−1 (la veille dans le calendrier du serveur), un atelier encore **privé et planifié**, avec moins de **6 participants confirmés**, déclenche une suspension de son organisateur jusqu'à la date du contrôle + 7 jours incluse. Le contrôle passe toutes les cinq minutes, ce qui couvre aussi les redémarrages et créations tardives dans cette journée.
- Une seule sanction est enregistrée par atelier, avec son numéro dans le motif. Les passages suivants ne prolongent pas cette sanction. Les ateliers complets, annulés, publics, passés ou à J−2 ne sont pas sanctionnés par ce contrôle.
- Pour le privé incomplet, la dette de l'organisateur est le **prix total moins les places confirmées et payées** : aucune place payée = 90 EUR ; deux places payées = 60 EUR. Une ancienne dette incluse dans un paiement ne représente pas des places supplémentaires.
- L'imputation du privé est enregistrée une seule fois dans `mouvements_dette`. Le traitement des invitations impayées ne refacture pas ces places. L'imputation unitaire historique des réservations publiques impayées reste distincte.
- Une fois le privé imputé, ses inscriptions et paiements individuels sont clos. Les modifications, ouvertures publiques et annulations remboursées susceptibles de modifier cette imputation sont refusées avec un message explicatif.
- L'ouverture volontaire au public à J−2 reste soumise à l'accord explicite de l'organisateur ; elle ne crée aucune sanction ni dette.

Depuis **Mes paiements**, le bouton **Régler ma dette** permet de solder le compte
sans réserver une nouvelle séance, y compris pendant une suspension. Le montant
attendu et une clé de demande sont vérifiés sous verrou : une relance ne facture
pas deux fois et n'efface pas une nouvelle dette. La participation et la création
restent soumises à deux conditions : **dette soldée ET suspension terminée**.
Un redémarrage ne prolonge pas une ancienne suspension de démonstration.

Les règlements sont enregistrés dans l'application, comme les paiements de
réservation existants ; aucun prestataire bancaire n'est connecté. Le journal
conserve les imputations et règlements sans modifier les mappings des tables
existantes. La migration additive est
`scripts/migrations/20261107_add_debt_ledger.up.sql` ; une sauvegarde précède son
application locale. Les soldes historiques sont conservés.

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
```

Les tests backend utilisent une **base isolée**, jamais `atelierconnect_db`.
Avec le conteneur de validation PostgreSQL 15 préparé, étiqueté
`atelierconnect.validation=true` et publié sur 5549 :

```powershell
.\scripts\test-backend-isolated.ps1
```

Le script accepte aussi `-TestContainer` et `-PostgresPort` pour utiliser un autre
conteneur de validation déjà préparé et étiqueté. Le conteneur applicatif et le
port 5441 sont refusés. Pendant les tests, le déclenchement périodique des sanctions
est désactivé ; les scénarios appellent explicitement le planificateur.

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

Le code a compilé, mais la configuration lancée ne reçoit pas les variables
nécessaires. Suivre la [procédure IntelliJ](#démarrer-le-backend-avec-la-flèche-verte-intellij)
et choisir **AtelierConnect — Local**. Les variables chargées dans un terminal
ne sont pas ajoutées à une instance IntelliJ déjà ouverte. Le lanceur PowerShell
charge l'environnement et désactive le redémarrage DevTools pendant son diagnostic.

### Connexion administrateur HTTP 400

Vérifier l'e-mail et le mot de passe dans le tableau administrateurs. Ouvrir les
outils de développement du navigateur, onglet Réseau, puis la réponse JSON de
`POST /api/auth/login`. Le message « Identifiants invalides. » indique un refus
d'authentification ; le port du frontend dans l'URL est normal lorsque le proxy est actif.

### Frontend : `EPERM` sur `esbuild.exe` ou `ENOENT` sur `package.json`

`EPERM unlink ...esbuild.exe` signifie que Windows refuse de remplacer le
fichier. Fermer les serveurs Angular et tâches de surveillance du projet avec
`Ctrl+C`, y compris ceux lancés sur un autre port, puis suivre la
[réparation des dépendances](#réparer-les-dépendances-frontend).

`ENOENT ...AtelierConnect\package.json` indique que npm a été exécuté dans le
dossier backend. Reprendre le chemin `frontend` indiqué au terminal 3. Les fichiers
npm utiles sont `frontend/package.json` et `frontend/package-lock.json`.

### Création d'atelier HTTP 400

Consulter la réponse JSON de `POST /api/ateliers` et le message du formulaire.
Contrôler la date et l'heure futures, l'accès au centre, les disponibilités,
le solde et les suspensions. Le refus pour `L10002` est attendu. Ne pas désactiver
les validations ou la sécurité pour contourner ce refus.

### PostgreSQL ou port occupé

Pour redémarrer AtelierConnect sur 8082, utiliser la commande du terminal 2,
qui contient `-Restart`. Un processus inconnu n'est jamais arrêté par le lanceur.

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

Les rapports historiques gardent leurs dates et leurs anciens nombres de tests.
La validation du 8 novembre comprend 246 tests backend et 96 tests Angular,
ainsi que les compilations Maven et Angular. Les dix scénarios Chromium ont
réussi lors d'une recette sur une nouvelle base isolée, après correction de
l'attente du chargement des salles dans le test de création.
Les 246 cas backend comprennent les 236 tests de la recette complète et dix
tests supplémentaires du garde-fou de sélection de la base, exécutés sans base.
La persistance des réservations, soldes et mouvements a été vérifiée après
redémarrage du PostgreSQL isolé. Les vérifications locales finales ont renvoyé
HTTP 200 pour Angular, Swagger, OpenAPI et l'API publique via le proxy ;
l'accès anonyme au solde reste refusé (HTTP 403).
