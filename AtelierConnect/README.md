# AtelierConnect

Gestion et réservation d'ateliers collaboratifs multi-centres : **Java 21, Spring Boot 3.5, Spring Security/JWT, PostgreSQL 15 et Angular 21**.

## État validé au 2 novembre 2026

- Entités Java et schéma PostgreSQL alignés : `ateliers`, `participants`, `centres`, `salles`.
- Migration transactionnelle testée sur sauvegarde restaurée, avec retour arrière.
- Démarrage Windows vérifié par JDBC et HTTP, Swagger public et API métier protégée.
- Formulaire Angular : dates/heures passées bloquées et centres filtrés pour les visiteurs.
- Connexion administrateur par e-mail ; mot de passe des comptes de démonstration dérivé de leur matricule et stocké en BCrypt.
- Recette complète : **188 tests backend, 83 tests Angular et 6 tests Chromium réussis**, sans échec ni test ignoré ; persistance vérifiée après redémarrage du PostgreSQL de test.
- Créations publique et privée vérifiées en navigateur sur base isolée ; parcours visiteur autorisé et suspension vérifiés.

Le [dossier phase 4](docs/phase-4/README.md) contient les diagrammes et la préparation du TFE.
Le [rapport du 2 novembre](docs/validation-2026-11-02.md) décrit la dernière recette complète.
Les [preuves du 1er novembre](docs/refactoring-final-2026-11-01.md) et la
[recette précédente](docs/phase-4/recette-finale.md) restent disponibles comme historique.

## Prérequis et configuration

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

Depuis le dossier backend :

```powershell
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

Depuis le dossier backend :

```powershell
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
| Swagger | http://localhost:8082/swagger-ui/index.html |
| OpenAPI | http://localhost:8082/v3/api-docs |
| API publique | http://localhost:8082/api/ateliers/public |

Le proxy Angular transmet `/api` vers `localhost:8082`.

## Base existante : migration avant démarrage

Les anciens noms `matches`, `membres`, `sites`, `terrains` sont remplacés par
les noms AtelierConnect. **Hibernate update ne réalise pas ce renommage.**

Suivre [la procédure de migration](docs/schema-naming-migration.md) : arrêter le
backend, sauvegarder, vérifier une restauration sur base isolée, puis appliquer
le script `.up.sql`. Le script `.down.sql` permet le retour aux anciens noms avec
la version du backend correspondante.

La migration vérifie les empreintes de toutes les lignes, les séquences et les
relations. `SchemaNamingGuard` refuse un ancien schéma avant qu'Hibernate puisse
créer des tables parallèles vides. `spring.jpa.hibernate.ddl-auto=update` reste conservé.
Voir [les mappings des entités](docs/entities-compatibility.md).

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

Documentation : [participants](docs/participant-demo-login.md),
[administrateurs](docs/admin-demo-login.md),
[accès visiteur et refus pour suspension](docs/demo-visitor-access.md).

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
`frontend/playwright-report/`. Voir le [guide Windows/IntelliJ](docs/phase-4/installation-windows.md).
