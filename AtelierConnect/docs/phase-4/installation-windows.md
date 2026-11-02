# Installation Windows, IntelliJ IDEA et Angular

## Versions et structure vérifiées

Depuis la racine Git, le backend est `AtelierConnect/pom.xml`, les sources sont
`AtelierConnect/src`, le frontend est `AtelierConnect/frontend` et Compose est
`AtelierConnect/docker-compose.yml`.

Vérifiés localement : Java 21.0.9 ; wrapper Maven 3.9.16 ; Maven système 3.9.15 ;
Node 24.19.0 ; npm 11.17.0 ; Angular core 21.2.9 ; CLI 21.2.8 ; TypeScript 5.9.3 ;
Docker Engine 29.4.2 ; PostgreSQL 15.19 de test.

Node 22 >=22.12 convient aussi. Node 24 est accepté par les `engines` des paquets
Angular installés ; il n'a donc pas été rétrogradé. Le champ `packageManager`
indique npm 11.12.1 ; npm 11.17.0 a installé et validé le lockfile sans contournement
de peer dependencies. Le lockfile principal est inchangé.

Ouvrir un terminal PowerShell dans le dossier backend `AtelierConnect` :

```powershell
java -version
.\mvnw.cmd -version
node --version
npm --version
docker version
```

## Configuration privée

Depuis le dossier backend, charger les variables dans le processus courant :

```powershell
.\scripts\set-local-environment.ps1
```

Le script relit une configuration privée existante ou demande les valeurs
manquantes sans les afficher. Il ne crée pas de nouveau fichier de secrets.
L'ancienne option `-ConfigureIntelliJ` est désactivée. Le fichier local existant
`.run/AtelierConnect_Local.private.run.xml` est ignoré par Git et ne doit jamais
être partagé. Les variables déjà présentes dans le terminal sont conservées.

Les variables requises sont `POSTGRES_USER`, `POSTGRES_PASSWORD`, `JWT_SECRET`
(au moins 32 octets UTF-8), `APP_DEMO_MEMBER_PASSWORD`. L'URL JDBC par défaut est
`jdbc:postgresql://localhost:5441/atelierconnect_db`. `APP_DEMO_ADMIN_PASSWORD`
n'est plus requis par l'initialiseur : les administrateurs de démonstration
utilisent leur propre matricule comme mot de passe, enregistré en BCrypt.

Un fichier `.env` n'est pas chargé automatiquement par Spring Boot. Ne publier
aucun secret, fichier privé ou journal. Changer une variable ne change pas le
mot de passe d'un rôle PostgreSQL ou d'un compte déjà présent.

## Démarrage simplifié (Docker + backend + frontend)

Depuis le dossier backend `AtelierConnect`, le lanceur sans secrets suivis
permet de charger les variables dans **le même processus** que Maven :

```powershell
.\scripts\run-local.ps1 -Component Backend -Build
```

Le lanceur appelle `set-local-environment.ps1` pour les variables absentes,
demande les valeurs privées sans les afficher, réutilise le conteneur
`atelierconnect-db` s'il existe et ne supprime pas son volume.
`-Build` effectue le packaging **sans exécuter de tests** : les tests d'intégration
doivent rester sur la base de validation dédiée via `validate-fullstack.ps1`.
Si les anciennes valeurs PostgreSQL sont différentes, fournir les valeurs
réellement utilisées par le volume existant. Changer une variable d'environnement
ne change pas le mot de passe d'un rôle PostgreSQL déjà créé.

Dans un **second terminal** ouvert au même endroit :

```powershell
.\scripts\run-local.ps1 -Component Frontend -Build
```

Le frontend écoute sur http://localhost:4200 et relaie les requêtes `/api` au
backend sur http://localhost:8082. Après la première compilation, retirer
`-Build` pour les lancements rapides.

