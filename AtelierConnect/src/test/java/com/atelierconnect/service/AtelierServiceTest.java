package com.atelierconnect.service;

import com.atelierconnect.entity.*;
import com.atelierconnect.entity.enums.StatutAtelier;
import com.atelierconnect.entity.enums.StatutPaiement;
import com.atelierconnect.entity.enums.StatutReservation;
import com.atelierconnect.entity.enums.TypeAtelier;
import com.atelierconnect.entity.enums.TypeParticipant;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.exception.ResourceNotFoundException;
import com.atelierconnect.mapper.AtelierMapper;
import com.atelierconnect.dto.request.CreateAtelierRequest;
import com.atelierconnect.dto.request.AtelierRequest;
import com.atelierconnect.dto.response.AtelierDto;
import com.atelierconnect.repository.*;
import com.atelierconnect.service.impl.AtelierServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de AtelierServiceImpl")
class AtelierServiceTest {

    @Mock
    private AtelierRepository atelierRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private PaiementRepository paiementRepository;
    @Mock
    private JourFermetureRepository jourFermetureRepository;
    @Mock
    private SalleService salleService;
    @Mock
    private ParticipantService participantService;
    @Mock
    private AtelierMapper atelierMapper;

    @InjectMocks
    private AtelierServiceImpl atelierService;

    private Participant organisateur;
    private Salle salle;
    private Centre centre;
    private CreateAtelierRequest createAtelierRequest;
    private AtelierDto atelierDto;

    @BeforeEach
    void setUp() {
        centre = new Centre();
        centre.setId(1L);
        centre.setDureeAtelierMinutes(AtelierRules.DUREE_MINUTES);
        centre.setDureeEntreAtelierMinutes(AtelierRules.PAUSE_MINUTES);
        centre.setHeureOuverture(LocalTime.of(9, 0));
        centre.setHeureFermeture(LocalTime.of(22, 0));
        centre.setOuvertVisiteurs(true);

        organisateur = new Participant();
        organisateur.setId(1L);
        organisateur.setMatricule("user123");
        organisateur.setPrenom("John");
        organisateur.setNom("Doe");
        organisateur.setTypeParticipant(TypeParticipant.VISITEUR);

        salle = new Salle();
        salle.setId(1L);
        salle.setNom("Court Central");
        salle.setPrix(20.0);
        salle.setCentre(centre);

        createAtelierRequest = new CreateAtelierRequest(
                salle.getId(),
                LocalDate.now().plusDays(5).atTime(10, 0),
                "PUBLIC"
        );

        atelierDto = new AtelierDto(
                1L,
                salle.getId(),
                salle.getNom(),
                null, // siteNom
                organisateur.getId(),
                organisateur.getPrenom() + " " + organisateur.getNom(),
                createAtelierRequest.atelierDate().toLocalDate(),
                createAtelierRequest.atelierDate().toLocalTime(),
                createAtelierRequest.atelierDate().plusMinutes(AtelierRules.DUREE_MINUTES).toLocalTime(),
                TypeAtelier.PUBLIC,
                StatutAtelier.PLANIFIE,
                0,
                15.0,
                null  // dateConversionPublic
        );
    }

    @Test
    void modificationRefuseUnDelaiDepassantLaCategorie() {
        Atelier existant = new Atelier();
        existant.setId(21L);
        existant.setOrganisateur(organisateur);
        existant.setStatut(StatutAtelier.PLANIFIE);
        when(atelierRepository.findById(21L)).thenReturn(Optional.of(existant));
        when(salleService.getById(1L)).thenReturn(salle);

        AtelierRequest demande = AtelierRequest.builder()
                .salleId(1L)
                .organisateurId(1L)
                .date(LocalDate.now().plusDays(AtelierRules.VISITEUR_JOURS + 1))
                .heureDebut(LocalTime.of(11, 0))
                .typeAtelier(TypeAtelier.PUBLIC)
                .build();

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> atelierService.updateAtelier(21L, demande));

