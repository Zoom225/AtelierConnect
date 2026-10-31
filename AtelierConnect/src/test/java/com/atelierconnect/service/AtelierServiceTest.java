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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de MatchServiceImpl")
class AtelierServiceTest {

    @Mock
    private AtelierRepository matchRepository;
    @Mock
    private ParticipantRepository membreRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private PaiementRepository paiementRepository;
    @Mock
    private JourFermetureRepository jourFermetureRepository;
    @Mock
    private SalleService terrainService;
    @Mock
    private ParticipantService membreService;
    @Mock
    private AtelierMapper matchMapper;

    @InjectMocks
    private AtelierServiceImpl matchService;

    private Participant organisateur;
    private Salle terrain;
    private Centre site;
    private CreateAtelierRequest createMatchRequest;
    private AtelierDto matchDto;

    @BeforeEach
    void setUp() {
        site = new Centre();
        site.setId(1L);
        site.setDureeMatchMinutes(AtelierRules.DUREE_MINUTES);
        site.setDureeEntreMatchMinutes(AtelierRules.PAUSE_MINUTES);
        site.setHeureOuverture(LocalTime.of(9, 0));
        site.setHeureFermeture(LocalTime.of(22, 0));
        site.setOuvertVisiteurs(true);

        organisateur = new Participant();
        organisateur.setId(1L);
        organisateur.setMatricule("user123");
        organisateur.setPrenom("John");
        organisateur.setNom("Doe");
        organisateur.setTypeMembre(TypeParticipant.LIBRE);

        terrain = new Salle();
        terrain.setId(1L);
        terrain.setNom("Court Central");
        terrain.setPrix(20.0);
        terrain.setSite(site);

        createMatchRequest = new CreateAtelierRequest(
                terrain.getId(),
                LocalDate.now().plusDays(5).atTime(10, 0),
                "PUBLIC"
        );

        matchDto = new AtelierDto(
                1L,
                terrain.getId(),
                terrain.getNom(),
                null, // siteNom
                organisateur.getId(),
                organisateur.getPrenom() + " " + organisateur.getNom(),
                createMatchRequest.matchDate().toLocalDate(),
                createMatchRequest.matchDate().toLocalTime(),
                createMatchRequest.matchDate().plusMinutes(AtelierRules.DUREE_MINUTES).toLocalTime(),
                TypeAtelier.PUBLIC,
                StatutAtelier.PLANIFIE,
                0,
                15.0,
                null  // dateConversionPublic
        );
    }

    @Test
    void ouverturePubliqueExigeAccordExplicite() {
        Atelier prive = new Atelier();
        prive.setId(10L);
        prive.setOrganisateur(organisateur);
        prive.setTypeMatch(TypeAtelier.PRIVE);
        prive.setStatut(StatutAtelier.PLANIFIE);
        prive.setNbJoueursActuels(1);
        prive.setDateDebut(LocalDate.now().plusDays(AtelierRules.OUVERTURE_PUBLIQUE_JOURS).atTime(10, 0));
        when(matchRepository.findById(10L)).thenReturn(Optional.of(prive));

        assertThrows(BusinessException.class, () -> matchService.convertToPublic(10L));
        verify(matchRepository, never()).save(any());

        matchService.consentToPublicOpening(10L, organisateur.getId());
        assertNotNull(prive.getDateAccordOuverturePublique());
        matchService.convertToPublic(10L);
        assertEquals(TypeAtelier.PUBLIC, prive.getTypeMatch());
        assertNotNull(prive.getDateConversionPublic());
        verify(membreService, never()).addPenalty(any());
    }

    @Test
    void refusAccordParUnAutreParticipant() {
        Atelier prive = new Atelier();
        prive.setId(10L);
        prive.setOrganisateur(organisateur);
        when(matchRepository.findById(10L)).thenReturn(Optional.of(prive));

        assertThrows(BusinessException.class, () -> matchService.consentToPublicOpening(10L, 999L));
        verify(matchRepository, never()).save(any());
    }