**Attention à la flèche verte IntelliJ :** lancer l'application directement
avec une configuration Application ne charge pas les variables PowerShell.
Utiliser `run-local.ps1` depuis le terminal IntelliJ, ou renseigner une
configuration Run/Debug privée avec les variables requises. Ne pas enregistrer
les secrets dans une configuration Git partagée.

## PostgreSQL existant

Depuis le dossier contenant le Compose :

```powershell
docker compose config --quiet
docker compose ps
```

Si le service existe et est arrêté : `docker compose start postgres`.
S'il n'existe pas encore : `docker compose up -d postgres`.
Ne pas recréer un service existant pour changer son mot de passe. Ne pas utiliser
de commande supprimant le volume. Le volume garde ses identifiants initiaux.

Le service applicatif est `atelierconnect-db`, base `atelierconnect_db`, port
publié `5441:5432`, volume `atelierconnect-postgres-data` préfixé par Compose.
Sur la machine auditée, le conteneur a été créé depuis une autre copie du dépôt :
il a été réutilisé, sans changement de volume ni de configuration.

Avant un premier démarrage Hibernate sur une base existante, sauvegarder :

```powershell
New-Item -ItemType Directory .local\backups -Force
docker exec atelierconnect-db pg_dump -U $env:POSTGRES_USER -d atelierconnect_db -Fc -f /tmp/atelierconnect-before-start.dump
docker cp atelierconnect-db:/tmp/atelierconnect-before-start.dump .local/backups/
```

Contrôler les codes de sortie. Les sauvegardes contiennent des données privées et
restent locales. Les tests et les essais de migration utilisent une autre base.

## Démarrage backend et IntelliJ

Importer **le pom backend** dans IntelliJ comme projet Maven. Définir Project SDK,
Maven importer et Maven runner sur Java 21. Activer l'annotation processing si
l'IDE ne reconnaît pas Lombok, puis recharger Maven.

Sélectionner la configuration privée existante **AtelierConnect — Local** si
elle est disponible. Sinon, préparer une configuration Run/Debug privée avec
la classe `com.atelierconnect.AtelierConnectApplication`, Java 21, le module
backend et les variables requises. Le répertoire de travail doit être le dossier
contenant `pom.xml`. Le script ne génère pas de configuration contenant des secrets.

Les variables d'un terminal ne modifient pas une instance IntelliJ déjà démarrée :
c'est la cause du placeholder `JWT_SECRET` manquant dans l'ancien lancement.

Le terminal IntelliJ constitue une alternative reproductible :

```powershell
.\scripts\set-local-environment.ps1
.\mvnw.cmd spring-boot:run
```

La connexion JDBC et le démarrage ont été vérifiés sur une copie PostgreSQL restaurée. Utiliser
Maven système si le wrapper ne peut pas télécharger sa distribution, après
`mvn -version`. Ne pas changer Spring Boot, Java ou les ports au hasard.

## Démarrage Angular

Dans un second terminal :

```powershell
cd frontend
npm ci
npm start
```

Ouvrir `http://localhost:4200`. Le proxy redirige `/api` vers
`http://localhost:8082`. Conserver les URL relatives dans les services ; en production,
l'hébergement devra router `/api` vers Spring Boot.

Endpoints de contrôle : `http://localhost:8082/api/ateliers/public`,
`http://localhost:8082/api/centres`, `http://localhost:8082/api/salles`,
`http://localhost:8082/swagger-ui.html` et `http://localhost:8082/v3/api-docs`.
Ces URL ont répondu durant les vérifications ; les serveurs de test sont arrêtés
automatiquement à la fin du script de validation.

