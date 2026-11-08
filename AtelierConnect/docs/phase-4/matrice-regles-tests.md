# Matrice règles métier et tests de recette

Les noms désignent les classes/méthodes du dépôt, sous `src/test/java/com/atelierconnect`
ou `frontend`. Les tests PostgreSQL sont activés uniquement sur une base dédiée avec
`RUN_POSTGRES_CONCURRENCY_TEST=true`. H2 ne constitue pas la preuve des verrous.

| Règle | Implémentation | Preuve automatisée |
| --- | --- | --- |
| Création aux bornes 30 / 15 / 7 | `AtelierServiceImpl.validateBookingDelay` | `AtelierBoundaryTest.acceptsLastAllowedDayWithCanonicalPricesAndPendingPlace`, paramétré sur les trois catégories |
| Création à borne +1 refusée | Même méthode | `AtelierBoundaryTest.refusesDayAfterCategoryLimit`, trois catégories |
| Inscription aux bornes 30 / 15 / 7 | `ReservationServiceImpl.create` | `ReservationBookingBoundaryTest.lastAllowedBookingDayRemainsPending`, trois catégories |
| Inscription à borne +1 refusée | Même méthode | `ReservationBookingBoundaryTest.dayAfterBookingLimitIsRejected`, trois catégories |
| PARTENAIRE multi-centres | Contrôle d'accès de création | `AtelierBoundaryTest.partenaireCanOrganizeOutsideItsCentre` |
| ABONNE uniquement son centre | Services Atelier/Réservation | `AtelierBoundaryTest.abonneCannotOrganizeOutsideItsCentre`, `ReservationServiceTest` ; absence de rattachement refusée sans NPE dans `ReservationBookingBoundaryTest` |
| VISITEUR centre autorisé | Services Atelier/Réservation | `AtelierBoundaryTest.visiteurCannotUseCentreClosedToVisitors`, `ReservationServiceTest.visiteurRefuseSiCentreFermeAuxVisiteurs` |
| Date passée interdite | Service Atelier | `AtelierBoundaryTest.refusesPastDate` |
| 120 minutes, 90 EUR et 15 EUR | `AtelierRules`, création | `AtelierBoundaryTest.acceptsLastAllowedDayWithCanonicalPricesAndPendingPlace` ; formulaire et parcours Chromium |
| Fin après minuit dans un centre de jour | Validation des horaires | `AtelierBoundaryTest.refusesWorkshopEndingAfterMidnightInDaytimeCentre` |
| Pause exacte de 20 minutes, pas 19 | Verrou de salle + requête de chevauchement | `AtelierConcurrencyPostgresTest.roomBreakAcceptsTwentyMinutesAndRejectsNineteenOnBothSides` |
| Huit créations sur un même créneau | `SalleRepository.findByIdForUpdate`, transaction de création | `AtelierConcurrencyPostgresTest.eightSimultaneousCreationsOnlyAllocateOneRoomSlot` : un succès, sept refus |
| Deux déplacements vers un même créneau | Verrous atelier puis salle cible | `AtelierConcurrencyPostgresTest.twoMovesToSameRoomSlotOnlyOneSucceeds` |
| Six places confirmées maximum | Verrou et rafraîchissement atelier lors du paiement | `PaiementConcurrencyPostgresTest.deuxParticipantsPourDernierePlaceUnSeulPaiementReussit` |
| Privé : organisateur + cinq invités | Comptage des réservations actives sous verrou atelier | `ReservationServiceTest.refusesSixthPrivateGuestEvenBeforePayment`, course privée dans `PaiementConcurrencyPostgresTest` |
| Intention EN_ATTENTE sans place attribuée | Création de réservation/paiement | Tests de bornes, `ReservationServiceTest`, parcours Chromium |
| Confirmation après paiement valide | `ReservationServiceImpl.confirm`, `PaiementServiceImpl.pay` | `ReservationServiceTest.refusesConfirmationWithoutValidatedPayment`, tests de paiement et navigateur |
| Double paiement refusé | Verrou réservation et contrôle PAYE | Test unitaire paiement et `PaiementConcurrencyPostgresTest.doubleClicSurMemeReservationNePaieQuUneFois` |
| Annulation / remboursement simulé | Service Réservation, décrément sous verrou | `ReservationServiceTest`, course paiement/annulation PostgreSQL et parcours Chromium |
| Dette ou suspension | `ParticipantService`, services Atelier/Réservation | Tests existants `AtelierServiceTest`, `ReservationServiceTest`, `ParticipantServiceTest` |
| Suspension automatique du privé incomplet à J−1 | Scheduler, services Atelier/Participant ; verrou atelier et motif contenant son numéro | `IncompletePrivatePenaltyIntegrationTest` : échéance, complétude, exclusions, répétitions, concurrence, état rafraîchi et refus JWT public/privé |
| Dette restante sur six places et règlement autonome | `DetteService`, verrou atelier puis participant, journal avec référence unique | `DebtSettlementIntegrationTest` : 0/1/2/4/5/6 places payées, absence de double imputation, règlement/JWT, rejeu, concurrence et deux conditions d'accès |
| Consentement explicite et daté | Service Atelier et scheduler | `AtelierBoundaryTest.booleanConsentWithoutTimestampDoesNotAuthorizeOpening`, tests scheduler existants, persistance navigateur |
| Accord limité au privé sélectionné | `MemberReservationsPage.consentToPublicOpening` | Régression Vitest avec deux ateliers privés |
| Aucun accès au paiement/réservation d'autrui | Contrôleurs et services d'identité | `AuthorizationJwtIntegrationTest.memberCannotReadPayOrCancelSomeoneElsesReservation` ; état des données inchangé après refus |
| Ancien prix de salle non utilisé pour un nouvel atelier | Constante tarifaire Angular | Test `MemberCreateAtelierPage` ouvrant réellement le select avec une salle à 60 EUR et vérifiant l'affichage à 90 EUR |

## Échéances et limites de validation

- **J−1, public** : le scheduler annule les réservations EN_ATTENTE correspondant
  à demain et impute leur prix unitaire à l'organisateur. Pour le privé incomplet,
  il applique une seule imputation du prix total moins les places déjà payées ;
  les invitations impayées n'ajoutent pas une seconde dette.
- **Fin de suspension, corrigée le 7 novembre** : la comparaison est désormais
  inclusive (`dateFin >= aujourd'hui`). Une pénalité reste active pendant son
  dernier jour ; les tests d'intégration couvrent hier, aujourd'hui et demain.
- **Privé incomplet, raccordé le 7 novembre** : à J−1, moins de six places
  confirmées sur un atelier encore privé et planifié déclenchent une suspension
  jusqu'à J+7 inclus. Le contrôle récurrent est idempotent par atelier, sous verrou.
  La règle complémentaire impute les places encore impayées à l'organisateur,
  sans double facturation par invitation et sans ouverture publique forcée.
- **J−2** : les tests appellent le traitement métier avec les dates contrôlées ;
  l'attente d'un vrai déclenchement à minuit en environnement cible reste une recette
  d'exploitation non effectuée. Une plage nocturne complète et le multi-fuseau ne
  sont pas réputés validés par le refus d'un dépassement d'horaire de jour.
