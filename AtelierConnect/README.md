# AtelierConnect

État du refactoring et preuves de validation : [rapport du 1er novembre 2026](docs/refactoring-final-2026-11-01.md).

Application de gestion et réservation d'ateliers collaboratifs multi-centres : backend Java 21 / Spring Boot 3.5, frontend Angular 21 et base PostgreSQL. Le backend utilise le domaine Participant / Atelier / Salle / Centre et conserve temporairement les anciennes routes REST pour assurer la transition Angular.

## Validation full-stack et préparation du TFE

Le [dossier de préparation phase 4](docs/phase-4/README.md) contient la matrice des
onze thèmes, les diagrammes, le manuel et la référence API. Le [guide Windows et
IntelliJ](docs/phase-4/installation-windows.md) décrit la configuration privée et
le script de validation isolée. Le [rapport de tests](docs/phase-4/tests-limites.md)
conserve la validation précédente. Le [rapport de recette finale](docs/phase-4/recette-finale.md)
documente désormais 179 tests Java, 75 tests Angular, six parcours Chromium, les
autorisations vérifiées, les captures et le plan des données historiques.
La PR #2 reste en brouillon ; aucune branche phase 4 avant intégration de phase 3.

## Prérequis

Java 21, Docker Desktop démarré, Node.js 22 >= 22.12 ou Node.js 24, et npm.
Les commandes ci-dessous utilisent **Windows PowerShell**.

Le dépôt contient un sous-dossier `AtelierConnect` : depuis la racine Git,
se placer dans le dossier backend contenant `pom.xml` et `docker-compose.yml` :

```powershell
cd .\AtelierConnect
java -version
.\mvnw.cmd -version
node --version
npm --version
docker version
```

Les sections Docker et backend partent de ce dossier. La section frontend utilise
un second terminal, dont le dossier de départ est indiqué séparément.

## Configuration locale

Variables requises :

| Variable | Usage |
| --- | --- |
| `POSTGRES_USER` | Utilisateur PostgreSQL, commun à Docker Compose et Spring Boot |
| `POSTGRES_PASSWORD` | Mot de passe PostgreSQL, commun à Docker Compose et Spring Boot |
| `JWT_SECRET` | Clé de signature JWT, aléatoire, au moins 32 octets |
| `APP_DEMO_MEMBER_PASSWORD` | Mot de passe initial des membres créés par le jeu de démonstration et des membres créés sans mot de passe explicite |
| `APP_DEMO_ADMIN_PASSWORD` | Mot de passe initial des administrateurs du jeu de démonstration |
| `SPRING_DATASOURCE_URL` | Facultatif ; défaut `jdbc:postgresql://localhost:5441/atelierconnect_db` |

Dans le terminal utilisé pour Docker et le backend, charger les variables avec le
script existant. Il conserve les valeurs déjà définies et demande les secrets
manquants sans les afficher ni les enregistrer dans un fichier :

```powershell
.\scripts\set-local-environment.ps1
```

Pour vérifier une configuration déjà chargée sans saisie interactive :

```powershell
.\scripts\set-local-environment.ps1 -NonInteractive
```

Utiliser les identifiants du volume PostgreSQL existant et conserver la clé JWT
privée prévue pour cette installation. Changer les variables de démonstration ne
réinitialise pas les mots de passe des comptes existants. Ne pas commiter les secrets.

## Docker : démarrer PostgreSQL

Le Compose utilise l'image prête à l'emploi `postgres:15` : **aucune compilation
Docker n'est nécessaire**. Il ne contient pas de services backend ou frontend ;
Java et Angular se compilent avec les commandes des sections suivantes.

Base : `atelierconnect_db`, conteneur : `atelierconnect-db`, volume persistant :
`atelierconnect-postgres-data` (préfixé par Compose), port hôte : `5441`.

Vérifier la configuration et l'état :

```powershell
docker compose config --quiet
docker compose ps
```