## Validation complète isolée

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/validate-fullstack.ps1
```

Le script réserve les ports applicatifs libres, crée un serveur PostgreSQL de test
sur 5549 si nécessaire et deux bases dédiées par exécution. Il ne cible jamais
`atelierconnect_db`. Il enchaîne wrapper `clean verify`, npm ci, Vitest, build,
installation Chromium, vrais serveurs, six parcours et contrôle de persistance
après redémarrage du **serveur PostgreSQL de test**. Il ne supprime ni base ni volume.
Les bases de preuve s'accumulent volontairement ; leur rétention doit être gérée
séparément, sans toucher à la base applicative.

Sur la machine actuelle, réutiliser le serveur dédié déjà existant :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/validate-fullstack.ps1 -TestContainer atelierconnect-refactor-test-20261101 -PostgresPort 5549
```

`-SkipInstall` permet une nouvelle validation après une installation réussie ; il
n'ignore pas les tests. Ne pas passer le nom du conteneur applicatif. Le script
refuse un serveur dont la base initiale n'est pas identifiée comme test/validation.
Les rapports sont dans `.local/validation/<date>/summary.json`, Surefire et le rapport
HTML Playwright. Un avertissement natif sur stderr ne remplace pas le code de sortie.

Ne pas lancer `clean verify` avec les variables de la base applicative : certains
tests démarrent Spring Boot et son initialiseur. Le script isole ces variables et
les restaure à la sortie. Les quatre tests concurrents sont activés avec
`RUN_POSTGRES_CONCURRENCY_TEST=true` uniquement sur la base dédiée.

## Diagnostic rapide

| Symptôme | Contrôle / correction |
| --- | --- |
| Placeholder POSTGRES/JWT manquant | Exécuter le helper dans le même terminal ; config privée séparée dans IntelliJ |
| Connection refused | Docker sain, bon port 5441, base et URL JDBC ; ne pas changer arbitrairement le port |
| Authentication failed PostgreSQL | Utiliser les identifiants initiaux du volume ; ne pas supprimer le volume |
| Port 8082/4200 occupé | `Get-NetTCPConnection` et identifier le propriétaire ; ne pas tuer un serveur inconnu |
| Token rejeté | Se reconnecter avec la bonne identité et la bonne clé ; ne pas désactiver les contrôles |
| CORS | Vérifier OPTIONS puis requête réelle ; la sécurité CORS est activée, JWT reste obligatoire pour écrire |
| LazyInitializationException | Vérifier transaction et graphes JPA ; ne pas réactiver Open Session in View pour cacher l'erreur |
| Import Angular ancien | Recharger la branche, npm ci puis tests ; ne pas masquer les conflits avec --force |

## Controle reel du backend depuis Windows

Pour une base existante, `run-local.ps1` desactive l'initialiseur de donnees
(`APP_DATA_INITIALIZE=false`) et valide le schema sans le modifier
(`SPRING_JPA_HIBERNATE_DDL_AUTO=validate`). Les comptes et donnees existants
ne sont donc pas reinitialises au demarrage. Le controle JDBC utilise le pilote
Maven du backend et compare le serveur joint au conteneur existant. Seul un
echec SQLSTATE 28P01 propose de saisir le mot de passe existant.

```powershell
./scripts/run-local.ps1 -NonInteractive
```

Pour verifier un lancement dont la sortie a ete redirigee dans un journal :

```powershell
./scripts/verify-backend.ps1 -StartupLog .local/diagnostics/backend.log
```

Le controle exige `Started AtelierConnectApplication`, OpenAPI HTTP 200 avec
un document JSON 3.x et des chemins, la redirection Swagger et sa page finale,
les ressources JS/CSS et swagger-config HTTP 200, puis le meme PID actif.
Il utilise curl.exe depuis Windows, sans proxy pour la boucle locale.

Pour executer les tests backend uniquement, avec le conteneur de validation
existant, identifie par son label et publie sur 5549 :

```powershell
./scripts/test-backend-isolated.ps1
```

Chaque execution cree une nouvelle base de test, active les tests concurrents
PostgreSQL, conserve le conteneur et les volumes, puis restaure les variables
du terminal. Aucun test ne cible atelierconnect_db. Resultats locaux :
`.local/diagnostics/backend-tests.log` et `backend-tests-summary.json`.