    @Test
    @DisplayName("createMatch - Succès")
    void createMatch_ShouldSucceed_WhenAllRulesAreMet() {
        // Arrange
        when(membreRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(terrainService.getById(terrain.getId())).thenReturn(terrain);
        when(membreService.hasOutstandingBalance(organisateur.getId())).thenReturn(false);
        when(membreService.hasActivePenalty(organisateur.getId())).thenReturn(false);
        when(matchRepository.findOverlappingAteliers(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(matchRepository.save(any(Atelier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paiementRepository.save(any(Paiement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchMapper.toAtelierDto(any(Atelier.class))).thenReturn(matchDto); // Utiliser le DTO corrigé

        // Act
        AtelierDto result = matchService.createMatch(createMatchRequest, "user123");

        // Assert
        assertNotNull(result);
        assertEquals(0, result.nbJoueursActuels());
        assertEquals(TypeAtelier.PUBLIC, result.typeMatch());
        verify(matchRepository, times(1)).save(argThat(match ->
                match.getStatut() == StatutAtelier.PLANIFIE &&
                        match.getNbJoueursActuels() == 0
        ));
        verify(reservationRepository, times(1)).save(argThat(reservation ->
                reservation.getStatut() == StatutReservation.EN_ATTENTE
        ));
        verify(paiementRepository, times(1)).save(argThat(paiement ->
                paiement.getStatut() == StatutPaiement.EN_ATTENTE
        ));
    }

    @Test
    @DisplayName("createMatch - Échoue si le membre a un solde impayé")
    void createMatch_ShouldFail_WhenMemberHasOutstandingBalance() {
        // Arrange
        when(membreRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(terrainService.getById(terrain.getId())).thenReturn(terrain);
        when(membreService.hasOutstandingBalance(organisateur.getId())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            matchService.createMatch(createMatchRequest, "user123");
        });
        assertEquals("Vous avez un solde impayé et ne pouvez pas créer de match.", exception.getMessage());
        verify(matchRepository, never()).save(any());
    }

    @Test
    @DisplayName("createMatch - Échoue si le membre a une pénalité active")
    void createMatch_ShouldFail_WhenMemberHasActivePenalty() {
        // Arrange
        when(membreRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(terrainService.getById(terrain.getId())).thenReturn(terrain);
        when(membreService.hasOutstandingBalance(organisateur.getId())).thenReturn(false);
        when(membreService.hasActivePenalty(organisateur.getId())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            matchService.createMatch(createMatchRequest, "user123");
        });
        assertEquals("Vous avez une pénalité active et ne pouvez pas créer de match.", exception.getMessage());
    }

    @Test
    @DisplayName("createMatch - Échoue si le délai de réservation n'est pas respecté")
    void createMatch_ShouldFail_WhenBookingDelayIsNotMet() {
        // Arrange
        CreateAtelierRequest tooFarRequest = new CreateAtelierRequest(terrain.getId(), LocalDateTime.now().plusDays(8), "PUBLIC");
        when(membreRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(terrainService.getById(terrain.getId())).thenReturn(terrain);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            matchService.createMatch(tooFarRequest, "user123");
        });
        assertTrue(exception.getMessage().contains("ne peut pas réserver plus de 7 jours à l'avance"));
    }

    @Test
    void delaisDesTroisCategoriesSontAppliques() {
        when(membreRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(terrainService.getById(terrain.getId())).thenReturn(terrain);

        for (TypeParticipant type : TypeParticipant.values()) {
            organisateur.setTypeMembre(type);
            int delai = AtelierRules.delaiReservation(type);
            CreateAtelierRequest demande = new CreateAtelierRequest(
                    terrain.getId(), LocalDate.now().plusDays(delai + 1).atTime(10, 0), "PUBLIC");
            BusinessException erreur = assertThrows(BusinessException.class,
                    () -> matchService.createMatch(demande, "user123"));
            assertTrue(erreur.getMessage().contains(delai + " jours"));
        }
        verify(matchRepository, never()).save(any());
    }

    @Test
    @DisplayName("createMatch - Échoue si le créneau est déjà pris")
    void createMatch_ShouldFail_WhenSlotIsAlreadyBooked() {
        // Arrange
        when(membreRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(terrainService.getById(terrain.getId())).thenReturn(terrain);
        when(matchRepository.findOverlappingAteliers(any(), any(), any(), any())).thenReturn(Collections.singletonList(new Atelier()));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            matchService.createMatch(createMatchRequest, "user123");
        });
        assertEquals("Ce créneau est déjà réservé sur le terrain : " + terrain.getId(), exception.getMessage());
    }

    @Test
    @DisplayName("createMatch - Échoue si le membre n'est pas trouvé")
    void createMatch_ShouldFail_WhenMemberNotFound() {
        // Arrange
        when(membreRepository.findByMatriculeIgnoreCase("unknownUser")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.createMatch(createMatchRequest, "unknownUser");
        });
        assertEquals("Membre non trouvé pour l'utilisateur: unknownUser", exception.getMessage());
    }
}
