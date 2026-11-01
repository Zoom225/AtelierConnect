package com.atelierconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.atelierconnect.config.JwtConfig;
import com.atelierconnect.entity.enums.StatutAtelier;
import com.atelierconnect.entity.enums.StatutReservation;
import com.atelierconnect.entity.enums.TypeAtelier;
import com.atelierconnect.dto.request.CreateAtelierRequest;
import com.atelierconnect.dto.response.AtelierDto;
import com.atelierconnect.repository.AdministrateurRepository;
import com.atelierconnect.repository.ParticipantRepository;
import com.atelierconnect.repository.ReservationRepository;
import com.atelierconnect.service.AtelierService;
import com.atelierconnect.service.impl.AdminAuthorizationService;
import com.atelierconnect.service.impl.CurrentParticipantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.atelierconnect.test.TestSecurityConfig;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Étape 1 : Importer la configuration de sécurité
//@Import(SecurityConfig.class)
// Étape 2 : Cibler le contrôleur
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = AtelierController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Tests de MatchController")
class AtelierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AtelierService atelierService;
    @MockitoBean
    private AdminAuthorizationService adminAuthorizationService;
    @MockitoBean
    private CurrentParticipantService currentParticipantService;
    @MockitoBean
    private ReservationRepository reservationRepository;

    // Étape 3 : Mocker les dépendances de la chaîne de sécurité (Je wtAuthenticationFilter)
    @MockitoBean
    private JwtConfig jwtConfig;
    @MockitoBean
    private AdministrateurRepository administrateurRepository;
    @MockitoBean
    private ParticipantRepository participantRepository;


    private CreateAtelierRequest validCreateAtelierRequest;
    private AtelierDto atelierDto;

    @BeforeEach
    void setUp() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(10).withNano(0);
        validCreateAtelierRequest = new CreateAtelierRequest(1L, startTime, "PUBLIC");

        atelierDto = new AtelierDto(
                1L,                                        // id
                1L,                                        // terrainId
                "Court Central",                           // terrainNom
                "Centre collaboratif Lyon",                         // siteNom
                10L,                                       // organisateurId
                "John Doe",                                // organisateurNom
                startTime.toLocalDate(),                   // date
                startTime.toLocalTime(),                   // heureDebut
                startTime.plusMinutes(90).toLocalTime(),   // heureFin
                TypeAtelier.PUBLIC,                          // typeMatch
                StatutAtelier.PLANIFIE,                      // statut
                1,                                         // nbJoueursActuels
                15.0,                                      // prixParJoueur
                null                                       // dateConversionPublic
        );
    }

    @Test
    @WithMockUser(username = "membre.test", roles = "LIBRE")
    @DisplayName("POST /api/ateliers - Doit créer un atelier pour un rôle autorisé")
    void createAtelier_WithAuthorizedRole_ShouldReturnCreated() throws Exception {
        when(atelierService.createAtelier(any(CreateAtelierRequest.class), anyString())).thenReturn(atelierDto);

        mockMvc.perform(post("/api/matches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateAtelierRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(atelierDto.id()));
    }




    @Test
    @DisplayName("GET /api/ateliers/public - Doit retourner les atelier publics sans authentification")
    void getPublicAteliers_WithoutAuthentication_ShouldReturnOk() throws Exception {
        when(atelierService.getPublicAvailableAteliers()).thenReturn(List.of(atelierDto));

        mockMvc.perform(get("/api/matches/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(atelierDto.id()));
        mockMvc.perform(get("/api/ateliers/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(atelierDto.id()));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void atelierPriveRefuseAuParticipantSansInvitation() throws Exception {
        AtelierDto privateDto = new AtelierDto(atelierDto.id(), atelierDto.salleId(), atelierDto.salleNom(),
                atelierDto.centreNom(), atelierDto.organisateurId(), atelierDto.organisateurNom(),
                atelierDto.date(), atelierDto.heureDebut(), atelierDto.heureFin(), TypeAtelier.PRIVE,
                atelierDto.statut(), atelierDto.nbParticipantsActuels(), atelierDto.prixParParticipant(), null);
        when(atelierService.getAtelierDtoById(1L)).thenReturn(privateDto);
        when(currentParticipantService.currentParticipantId()).thenReturn(99L);

        mockMvc.perform(get("/api/ateliers/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void atelierPriveAccessibleAuParticipantInvite() throws Exception {
        AtelierDto privateDto = new AtelierDto(atelierDto.id(), atelierDto.salleId(), atelierDto.salleNom(),
                atelierDto.centreNom(), atelierDto.organisateurId(), atelierDto.organisateurNom(),
                atelierDto.date(), atelierDto.heureDebut(), atelierDto.heureFin(), TypeAtelier.PRIVE,
                atelierDto.statut(), atelierDto.nbParticipantsActuels(), atelierDto.prixParParticipant(), null);
        when(atelierService.getAtelierDtoById(1L)).thenReturn(privateDto);
        when(currentParticipantService.currentParticipantId()).thenReturn(99L);
        when(reservationRepository.existsByAtelierIdAndParticipantIdAndStatutNot(1L, 99L, StatutReservation.ANNULEE))
                .thenReturn(true);

        mockMvc.perform(get("/api/ateliers/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void atelierPriveRefuseApresAnnulationDeLInvitation() throws Exception {
        AtelierDto privateDto = new AtelierDto(atelierDto.id(), atelierDto.salleId(), atelierDto.salleNom(),
                atelierDto.centreNom(), atelierDto.organisateurId(), atelierDto.organisateurNom(),
                atelierDto.date(), atelierDto.heureDebut(), atelierDto.heureFin(), TypeAtelier.PRIVE,
                atelierDto.statut(), atelierDto.nbParticipantsActuels(), atelierDto.prixParParticipant(), null);
        when(atelierService.getAtelierDtoById(1L)).thenReturn(privateDto);
        when(currentParticipantService.currentParticipantId()).thenReturn(99L);

        mockMvc.perform(get("/api/ateliers/1"))
                .andExpect(status().isForbidden());

        verify(reservationRepository).existsByAtelierIdAndParticipantIdAndStatutNot(
                1L, 99L, StatutReservation.ANNULEE);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ateliers - Doit retourner tous les atelier pour un utilisateur authentifié")
    void getAllAteliers_WithAuthenticatedUser_ShouldReturnOk() throws Exception {
        when(atelierService.findAllAteliers()).thenReturn(List.of(atelierDto));
        when(adminAuthorizationService.filterAtelierDtos(any())).thenReturn(List.of(atelierDto));

        mockMvc.perform(get("/api/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(atelierDto.id()));
    }
}
