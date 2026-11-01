# Inventaire du refactoring du 1er novembre 2026

Diff de `8c7be9d` ? `2b1c988` ; M = modifi?, A = ajout?, D = supprim?, R = renomm? (similarit? Git). Les suppressions et ajouts de mod?les correspondent ?galement ? leur r??criture sous les nouveaux noms.

```text
M	AtelierConnect/frontend/.gitignore
M	AtelierConnect/frontend/e2e/atelierconnect-fullstack.spec.ts
M	AtelierConnect/frontend/src/app/app.html
M	AtelierConnect/frontend/src/app/app.spec.ts
M	AtelierConnect/frontend/src/app/app.ts
M	AtelierConnect/frontend/src/app/core/api/ateliers-api.service.ts
M	AtelierConnect/frontend/src/app/core/api/auth-api.service.ts
M	AtelierConnect/frontend/src/app/core/api/centres-api.service.ts
M	AtelierConnect/frontend/src/app/core/api/jours-fermeture-api.service.ts
D	AtelierConnect/frontend/src/app/core/api/matches-api.service.ts
D	AtelierConnect/frontend/src/app/core/api/membres-api.service.ts
M	AtelierConnect/frontend/src/app/core/api/paiements-api.service.ts
M	AtelierConnect/frontend/src/app/core/api/participants-api.service.ts
M	AtelierConnect/frontend/src/app/core/api/reservations-api.service.ts
M	AtelierConnect/frontend/src/app/core/api/salles-api.service.ts
D	AtelierConnect/frontend/src/app/core/api/sites-api.service.ts
D	AtelierConnect/frontend/src/app/core/api/terrains-api.service.ts
M	AtelierConnect/frontend/src/app/core/auth/admin-session.service.spec.ts
M	AtelierConnect/frontend/src/app/core/auth/admin-session.service.ts
M	AtelierConnect/frontend/src/app/core/auth/auth-session.service.ts
M	AtelierConnect/frontend/src/app/core/auth/auth.service.ts
M	AtelierConnect/frontend/src/app/core/auth/member-session.service.spec.ts
M	AtelierConnect/frontend/src/app/core/auth/member-session.service.ts
M	AtelierConnect/frontend/src/app/core/guards/admin.guard.spec.ts
M	AtelierConnect/frontend/src/app/core/guards/member.guard.spec.ts
M	AtelierConnect/frontend/src/app/core/i18n/translations.ts
M	AtelierConnect/frontend/src/app/core/interceptors/auth-token.interceptor.spec.ts
M	AtelierConnect/frontend/src/app/core/interceptors/auth-token.interceptor.ts
M	AtelierConnect/frontend/src/app/features/admin/admin.routes.ts
R058	AtelierConnect/frontend/src/app/features/admin/pages/admin-matches.page.ts	AtelierConnect/frontend/src/app/features/admin/pages/admin-ateliers.page.ts
R063	AtelierConnect/frontend/src/app/features/admin/pages/admin-sites.page.spec.ts	AtelierConnect/frontend/src/app/features/admin/pages/admin-centres.page.spec.ts
R077	AtelierConnect/frontend/src/app/features/admin/pages/admin-sites.page.ts	AtelierConnect/frontend/src/app/features/admin/pages/admin-centres.page.ts
M	AtelierConnect/frontend/src/app/features/admin/pages/admin-home.page.ts
M	AtelierConnect/frontend/src/app/features/admin/pages/admin-jours-fermeture.page.spec.ts
M	AtelierConnect/frontend/src/app/features/admin/pages/admin-jours-fermeture.page.ts
M	AtelierConnect/frontend/src/app/features/admin/pages/admin-login.page.ts
R054	AtelierConnect/frontend/src/app/features/admin/pages/admin-members.page.spec.ts	AtelierConnect/frontend/src/app/features/admin/pages/admin-participants.page.spec.ts
R084	AtelierConnect/frontend/src/app/features/admin/pages/admin-members.page.ts	AtelierConnect/frontend/src/app/features/admin/pages/admin-participants.page.ts
A	AtelierConnect/frontend/src/app/features/admin/pages/admin-salles.page.spec.ts
R071	AtelierConnect/frontend/src/app/features/admin/pages/admin-terrains.page.ts	AtelierConnect/frontend/src/app/features/admin/pages/admin-salles.page.ts
D	AtelierConnect/frontend/src/app/features/admin/pages/admin-terrains.page.spec.ts
M	AtelierConnect/frontend/src/app/features/home/pages/landing.page.ts
M	AtelierConnect/frontend/src/app/features/member/member.routes.ts
R063	AtelierConnect/frontend/src/app/features/member/pages/member-create-match.page.spec.ts	AtelierConnect/frontend/src/app/features/member/pages/member-create-atelier.page.spec.ts
R073	AtelierConnect/frontend/src/app/features/member/pages/member-create-match.page.ts	AtelierConnect/frontend/src/app/features/member/pages/member-create-atelier.page.ts
M	AtelierConnect/frontend/src/app/features/member/pages/member-home.page.spec.ts
M	AtelierConnect/frontend/src/app/features/member/pages/member-home.page.ts
M	AtelierConnect/frontend/src/app/features/member/pages/member-payments.page.spec.ts
M	AtelierConnect/frontend/src/app/features/member/pages/member-payments.page.ts
M	AtelierConnect/frontend/src/app/features/member/pages/member-profile.page.ts
R055	AtelierConnect/frontend/src/app/features/member/pages/member-public-matches.page.spec.ts	AtelierConnect/frontend/src/app/features/member/pages/member-public-ateliers.page.spec.ts
R064	AtelierConnect/frontend/src/app/features/member/pages/member-public-matches.page.ts	AtelierConnect/frontend/src/app/features/member/pages/member-public-ateliers.page.ts
M	AtelierConnect/frontend/src/app/features/member/pages/member-reservations.page.spec.ts
M	AtelierConnect/frontend/src/app/features/member/pages/member-reservations.page.ts
M	AtelierConnect/frontend/src/app/shared/models/atelier-rules.ts
A	AtelierConnect/frontend/src/app/shared/models/atelier.model.ts
M	AtelierConnect/frontend/src/app/shared/models/auth.model.ts
R059	AtelierConnect/frontend/src/app/shared/models/site-terrain.model.ts	AtelierConnect/frontend/src/app/shared/models/centre-salle.model.ts
M	AtelierConnect/frontend/src/app/shared/models/enums.model.ts
D	AtelierConnect/frontend/src/app/shared/models/match.model.ts
D	AtelierConnect/frontend/src/app/shared/models/membre.model.ts
R092	AtelierConnect/frontend/src/app/shared/models/membre.model.spec.ts	AtelierConnect/frontend/src/app/shared/models/participant.model.spec.ts
A	AtelierConnect/frontend/src/app/shared/models/participant.model.ts
M	AtelierConnect/frontend/src/app/shared/models/reservation.model.ts
M	AtelierConnect/frontend/src/app/shared/utils/api-error.util.spec.ts
M	AtelierConnect/src/main/java/com/atelierconnect/DataInitializer.java
M	AtelierConnect/src/main/java/com/atelierconnect/config/SecurityConfig.java
M	AtelierConnect/src/main/java/com/atelierconnect/config/SwaggerConfig.java
M	AtelierConnect/src/main/java/com/atelierconnect/controller/AtelierController.java
M	AtelierConnect/src/main/java/com/atelierconnect/controller/AuthController.java
M	AtelierConnect/src/main/java/com/atelierconnect/controller/CentreController.java
M	AtelierConnect/src/main/java/com/atelierconnect/controller/JourFermetureController.java
M	AtelierConnect/src/main/java/com/atelierconnect/controller/PaiementController.java
M	AtelierConnect/src/main/java/com/atelierconnect/controller/ParticipantController.java
M	AtelierConnect/src/main/java/com/atelierconnect/controller/ReservationController.java
M	AtelierConnect/src/main/java/com/atelierconnect/controller/SalleController.java
M	AtelierConnect/src/main/java/com/atelierconnect/dto/request/AtelierRequest.java
M	AtelierConnect/src/main/java/com/atelierconnect/dto/request/CentreRequest.java
M	AtelierConnect/src/main/java/com/atelierconnect/dto/request/CreateAtelierRequest.java
M	AtelierConnect/src/main/java/com/atelierconnect/dto/request/JourFermetureRequest.java
M	AtelierConnect/src/main/java/com/atelierconnect/dto/request/ParticipantRequest.java
M	AtelierConnect/src/main/java/com/atelierconnect/dto/request/ReservationInviteRequest.java
M	AtelierConnect/src/main/java/com/atelierconnect/dto/request/ReservationRequest.java
M	AtelierConnect/src/main/java/com/atelierconnect/dto/request/SalleRequest.java
M	AtelierConnect/src/main/java/com/atelierconnect/dto/response/AtelierDto.java
M	AtelierConnect/src/main/java/com/atelierconnect/dto/response/CentreResponse.java
M	AtelierConnect/src/main/java/com/atelierconnect/dto/response/JourFermetureResponse.java
M	AtelierConnect/src/main/java/com/atelierconnect/dto/response/LoginResponse.java
M	AtelierConnect/src/main/java/com/atelierconnect/dto/response/ParticipantResponse.java
M	AtelierConnect/src/main/java/com/atelierconnect/dto/response/ReservationResponse.java
M	AtelierConnect/src/main/java/com/atelierconnect/dto/response/SalleResponse.java
M	AtelierConnect/src/main/java/com/atelierconnect/entity/Administrateur.java
M	AtelierConnect/src/main/java/com/atelierconnect/entity/Atelier.java
M	AtelierConnect/src/main/java/com/atelierconnect/entity/Centre.java
M	AtelierConnect/src/main/java/com/atelierconnect/entity/JourFermeture.java
M	AtelierConnect/src/main/java/com/atelierconnect/entity/Participant.java
M	AtelierConnect/src/main/java/com/atelierconnect/entity/Penalite.java
M	AtelierConnect/src/main/java/com/atelierconnect/entity/Reservation.java
M	AtelierConnect/src/main/java/com/atelierconnect/entity/Salle.java
M	AtelierConnect/src/main/java/com/atelierconnect/entity/enums/StatutAtelier.java
M	AtelierConnect/src/main/java/com/atelierconnect/entity/enums/TypeAdministrateur.java
M	AtelierConnect/src/main/java/com/atelierconnect/entity/enums/TypeParticipant.java
M	AtelierConnect/src/main/java/com/atelierconnect/entity/enums/TypeParticipantLegacy.java
M	AtelierConnect/src/main/java/com/atelierconnect/mapper/AtelierMapper.java
M	AtelierConnect/src/main/java/com/atelierconnect/mapper/CentreMapper.java
M	AtelierConnect/src/main/java/com/atelierconnect/mapper/ParticipantMapper.java
M	AtelierConnect/src/main/java/com/atelierconnect/mapper/ReservationMapper.java
M	AtelierConnect/src/main/java/com/atelierconnect/mapper/SalleMapper.java
M	AtelierConnect/src/main/java/com/atelierconnect/repository/AtelierRepository.java
M	AtelierConnect/src/main/java/com/atelierconnect/repository/CentreRepository.java
M	AtelierConnect/src/main/java/com/atelierconnect/repository/JourFermetureRepository.java
M	AtelierConnect/src/main/java/com/atelierconnect/repository/PaiementRepository.java
M	AtelierConnect/src/main/java/com/atelierconnect/repository/ParticipantRepository.java
M	AtelierConnect/src/main/java/com/atelierconnect/repository/PenaliteRepository.java
M	AtelierConnect/src/main/java/com/atelierconnect/repository/ReservationRepository.java
M	AtelierConnect/src/main/java/com/atelierconnect/repository/SalleRepository.java
M	AtelierConnect/src/main/java/com/atelierconnect/scheduler/AtelierScheduler.java
M	AtelierConnect/src/main/java/com/atelierconnect/service/AtelierService.java
M	AtelierConnect/src/main/java/com/atelierconnect/service/CentreService.java
M	AtelierConnect/src/main/java/com/atelierconnect/service/PaiementService.java
M	AtelierConnect/src/main/java/com/atelierconnect/service/ParticipantService.java
M	AtelierConnect/src/main/java/com/atelierconnect/service/ReservationService.java
M	AtelierConnect/src/main/java/com/atelierconnect/service/SalleService.java
M	AtelierConnect/src/main/java/com/atelierconnect/service/impl/AdminAuthorizationService.java
M	AtelierConnect/src/main/java/com/atelierconnect/service/impl/AtelierServiceImpl.java
M	AtelierConnect/src/main/java/com/atelierconnect/service/impl/AuthServiceImpl.java
M	AtelierConnect/src/main/java/com/atelierconnect/service/impl/CentreServiceImpl.java
M	AtelierConnect/src/main/java/com/atelierconnect/service/impl/CurrentParticipantService.java
M	AtelierConnect/src/main/java/com/atelierconnect/service/impl/PaiementServiceImpl.java
M	AtelierConnect/src/main/java/com/atelierconnect/service/impl/ParticipantAccessService.java
M	AtelierConnect/src/main/java/com/atelierconnect/service/impl/ParticipantCreationService.java
M	AtelierConnect/src/main/java/com/atelierconnect/service/impl/ParticipantServiceImpl.java
M	AtelierConnect/src/main/java/com/atelierconnect/service/impl/ReservationServiceImpl.java
M	AtelierConnect/src/main/java/com/atelierconnect/service/impl/SalleServiceImpl.java
M	AtelierConnect/src/test/java/com/atelierconnect/DataInitializerTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/controller/AtelierControllerTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/controller/JourFermetureControllerTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/controller/PaiementControllerTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/controller/ParticipantControllerTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/controller/ReservationControllerTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/controller/SallePublicEndpointIntegrationTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/dto/DtoJsonCompatibilityTest.java
A	AtelierConnect/src/test/java/com/atelierconnect/entity/LegacySchemaCompatibilityTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/mapper/AtelierMapperTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/mapper/ParticipantMapperTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/mapper/ReservationMapperTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/repository/AtelierRepositoryTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/repository/JourFermetureRepositoryTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/repository/ReservationRepositoryTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/service/AdminAuthorizationServiceTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/service/AtelierServiceTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/service/PaiementConcurrencyPostgresTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/service/PaiementServiceTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/service/ParticipantAccessServiceTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/service/ParticipantServiceTest.java
M	AtelierConnect/src/test/java/com/atelierconnect/service/ReservationServiceTest.java
```

Documentation ajout?e : ce fichier et `refactoring-final-2026-11-01.md`.
