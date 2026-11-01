# Phase 3 — Migration Angular vers AtelierConnect

## Objet
Cette branche transforme l'interface Angular 21 du projet PadellAout en plateforme
AtelierConnect (ateliers collaboratifs multi-centres). Le backend validé en phase 2
reste la référence métier : Java 21, Spring Boot, PostgreSQL, JWT.

## Règles métier affichées
- Catégories : PARTENAIRE (30 jours), ABONNÉ (15 jours, centre affecté) et
  VISITEUR (7 jours, centres ouverts aux visiteurs).
- Atelier de 120 minutes, pause de 20 minutes, 6 participants maximum.
- Tarif total de 90 €, soit 15 € par personne.
- Atelier privé : organisateur + jusqu'à cinq invités.
- Ouverture au public à J−2 : uniquement avec consentement explicite et daté
  de l'organisateur. L'interface demande une confirmation avant d'appeler l'API.

## API transitoire
Les services Angular modernisés sont :
- `AteliersApiService` : `/api/ateliers`
- `ParticipantsApiService` : `/api/participants`
- `CentresApiService` : `/api/centres`
- `SallesApiService` : `/api/salles`

Les anciens fichiers de services réexportent temporairement les nouveaux
services sans instancier de logique en double. Ils peuvent être supprimés
lorsque tous les imports auront été migrés.

`MembreResponse.categorie` utilise PARTENAIRE, ABONNE, VISITEUR lorsqu'il est
présent. `typeMembre` (GLOBAL, SITE, LIBRE) reste reconnu pour les anciens
comptes et les formulaires de transition. Les requêtes d'administration
transmettent les deux valeurs correspondantes.

## Préservation de la base
- Pas de Flyway.
- `spring.jpa.hibernate.ddl-auto=update` conservé.
- Anciennes tables/colonnes SQL et données existantes préservées.
- `master` n'est pas modifiée ; la branche ne se fusionne que dans `develop`
  après tests et revue.

## Preuves de validation — 31 octobre 2026
Le workflow `.github/workflows/atelierconnect-backend.yml` valide :
- **143 tests Java** réussis, 0 échec (Java 21, PostgreSQL 15).
- **70 tests Angular/Vitest** réussis, 0 échec, build Angular réussi.
- **1 parcours navigateur Chromium** réussi (vrai Spring Boot + Angular,
  base PostgreSQL temporaire, jamais les données de production).

Exécution GitHub Actions :
https://github.com/Zoom225/AtelierConnect/actions/runs/41203981112

Test : `frontend/e2e/atelierconnect-fullstack.spec.ts`, Playwright isolé
dans `frontend/e2e/package.json`.

Parcours réellement vérifiés :
1. Liste publique des ateliers et refus d'accès anonyme aux routes protégées.
2. Authentification de deux participants (PARTENAIRE et ABONNÉ).
3. Création d'un atelier PRIVÉ dans un centre de Bruxelles.
4. Invitation d'un autre participant par matricule.
5. Contrôle des permissions : accès privé d'un non-invité refusé,
   consultation des ateliers d'un autre organisateur refusée.
6. Paiement applicatif des deux réservations, interdiction pour un autre
   participant de payer celle de l'organisateur, compteur de places correct.
7. Annulation de l'inscription payée d'un invité : paiement marqué REMBOURSE,
   compteur de places décrémenté et deuxième annulation refusée.
8. Consentement explicite réservé à l'organisateur et horodaté côté backend.
9. Conversion vers un atelier public refusée avant J−2.
10. Connexion dans Chromium, consultation de l'atelier et maintien du consentement
   après rechargement complet de la page.

### Anomalies mises au jour par l'intégration réelle
- HTTP 500 `GET /api/salles` : `LazyInitializationException` corrigée par
  `@EntityGraph(site)` sur les lectures de `SalleRepository`.
- HTTP 500 `GET /api/reservations/participant/{id}` : associations chargées
  explicitement dans les requêtes de liste ; transaction de lecture limitée
  à l'endpoint de détail.
- Concurrence sur la sixième place : le chargement JPA anticipé rendait possible
  la lecture d'un ancien compteur. La transaction recharge désormais le compteur
  après le verrou `FOR UPDATE`. Les 3 tests PostgreSQL concurrents sont verts.

### Restant avant validation finale de la phase 3
1. Tester manuellement l'ensemble des interfaces administrateur, l'édition des
   ateliers et la fermeture des centres dans un navigateur.
2. Tester le passage effectif PRIVE → PUBLIC à J−2 avec le scheduler et
   le consentement, ainsi que les annulations et remboursements.
3. Vérifier l'accessibilité clavier, les messages d'erreur et les traductions
   françaises/anglaises.
4. Finaliser le retrait des noms internes et alias transitoires sans rompre
   les clients existants.
5. Révoquer toute ancienne clé JWT éventuellement publiée.

La restitution REMBOURSE est un état comptable simulé : aucune transaction ni remboursement bancaire réel n'est effectué.

Le résultat CI ne remplace pas la vérification métier par un utilisateur réel.
La PR #2 reste en brouillon : ne pas fusionner dans `develop` avant cette revue.

Important : le statut `PAYE` correspond à la **validation applicative** du
paiement ; il ne démontre pas une transaction avec un prestataire réel.
