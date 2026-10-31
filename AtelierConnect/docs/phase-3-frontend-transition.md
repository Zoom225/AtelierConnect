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

## Tests et limites
Le workflow `.github/workflows/atelierconnect-backend.yml` exécute Java 21,
PostgreSQL 15, tests Spring Boot, tests Vitest et compilation Angular.
Une compilation verte ne constitue pas un test navigateur de bout en bout.

À valider avant fusion :
1. Parcours sur un vrai navigateur : connexion, catégories, création, invitation,
   paiement simulé, annulation, autorisations et conversion à J−2.
2. Compatibilité de tous les champs DTO pendant la suppression progressive
   des anciennes appellations et des imports Angular historiques.
3. Exposer en lecture seule le consentement enregistré dans le DTO atelier,
   afin que le bouton d'accord reste désactivé après rafraîchissement.
4. Vérifier les messages et l'accessibilité des écrans en français et en anglais.
5. L'ancienne clé JWT éventuellement exposée doit être révoquée séparément.

Important : le statut PAYE correspond à la validation applicative du paiement ;
il ne prouve pas une transaction avec un prestataire de paiement externe.
