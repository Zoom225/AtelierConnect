package com.atelierconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.atelierconnect.config.JwtConfig;
import com.atelierconnect.config.SecurityConfig;
import com.atelierconnect.test.TestSecurityConfig;
import com.atelierconnect.dto.request.ParticipantLoginRequest;
import com.atelierconnect.dto.request.ParticipantRequest;
import com.atelierconnect.dto.response.ParticipantResponse;
import com.atelierconnect.entity.Participant;
import com.atelierconnect.mapper.ParticipantMapper;
import com.atelierconnect.repository.AdministrateurRepository;
import com.atelierconnect.repository.ParticipantRepository;
import com.atelierconnect.service.ParticipantService;
import com.atelierconnect.service.impl.CurrentParticipantService;
import com.atelierconnect.service.impl.ParticipantAccessService;
import com.atelierconnect.service.impl.ParticipantCreationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, TestSecurityConfig.class})
@WebMvcTest(controllers = ParticipantController.class)
@DisplayName("Tests de MembreController")
class ParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ParticipantService participantService;
    @MockitoBean
    private ParticipantMapper participantMapper;
    @MockitoBean(name = "participantAccessService")
    private ParticipantAccessService participantAccessService;
    @MockitoBean
    private ParticipantCreationService participantCreationService;
    @MockitoBean
    private CurrentParticipantService currentParticipantService;

    @MockitoBean
    private JwtConfig jwtConfig;
    @MockitoBean
    private AdministrateurRepository administrateurRepository;
    @MockitoBean
    private ParticipantRepository participantRepository;

    private ParticipantRequest participantRequest;
    private ParticipantResponse participantResponse;
    private Participant participantEntity; // Added for authenticate mock

    @BeforeEach
    void setUp() {
        participantRequest = new ParticipantRequest();
        participantRequest.setMatricule("L12345");
        participantRequest.setNom("Doe");
        participantRequest.setPrenom("John");
        participantRequest.setTypeParticipant(com.atelierconnect.entity.enums.TypeParticipantLegacy.GLOBAL);

        when(participantAccessService.canCreate(nullable(Long.class))).thenReturn(true);
        when(participantAccessService.canManageById(anyLong())).thenReturn(true);
        when(participantAccessService.canReadById(anyLong())).thenReturn(true);
        when(participantAccessService.canReadByMatricule(anyString())).thenReturn(true);

        participantResponse = new ParticipantResponse();
        participantResponse.setId(1L);
        participantResponse.setMatricule("L12345");

        participantEntity = new Participant(); // Initialize membreEntity
        participantEntity.setId(1L);
        participantEntity.setMatricule("L12345");
        participantEntity.setTypeParticipant(com.atelierconnect.entity.enums.TypeParticipant.VISITEUR);
    }

    @Test
    @DisplayName("POST /api/participants - Doit refuser la creation sans authentification")
    void createParticipant_WithoutAuthentication_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/membres")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(participantRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("POST /api/participants - Doit refuser la creation avec un role participant")
    void createParticipant_WithParticipantRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/membres")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(participantRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/participants - Doit creer un participant avec un role admin")
    void createParticipant_WithAdminRole_ShouldReturnCreated() throws Exception {
        when(participantCreationService.create(any(ParticipantRequest.class))).thenReturn(participantEntity);
        when(participantMapper.toResponse(any(Participant.class))).thenReturn(participantResponse);

        mockMvc.perform(post("/api/membres")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(participantRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/participants/{id} - Doit mettre à jour un participant avec un rôle autorisé")
    void updateParticipant_WithAuthorizedRole_ShouldReturnOk() throws Exception {
        when(participantService.update(anyLong(), any(Participant.class))).thenReturn(new Participant());
        when(participantMapper.toEntity(any(ParticipantRequest.class))).thenReturn(new Participant());
        when(participantMapper.toResponse(any(Participant.class))).thenReturn(participantResponse);

        mockMvc.perform(put("/api/membres/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(participantRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("PUT /api/participants/{id} - Doit retourner 403 pour un rôle non autorisé")
    void updateParticipant_WithUnauthorizedRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(put("/api/membres/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(participantRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/participants/{id} - Doit supprimer un participant avec un rôle autorisé")
    void deleteParticipant_WithAuthorizedRole_ShouldReturnNoContent() throws Exception {
        doNothing().when(participantService).delete(1L);

        mockMvc.perform(delete("/api/membres/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/participants/login - Doit authentifier un participant et retourner un token")
    void loginParticipant_ShouldAuthenticateAndReturnToken() throws Exception {
        ParticipantLoginRequest loginRequest = new ParticipantLoginRequest("L12345", "Membre1234!");
        when(participantService.authenticate("L12345", "Membre1234!")).thenReturn(participantEntity);
        when(participantMapper.toResponse(any(Participant.class))).thenReturn(participantResponse);
        when(jwtConfig.generateToken(anyString(), anyString(), anyString())).thenReturn("mocked-jwt-token");

        mockMvc.perform(post("/api/membres/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricule").value("L12345"))
                .andExpect(jsonPath("$.token").value("mocked-jwt-token"));
    }
}
