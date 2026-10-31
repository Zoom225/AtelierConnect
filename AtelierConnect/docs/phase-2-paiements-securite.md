# Phase 2 — Vérification des paiements et des accès privés

## Périmètre et architecture préservée

- Java 21 / Spring Boot 3.5 / Spring Security / JWT / PostgreSQL.
- `spring.jpa.hibernate.ddl-auto=update` conservé ; aucune dépendance Flyway.
- Les tables physiques `reservations`, `paiements`, `matches` et leurs colonnes historiques sont conservées.
- Les modifications restent sur `phase-2/refactoring-backend` ; `master` et `develop` ne sont pas modifiées.

## Sérialisation des paiements

`PaiementServiceImpl.pay()` verrouille maintenant la ligne de la réservation avec
`ReservationRepository.lockReservationById()` **avant** de lire les statuts
`EN_ATTENTE` et `PAYE`. La requête native verrouille seulement la ligne
`reservations` afin d'éviter les jointures externes sous `FOR UPDATE`.

Ensuite le service verrouille l'atelier existant via
`AtelierRepository.findByIdForUpdate()` avant d'attribuer une place.

Cela protège deux scénarios :
1. Deux paiements distincts pour la sixième place : au plus un paiement confirmé.
2. Deux requêtes simultanées sur la même réservation : une seule confirmation.

Les tests d'intégration `PaiementConcurrencyPostgresTest` utilisent deux vrais
threads et une base PostgreSQL de test. Ils ne sont activés que par
`RUN_POSTGRES_CONCURRENCY_TEST=true`. Ne jamais lancer ces tests sur une base
contenant des données métier réelles.

## Autorisation et règles métier

- La consultation d'un atelier privé exige d'être organisateur, administrateur autorisé ou participant ayant une inscription non annulée.
- Une inscription annulée ne permet plus de consulter les détails privés.
- La modification d'un atelier vérifie les limites de réservation, le centre autorisé et la date future, comme la création.
- Les anciennes routes REST restent des alias provisoires destinés au frontend existant.

## Preuve CI

Workflow : `.github/workflows/atelierconnect-backend.yml`.

Exécution de référence :
https://github.com/Zoom225/AtelierConnect/actions/runs/41198701529

- Environnement : Java 21 et PostgreSQL 15, base éphémère `atelierconnect_ci`.
- Commande : `mvn -B -ntp -f AtelierConnect/pom.xml clean verify`.
- Résultat : **136 tests, 0 échec, 0 erreur, 0 ignoré — BUILD SUCCESS**.
- Les 2 tests de concurrence PostgreSQL ont réussi.

La validation ci-dessus concerne le commit `08e181a1`. Les commits publiés
après ce point doivent disposer d'un nouveau résultat CI vert avant intégration.

## Éléments à valider avant la fusion dans develop

- La validation CI du dernier commit sur la branche.
- Les éventuelles interactions entre paiement, annulation et scheduler sous forte concurrence.
- La mise à jour des données de démonstration encore intitulées « Padel ».
- La vérification des autorisations sur toutes les routes administrateur/participant.
- La rotation effective de toute clé JWT anciennement publiée.
- La compatibilité API avec Angular avant retrait des anciens noms.

**Attention :** le statut `PAYE` correspond actuellement à la validation du paiement
dans la logique applicative ; cette preuve ne démontre pas l'intégration d'un
prestataire de paiement réel.
