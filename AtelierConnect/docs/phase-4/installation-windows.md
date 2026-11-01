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

```powershell
.\scripts\set-local-environment.ps1
```

Le script complète les variables manquantes dans ce terminal, demande les secrets
sans les afficher et ne les écrit dans aucun fichier. Il conserve les variables
déjà définies. `-NonInteractive` vérifie la configuration sans poser de question.
Ne transmettre aucune valeur secrète dans un ticket, une capture ou le dépôt.

| Variable | Valeur attendue |
| --- | --- |
| POSTGRES_USER / POSTGRES_PASSWORD | Identifiants du volume PostgreSQL existant ; ne pas les remplacer pour faire disparaître une erreur de connexion |
| SPRING_DATASOURCE_URL | Défaut `jdbc:postgresql://localhost:5441/atelierconnect_db` |
| JWT_SECRET | Clé privée existante, au moins 32 octets ; changer la clé invalide les jetons signés auparavant |
| APP_DEMO_MEMBER_PASSWORD | Mot de passe initial des nouveaux comptes de démonstration / créations sans mot de passe explicite |
| APP_DEMO_ADMIN_PASSWORD | Mot de passe initial des administrateurs de démonstration |

Une variable APP_DEMO différente ne réinitialise pas les mots de passe des comptes
existants. Utiliser leur mot de passe actuel pour se connecter. Pour une toute
première installation, créer et conserver les secrets dans un gestionnaire privé.
Un fichier `.env` n'est pas automatiquement chargé par Spring Boot : exporter les
variables dans le processus qui lance Java.

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

Configuration Application : classe `com.atelierconnect.AtelierConnectApplication`,
classpath du module backend, working directory du dossier contenant `pom.xml`.
Renseigner les six variables dans une configuration privée, non partagée dans Git.
Les variables d'un terminal ne modifient pas une instance IntelliJ déjà démarrée.

Le terminal IntelliJ constitue une alternative reproductible :

```powershell
.\scripts\set-local-environment.ps1
.\mvnw.cmd spring-boot:run
```

Le wrapper a été exécuté réellement contre une copie PostgreSQL restaurée. Utiliser
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
