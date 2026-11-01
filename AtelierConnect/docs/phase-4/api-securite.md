# Référence API et sécurité

## Contrat et découverte

Base locale : `http://localhost:8082/api`. Depuis Angular, utiliser `/api` et le
proxy ; aucune URL absolue locale ne doit être incorporée dans les services de
production. Documentation générée : `/swagger-ui.html` et `/v3/api-docs`.

Les corps sont JSON. Les identifiants ci-dessous sont des exemples de structure,
pas des identifiants garantis dans une base existante.

```json
{"salleId": 1, "atelierDate": "2030-05-01T10:00:00", "atelierType": "PRIVE"}
```

La date réelle doit respecter le délai de la catégorie et les horaires du centre.
Cet exemple fixe ne constitue pas une requête utilisable telle quelle à toute date.

```json
{"atelierId": 1, "participantId": 2, "requesterId": 2}
```

`requesterId` reste requis par le DTO historique, mais le contrôleur utilise
l'identité authentifiée comme demandeur : un ID fourni par le client ne lui donne
pas l'autorisation d'agir pour un tiers.

```json
{"atelierId": 1, "inviteeMatricules": ["S10001", "L10001"]}
```

## Principales routes canoniques

| Méthode / route relative à `/api` | Accès / effet |
| --- | --- |
| POST `/participants/login` | Public ; matricule/password ; profil et token |
| POST `/auth/login` | Public ; email/password ; profil administratif et token |
| GET `/centres`, `/centres/{id}` | Public |
| POST/PUT/DELETE `/centres[/id]` | Administration globale selon opération |
| GET `/salles`, `/salles/{id}`, `/salles/centre/{id}` | Public ; centre chargé dans le DTO |
| POST/PUT/DELETE `/salles[/id]` | Administrateur, contrôle de centre ; PUT modifie le nom |
| GET `/ateliers/public` | Public |
| POST `/ateliers` | Participant ; création et réservation organisateur en attente |
| GET `/ateliers` | Administrateur, périmètre filtré |
| GET `/ateliers/centre/{id}` | Administrateur autorisé sur le centre |
| GET `/ateliers/organisateur/{id}` | Participant propriétaire |
| GET `/ateliers/{id}` | Participant/admin avec autorisation métier ; privé non exposé à un participant non autorisé |
| PUT `/ateliers/{id}` | Organisateur ; mêmes validations de date/créneau/centre |
| PATCH `/ateliers/{id}/cancel` | Organisateur |
| PATCH `/ateliers/{id}/consent-open-public` | Organisateur d'un atelier privé planifié à venir |
| PATCH `/ateliers/{id}/convert-public` | Administrateur ; consentement et délai requis |
| POST `/reservations` | Participant ; inscription EN_ATTENTE |
| POST `/reservations/private-invites` | Organisateur ; invitations privées |
| GET `/reservations/participant/{id}` | Participant propriétaire |
| GET `/reservations/atelier/{id}` | Organisateur/admin autorisé |
| GET `/reservations/{id}` | Propriétaire/admin autorisé |
| PATCH `/reservations/{id}/cancel` | Propriétaire autorisé ; libération éventuelle de place |
| POST `/paiements/reservation/{id}/pay` | Participant propriétaire ; validation simulée |
| GET `/paiements/reservation/{id}`, `/paiements/participant/{id}`, `/paiements/{id}` | Participant propriétaire |
| GET/POST/DELETE `/participants[/id]` et PUT `/participants/{id}` | Administration selon périmètre ; lecture individuelle également autorisée au propriétaire |
| GET `/participants/me` | Participant courant |
| GET `/participants/{id}/penalty`, `/participants/{id}/balance` | Contrôle d'accès individuel |
| Routes `/jours-fermeture` | Administrateur ; voir Swagger pour les variantes globales et par centre |

Les slashs entre verbes représentent des opérations séparées ; les corps PUT ne
sont pas identiques aux corps de création d'atelier. Le PUT Atelier utilise
`salleId`, `organisateurId`, `date`, `heureDebut`, `typeAtelier`.

## Authentification et autorisations

Le token se transmet dans `Authorization: Bearer <token privé>`. Ne pas commiter,
journaliser ou capturer sa valeur. BCrypt protège les mots de passe stockés.
`JwtConfig` exige au moins 32 octets pour la clé ; l'expiration configurée est
86400000 ms. Le claim `principalType` distingue ADMIN et MEMBER ; les services
résolvent le compte et vérifient propriétaire et centre.

`GLOBAL`/`SITE` sont les types d'administrateur ; PARTENAIRE/ABONNE/VISITEUR sont
des catégories participantes. `ROLE_ADMIN`/`ROLE_MEMBER` sont les autorités Spring.
Le changement de terminologie ne fusionne pas ces trois notions.

La chaîne Spring active CORS avant le contrôle d'accès : le navigateur peut envoyer
OPTIONS sans token. Une véritable écriture sans authentification reste refusée.
La politique MVC existante accepte toutes les origines, sans cookies de session
cross-origin ; une liste d'origines de production devra être définie lors du
déploiement. Aucun contrôle JWT n'a été désactivé pour résoudre CORS.

## États et erreurs réellement implémentés

| Code | Comportement observé |
| --- | --- |
| 200 | Lectures, login, modification, paiement, Swagger et préflight CORS |
| 201 | Création d'atelier, salle, centre ou réservation |
| 204 | Annulations et suppressions selon endpoint |
| 400 | Validation, JSON invalide, identifiants invalides, doublon et règle métier non respectée |
| 403 | Écriture anonyme protégée et refus d'autorisation ; le filtre peut répondre avant le gestionnaire JSON |
| 404 | Ressource absente, notamment après suppression d'un objet créé par le test |
| 500 | Erreur inattendue ; message public générique et type d'exception journalisé |

Ne pas annoncer 401 pour tout échec de login ou 409 pour tout conflit : ce n'est
pas la convention actuelle. Les erreurs métier sont mappées sur 400 et la chaîne
de sécurité renvoie 403 pour les accès anonymes testés. Une normalisation 401/409
serait une évolution de contrat à traiter avec les consommateurs.

`ErrorResponse` expose `status`, `message`, `timestamp`, et éventuellement `errors`
par champ pour la validation. Exemple :

```json
{"status":400,"message":"Identifiants invalides.","timestamp":"2026-11-01T10:00:00"}
```

## Compatibilité et limites

Les routes historiques `/matches`, `/membres`, `/terrains`, `/sites` et certains
sous-chemins `match/membre/site` restent disponibles. Les DTO acceptent ou exposent
les anciens champs, avec les propriétés canoniques. Les valeurs SQL participantes
GLOBAL/SITE/LIBRE sont converties sans toucher aux données existantes. Les clés
de stockage historiques sont lues pour restaurer les sessions.

Les JWT stockés dans le navigateur n'ont pas de révocation centralisée ni refresh
token dans le périmètre réalisé. Une capture bancaire, des remboursements externes,
des notifications et une limitation de débit globale ne sont pas implémentés.
Cette documentation n'est pas une homologation de sécurité de production.

