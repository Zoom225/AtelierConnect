# Préparation de la validation TFE AtelierConnect

État du 1er novembre 2026. Ces documents sont préparés sur
`phase-3/refactoring-frontend`. La PR #2 reste en brouillon vers `develop` ;
la branche `phase-4/validation-tfe` ne sera créée qu'après validation et intégration
de la phase 3. Aucun acquis n'est présenté comme officiellement validé par l'EPHEC.

## Dossier de preuve

- [Analyse fonctionnelle et cahier des charges](fonctionnel.md)
- [Architecture, UML, entité-association et schéma relationnel](architecture-donnees.md)
- [Installation Windows et démarrage IntelliJ/Angular](installation-windows.md)
- [Manuel participant/administrateur, démonstration et défense](manuel-demonstration.md)
- [API REST, authentification et autorisations](api-securite.md)
- [Rapport de tests, anomalies, limites et fonctionnalités manquantes](tests-limites.md)

Les diagrammes Mermaid se lisent directement dans GitHub ; ils utilisent le modèle
Java canonique et indiquent séparément les noms SQL historiques.

## Matrice de traçabilité des onze thèmes demandés

Les numéros ci-dessous correspondent aux onze thèmes de la demande ; la grille
officielle de l'établissement doit être confrontée à ce dossier par l'étudiant.
Les classes Java sont sous `src/main/java/com/atelierconnect`, les tests Java sous
`src/test/java/com/atelierconnect`, et Angular sous `frontend/src/app`.

| Thème | Classes et méthodes à montrer | Tests et preuves | Document / état restant |
| --- | --- | --- | --- |
| 1. Analyse fonctionnelle | `AtelierRules`, `AtelierServiceImpl.createAtelier`, `ReservationServiceImpl.create` | Cas participant/admin des quatre parcours Playwright ; critères UC du CDC | [fonctionnel](fonctionnel.md) ; faire valider le besoin par les parties prenantes |
| 2. Cahier des charges | `AtelierRules.delaiReservation`, `validateCentreAccessForOrganizer`, `validateBookingDelay` | `AtelierServiceTest`, `ReservationServiceTest`, tests de formulaire Angular | [fonctionnel](fonctionnel.md) ; arbitrer les règles historiques non spécifiées |
| 3. Modèle conceptuel et relationnel | `Participant`, `Centre`, `Salle`, `Atelier`, `Reservation`, `Paiement`, `Penalite`, `JourFermeture`, `Administrateur` | `LegacySchemaCompatibilityTest`, restauration d'une copie réelle | [architecture-donnees](architecture-donnees.md) ; revue du modèle avec l'encadrant |
| 4. Intégrité PostgreSQL | Repositories et FK JPA ; `AtelierRepository.findByIdForUpdate`, `ReservationRepository.lockReservationById` | `PaiementConcurrencyPostgresTest` : quatre courses ; empreinte persistante après redémarrage | [architecture-donnees](architecture-donnees.md) ; stratégie de reprise des données historiques |
| 5. Architecture trois couches | `SalleController.update` → `SalleServiceImpl.update` → `SalleRepository` ; DTO et mappers | `SalleWriteEndpointIntegrationTest`, compilations et démarrage réseau réel | [architecture-donnees](architecture-donnees.md) ; expliquer la frontière transactionnelle |
| 6. Authentification et autorisation | `JwtConfig`, `JwtAuthenticationFilter`, `SecurityConfig`, `CurrentParticipantService`, `AdminAuthorizationService` | `JwtConfigTest`, `JwtAuthenticationFilterTest`, `AdminAuthorizationServiceTest`, tests d'accès HTTP | [api-securite](api-securite.md) ; recette manuelle complète d'un admin SITE |
| 7. API REST sécurisée | Contrôleurs REST, `ParticipantAccessService`, `GlobalExceptionHandler` | Tests contrôleurs ; Swagger et refus 400/403/404 testés dans Chromium | [api-securite](api-securite.md) ; décider d'une éventuelle normalisation 401/409 |
| 8. Interface Angular | `MemberCreateAtelierPage.submit`, `MemberReservationsPage.pay/cancel/consentToPublicOpening`, pages d'administration | 74 tests Vitest, quatre parcours Chromium, build production | [manuel](manuel-demonstration.md) ; recette FR/EN, mobile et clavier |
| 9. Métier et erreurs | `ReservationServiceImpl.create/confirm`, `PaiementServiceImpl.pay`, `AtelierServiceImpl.checkAndConvertExpiredPrivateAteliers` | Dette, suspension, consentement, plafond privé et paiement testés ; erreurs JSON | [fonctionnel](fonctionnel.md), [rapport](tests-limites.md) ; frontières de dates à arbitrer |
| 10. Tests et CI/CD | `.github/workflows/atelierconnect-backend.yml`, `scripts/validate-fullstack.ps1` | Rapports Surefire, Vitest JSON, Playwright JSON/HTML ; trois jobs CI | [rapport](tests-limites.md) ; aucun déploiement continu de production configuré |
| 11. Documentation et défense | Ce dossier, README et scripts Windows | Démonstration reproductible ; résultats datés et limites explicites | [manuel](manuel-demonstration.md), [installation](installation-windows.md) ; captures et répétition orale à compléter |

## Passage à la phase 4

Le démarrage, les échanges HTTP, les données et les principaux parcours sont
vérifiés. L'intégration de phase 3 reste une décision de revue : recette manuelle,
stratégie pour les anciens ateliers et examen des limites doivent précéder la
sortie du brouillon puis la fusion explicite de la PR #2. Aucune fusion automatique.

