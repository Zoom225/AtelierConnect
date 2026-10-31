package com.atelierconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.atelierconnect.config.JwtConfig;
import com.atelierconnect.entity.enums.StatutAtelier;
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
    private AtelierService matchService;
    @MockitoBean
    private AdminAuthorizationService adminAuthorizationService;
    @MockitoBean
    private CurrentParticipantService currentMemberService;
    @MockitoBean
    private ReservationRepository reservationRepository;

    // Étape 3 : Mocker les dépendances de la chaîne de sécurité (Je wtAuthenticationFilter)
    @MockitoBean
    private JwtConfig jwtConfig;
    @MockitoBean
    private AdministrateurRepository administrateurRepository;
    @MockitoBean
    private ParticipantRepository membreRepository;


    private CreateAtelierRequest validCreateMatchRequest;
    private AtelierDto matchDto;

    @BeforeEach
    void setUp() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(10).withNano(0);
        validCreateMatchRequest = new CreateAtelierRequest(1L, startTime, "PUBLIC");

        matchDto = new AtelierDto(
                1L,                                        // id
                1L,                                        // terrainId
                "Court Central",                           // terrainNom
                "Padel Club Lyon",                         // siteNom
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
    @DisplayName("POST /api/matches - Doit créer un match pour un rôle autorisé")
    void createMatch_WithAuthorizedRole_ShouldReturnCreated() throws Exception {
        when(matchService.createMatch(any(CreateAtelierRequest.class), anyString())).thenReturn(matchDto);

        mockMvc.perform(post("/api/matches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateMatchRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(matchDto.id()));
    }




    @Test
    @DisplayName("GET /api/matches/public - Doit retourner les matchs publics sans authentification")
    void getPublicMatches_WithoutAuthentication_ShouldReturnOk() throws Exception {
        when(matchService.getPublicAvailableMatches()).thenReturn(List.of(matchDto));

        mockMvc.perform(get("/api/matches/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(matchDto.id()));
        mockMvc.perform(get("/api/ateliers/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(matchDto.id()));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void atelierPriveRefuseAuParticipantSansInvitation() throws Exception {
        AtelierDto privateDto = new AtelierDto(matchDto.id(), matchDto.terrainId(), matchDto.terrainNom(),
                matchDto.siteNom(), matchDto.organisateurId(), matchDto.organisateurNom(),
                matchDto.date(), matchDto.heureDebut(), matchDto.heureFin(), TypeAtelier.PRIVE,
                matchDto.statut(), matchDto.nbJoueursActuels(), matchDto.prixParJoueur(), null);
        when(matchService.getMatchDtoById(1L)).thenReturn(privateDto);
        when(currentMemberService.currentMemberId()).thenReturn(99L);

        mockMvc.perform(get("/api/ateliers/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void atelierPriveAccessibleAuParticipantInvite() throws Exception {
        AtelierDto privateDto = new AtelierDto(matchDto.id(), matchDto.terrainId(), matchDto.terrainNom(),
                matchDto.siteNom(), matchDto.organisateurId(), matchDto.organisateurNom(),
                matchDto.date(), matchDto.heureDebut(), matchDto.heureFin(), TypeAtelier.PRIVE,
                matchDto.statut(), matchDto.nbJoueursActuels(), matchDto.prixParJoueur(), null);
        when(matchService.getMatchDtoById(1L)).thenReturn(privateDto);
        when(currentMemberService.currentMemberId()).thenReturn(99L);
        when(reservationRepository.existsByMatchIdAndMembreId(1L, 99L)).thenReturn(true);

        mockMvc.perform(get("/api/ateliers/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/matches - Doit retourner tous les matchs pour un utilisateur authentifié")
    void getAllMatches_WithAuthenticatedUser_ShouldReturnOk() throws Exception {
        when(matchService.findAllMatches()).thenReturn(List.of(matchDto));
        when(adminAuthorizationService.filterMatchDtos(any())).thenReturn(List.of(matchDto));

        mockMvc.perform(get("/api/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(matchDto.id()));
    }
}