        assertTrue(erreur.getMessage().contains("7 jours"));
        verify(atelierRepository, never()).save(any());
    }

    @Test
    void modificationRefuseUnCentreEtrangerPourAbonne() {
        organisateur.setTypeParticipant(TypeParticipant.ABONNE);
        organisateur.setCentre(centre);
        Centre autreCentre = new Centre();
        autreCentre.setId(2L);
        Salle autreSalle = new Salle();
        autreSalle.setId(2L);
        autreSalle.setCentre(autreCentre);
        Atelier existant = new Atelier();
        existant.setId(22L);
        existant.setOrganisateur(organisateur);
        existant.setStatut(StatutAtelier.PLANIFIE);
        when(atelierRepository.findById(22L)).thenReturn(Optional.of(existant));
        when(salleService.getById(2L)).thenReturn(autreSalle);

        AtelierRequest demande = AtelierRequest.builder()
                .salleId(2L)
                .organisateurId(1L)
                .date(LocalDate.now().plusDays(3))
                .heureDebut(LocalTime.of(11, 0))
                .typeAtelier(TypeAtelier.PUBLIC)
                .build();

        assertThrows(BusinessException.class,
                () -> atelierService.updateAtelier(22L, demande));
        verify(atelierRepository, never()).save(any());
    }

    @Test
    void ouverturePubliqueExigeAccordExplicite() {
        Atelier prive = new Atelier();
        prive.setId(10L);
        prive.setOrganisateur(organisateur);
        prive.setTypeAtelier(TypeAtelier.PRIVE);
        prive.setStatut(StatutAtelier.PLANIFIE);
        prive.setNbParticipantsActuels(1);
        prive.setDateDebut(LocalDate.now().plusDays(AtelierRules.OUVERTURE_PUBLIQUE_JOURS).atTime(10, 0));
        when(atelierRepository.findById(10L)).thenReturn(Optional.of(prive));

        assertThrows(BusinessException.class, () -> atelierService.convertToPublic(10L));
        verify(atelierRepository, never()).save(any());

        atelierService.consentToPublicOpening(10L, organisateur.getId());
        assertNotNull(prive.getDateAccordOuverturePublique());
        atelierService.convertToPublic(10L);
        assertEquals(TypeAtelier.PUBLIC, prive.getTypeAtelier());
        assertNotNull(prive.getDateConversionPublic());
        verify(participantService, never()).addPenalty(any());
    }

    @Test
    void schedulerOuvreADeuxJoursSeulementLesAteliersAvecAccord() {
        LocalDateTime date = LocalDate.now()
                .plusDays(AtelierRules.OUVERTURE_PUBLIQUE_JOURS).atTime(10, 0);
        Atelier consenti = new Atelier();
        consenti.setId(31L);
        consenti.setOrganisateur(organisateur);
        consenti.setTypeAtelier(TypeAtelier.PRIVE);
        consenti.setStatut(StatutAtelier.PLANIFIE);
        consenti.setDateDebut(date);
        consenti.setAccordOuverturePublique(true);
        consenti.setNbParticipantsActuels(1);

        Atelier sansAccord = new Atelier();
        sansAccord.setId(32L);
        sansAccord.setTypeAtelier(TypeAtelier.PRIVE);
        sansAccord.setStatut(StatutAtelier.PLANIFIE);
        sansAccord.setDateDebut(date);
        sansAccord.setAccordOuverturePublique(false);
        sansAccord.setNbParticipantsActuels(1);

        when(atelierRepository.findByDateDebutBetweenAndStatut(any(), any(), eq(StatutAtelier.PLANIFIE)))
                .thenReturn(List.of(consenti, sansAccord));
        when(atelierRepository.findById(31L)).thenReturn(Optional.of(consenti));

        atelierService.checkAndConvertExpiredPrivateAteliers();

        assertEquals(TypeAtelier.PUBLIC, consenti.getTypeAtelier());
        assertNotNull(consenti.getDateConversionPublic());
        assertEquals(TypeAtelier.PRIVE, sansAccord.getTypeAtelier());
        verify(atelierRepository).save(consenti);
        verify(atelierRepository, never()).save(sansAccord);
        verify(participantService, never()).addPenalty(any());
    }

    @Test
    void schedulerAccepteUnCompteurParticipantsHistoriqueNull() {
        Atelier consenti = new Atelier();
        consenti.setId(33L);
        consenti.setOrganisateur(organisateur);
        consenti.setTypeAtelier(TypeAtelier.PRIVE);
        consenti.setStatut(StatutAtelier.PLANIFIE);
        consenti.setDateDebut(LocalDate.now().plusDays(AtelierRules.OUVERTURE_PUBLIQUE_JOURS).atTime(10, 0));
        consenti.setAccordOuverturePublique(true);
        consenti.setNbParticipantsActuels(null);

        when(atelierRepository.findByDateDebutBetweenAndStatut(any(), any(), eq(StatutAtelier.PLANIFIE)))
                .thenReturn(List.of(consenti));
        when(atelierRepository.findById(33L)).thenReturn(Optional.of(consenti));

        atelierService.checkAndConvertExpiredPrivateAteliers();

        assertEquals(TypeAtelier.PUBLIC, consenti.getTypeAtelier());
        verify(atelierRepository).save(consenti);
    }

    @Test
    void refusAccordParUnAutreParticipant() {
        Atelier prive = new Atelier();
        prive.setId(10L);
        prive.setOrganisateur(organisateur);
        when(atelierRepository.findById(10L)).thenReturn(Optional.of(prive));

        assertThrows(BusinessException.class, () -> atelierService.consentToPublicOpening(10L, 999L));
        verify(atelierRepository, never()).save(any());
    }

    @Test
    @DisplayName("createMatch - Succès")
    void createAtelier_ShouldSucceed_WhenAllRulesAreMet() {
        // Arrange
        when(participantRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(salleService.getById(salle.getId())).thenReturn(salle);
        when(participantService.hasOutstandingBalance(organisateur.getId())).thenReturn(false);
        when(participantService.hasActivePenalty(organisateur.getId())).thenReturn(false);
        when(atelierRepository.findOverlappingAteliers(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(atelierRepository.save(any(Atelier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paiementRepository.save(any(Paiement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(atelierMapper.toAtelierDto(any(Atelier.class))).thenReturn(atelierDto); // Utiliser le DTO corrigé

        // Act
        AtelierDto result = atelierService.createAtelier(createAtelierRequest, "user123");

        // Assert
        assertNotNull(result);
        assertEquals(0, result.nbParticipantsActuels());
        assertEquals(TypeAtelier.PUBLIC, result.typeAtelier());
        verify(atelierRepository, times(1)).save(argThat(atelier ->
                atelier.getStatut() == StatutAtelier.PLANIFIE &&
                        atelier.getNbParticipantsActuels() == 0
        ));
        verify(reservationRepository, times(1)).save(argThat(reservation ->
                reservation.getStatut() == StatutReservation.EN_ATTENTE
        ));
        verify(paiementRepository, times(1)).save(argThat(paiement ->
                paiement.getStatut() == StatutPaiement.EN_ATTENTE
        ));
    }

    @Test
    @DisplayName("createMatch - Échoue si le participant a un solde impayé")
    void createAtelier_ShouldFail_WhenParticipantHasOutstandingBalance() {
        // Arrange
        when(participantRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(salleService.getById(salle.getId())).thenReturn(salle);
        when(participantService.hasOutstandingBalance(organisateur.getId())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            atelierService.createAtelier(createAtelierRequest, "user123");
        });
        assertEquals("Vous avez un solde impayé et ne pouvez pas créer d'atelier.", exception.getMessage());
        verify(atelierRepository, never()).save(any());
    }

    @Test
    @DisplayName("createMatch - Échoue si le participant a une pénalité active")
    void createAtelier_ShouldFail_WhenParticipantHasActivePenalty() {
        // Arrange
        when(participantRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(salleService.getById(salle.getId())).thenReturn(salle);
        when(participantService.hasOutstandingBalance(organisateur.getId())).thenReturn(false);
        when(participantService.hasActivePenalty(organisateur.getId())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            atelierService.createAtelier(createAtelierRequest, "user123");
        });
        assertEquals("Vous avez une suspension active et ne pouvez pas créer d'atelier.", exception.getMessage());
    }

    @Test
    @DisplayName("createMatch - Échoue si le délai de réservation n'est pas respecté")
    void createAtelier_ShouldFail_WhenBookingDelayIsNotMet() {
        // Arrange
        CreateAtelierRequest tooFarRequest = new CreateAtelierRequest(salle.getId(), LocalDateTime.now().plusDays(8), "PUBLIC");
        when(participantRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(salleService.getById(salle.getId())).thenReturn(salle);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            atelierService.createAtelier(tooFarRequest, "user123");
        });
        assertTrue(exception.getMessage().contains("ne peut pas réserver plus de 7 jours à l'avance"));
    }

    @Test
    void delaisDesTroisCategoriesSontAppliques() {
        when(participantRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(salleService.getById(salle.getId())).thenReturn(salle);

        for (TypeParticipant type : TypeParticipant.values()) {
            organisateur.setTypeParticipant(type);
            int delai = AtelierRules.delaiReservation(type);
            CreateAtelierRequest demande = new CreateAtelierRequest(
                    salle.getId(), LocalDate.now().plusDays(delai + 1).atTime(10, 0), "PUBLIC");
            BusinessException erreur = assertThrows(BusinessException.class,
                    () -> atelierService.createAtelier(demande, "user123"));
            assertTrue(erreur.getMessage().contains(delai + " jours"));
        }
        verify(atelierRepository, never()).save(any());
    }

    @Test
    @DisplayName("createMatch - Échoue si le créneau est déjà pris")
    void createAtelier_ShouldFail_WhenSlotIsAlreadyBooked() {
        // Arrange
        when(participantRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(salleService.getById(salle.getId())).thenReturn(salle);
        when(atelierRepository.findOverlappingAteliers(any(), any(), any(), any())).thenReturn(Collections.singletonList(new Atelier()));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            atelierService.createAtelier(createAtelierRequest, "user123");
        });
        assertEquals("Ce créneau est déjà réservé pour la salle : " + salle.getId(), exception.getMessage());
    }

    @Test
    @DisplayName("createMatch - Échoue si le participant n'est pas trouvé")
    void createAtelier_ShouldFail_WhenParticipantNotFound() {
        // Arrange
        when(participantRepository.findByMatriculeIgnoreCase("unknownUser")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            atelierService.createAtelier(createAtelierRequest, "unknownUser");
        });
        assertEquals("Participant introuvable pour l'utilisateur : unknownUser", exception.getMessage());
    }
}