Si le conteneur existe et est arrêté, le réutiliser :

```powershell
docker compose start postgres
```

Uniquement pour une première installation sans ce conteneur :

```powershell
docker compose pull postgres
docker compose up -d postgres
```

Contrôler le démarrage, puis arrêter sans supprimer le conteneur ni ses données si nécessaire :

```powershell
docker compose ps
docker compose logs --tail 50 postgres
```

```powershell
docker compose stop postgres
```

## Backend : compiler et démarrer

Depuis le dossier backend, avec les variables chargées et PostgreSQL démarré.
Compiler le JAR sans lancer les tests qui nécessitent une base dédiée :

```powershell
.\mvnw.cmd -B -ntp -DskipTests clean package
```

Les tests complets sont exécutés par la procédure isolée ci-dessous. Pour démarrer
le backend avec Maven :

```powershell
.\mvnw.cmd spring-boot:run
```

Ou, à la place de Maven, démarrer le JAR compilé :

```powershell
java -jar .\target\atelierconnect-backend-0.0.1-SNAPSHOT.jar
```

Utiliser une seule de ces deux méthodes à la fois. Arrêter le serveur avec `Ctrl+C`.
`spring.jpa.hibernate.ddl-auto=update` est conservé ; aucun Flyway n'est utilisé.
Avant un démarrage sur des données existantes, suivre la procédure de sauvegarde du
[guide Windows](docs/phase-4/installation-windows.md).

Depuis une autre console, vérifier l'API :

```powershell
Invoke-RestMethod http://localhost:8082/api/ateliers/public
```

Backend : `http://localhost:8082` ; Swagger : `http://localhost:8082/swagger-ui.html`.
Le [guide IntelliJ](docs/phase-4/installation-windows.md) décrit aussi le lancement
de `com.atelierconnect.AtelierConnectApplication` avec une configuration privée.

## Frontend : installer, tester, compiler et démarrer

Dans **un second terminal ouvert à la racine Git**, pendant que le backend tourne :

```powershell
cd .\AtelierConnect\frontend
npm ci
npm test -- --maxWorkers=2
npm run build
npm start
```

`npm ci` installe les versions du lockfile ; Vitest exécute tous les tests avec deux
workers pour limiter la mémoire. `npm run build` produit le build de production
dans `dist/frontend/`. `npm start` lance le serveur de développement et son proxy.

Frontend : `http://localhost:4200`. `/api` est redirigé vers
`http://localhost:8082`. Arrêter Angular avec `Ctrl+C`.

## Validation complète sur bases dédiées

Revenir dans le dossier backend contenant `pom.xml`. Arrêter d'abord vos serveurs
Java et Angular avec `Ctrl+C` pour libérer `8082` et `4200` ; laisser Docker Desktop
actif. Ne pas lancer les tests Java avec les variables de la base applicative.

Dans l'environnement actuel, réutiliser le serveur PostgreSQL de test déjà présent :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/validate-fullstack.ps1 -TestContainer atelierconnect-refactor-test-20261101 -PostgresPort 5549
```

Sur une nouvelle installation, si le port de test `5549` est libre, le script peut
créer son serveur dédié par défaut :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/validate-fullstack.ps1
```

Le script exécute le wrapper Maven `clean verify` (dont les tests PostgreSQL de
concurrence), `npm ci`, Vitest, le build Angular et les six parcours Playwright.
Il démarre les vrais serveurs, contrôle la persistance après redémarrage du serveur
de test, puis arrête les processus Java/Angular qu'il a lancés. Il conserve les
bases et volumes, sans toucher à `atelierconnect_db`.

Rapports : `.local/validation/<date>/summary.json`, `target/surefire-reports/` et
`frontend/playwright-report/`. Après une installation réussie, l'option `-SkipInstall`
évite uniquement de réinstaller les dépendances ; elle n'ignore aucun test.
