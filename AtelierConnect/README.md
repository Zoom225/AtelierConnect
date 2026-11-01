# AtelierConnect

?tat du refactoring et preuves de validation : [rapport du 1er novembre 2026](docs/refactoring-final-2026-11-01.md).

Application de gestion et réservation d'ateliers collaboratifs multi-centres : backend Java 21 / Spring Boot 3.5, frontend Angular 21 et base PostgreSQL. Le backend utilise le domaine Participant / Atelier / Salle / Centre et conserve temporairement les anciennes routes REST pour assurer la transition Angular.

## Prérequis

Java 21, Docker Desktop, Node.js compatible Angular 21 et npm. Exécuter les commandes ci-dessous depuis `AtelierConnect/` dans Windows PowerShell.

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

Définir les variables dans chaque terminal qui lance Docker Compose ou le backend. Les commandes ci-dessous demandent les valeurs sans les afficher. Conserver les mêmes valeurs lors des redémarrages : le volume PostgreSQL conserve son mot de passe initial, et changer la clé JWT invalide les jetons existants. Garder ces valeurs dans un gestionnaire de secrets, jamais dans Git.

```powershell
$env:POSTGRES_USER = 'atelierconnect'
$secure = Read-Host 'POSTGRES_PASSWORD' -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try { $env:POSTGRES_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
$secure = Read-Host 'JWT_SECRET (au moins 32 octets)' -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try { $env:JWT_SECRET = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
$secure = Read-Host 'APP_DEMO_MEMBER_PASSWORD' -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try { $env:APP_DEMO_MEMBER_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
$secure = Read-Host 'APP_DEMO_ADMIN_PASSWORD' -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try { $env:APP_DEMO_ADMIN_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
```

Pour créer une nouvelle clé JWT aléatoire, la générer localement et la stocker dans le gestionnaire de secrets avant de fermer le terminal :

```powershell
$bytes = New-Object byte[] 64
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
```

La clé JWT précédemment publiée dans l'historique Git doit être révoquée/remplacée partout où elle a été utilisée. Les anciens mots de passe de démonstration publiés doivent aussi être remplacés pour les comptes déjà créés ; changer les variables ne réinitialise pas les mots de passe des comptes existants. L'initialisation de démonstration ne remplace plus les identifiants personnalisés.

## PostgreSQL

`docker-compose.yml` crée la base `atelierconnect_db`, le conteneur `atelierconnect-db` et le volume distinct `atelierconnect-postgres-data`. Le port hôte est `5441`. Aucun ancien volume n'est supprimé.

```powershell
docker compose config --quiet
docker compose up -d postgres
docker compose ps
```

Pour arrêter le conteneur en conservant les données : `docker compose down`. Ne pas utiliser `down -v` si les données doivent être conservées.

## Backend

`spring.jpa.hibernate.ddl-auto=update` reste actif. Depuis un terminal configuré avec les variables ci-dessus :

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

Backend : `http://localhost:8082` ; Swagger : `http://localhost:8082/swagger-ui.html`.

## Frontend

Depuis `AtelierConnect/frontend/` :

```powershell
npm ci
npm test
npm run build
npm start
```

Frontend : `http://localhost:4200`. Le proxy de développement redirige `/api` vers le backend sur le port `8082`.
