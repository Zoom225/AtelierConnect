package com.atelierconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.atelierconnect.config.JwtConfig;
import com.atelierconnect.config.SecurityConfig;
import com.atelierconnect.test.TestSecurityConfig;
import com.atelierconnect.dto.response.PaiementResponse;
import com.atelierconnect.entity.Paiement;
import com.atelierconnect.mapper.PaiementMapper;
import com.atelierconnect.repository.AdministrateurRepository;
import com.atelierconnect.repository.ParticipantRepository;
import com.atelierconnect.service.PaiementService;
import com.atelierconnect.service.impl.CurrentParticipantService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, TestSecurityConfig.class})
@WebMvcTest(controllers = PaiementController.class)
@DisplayName("Tests de PaiementController")
class PaiementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaiementService paiementService;
    @MockitoBean
    private PaiementMapper paiementMapper;
    @MockitoBean
    private CurrentParticipantService currentParticipantService;

    @MockitoBean
    private JwtConfig jwtConfig;
    @MockitoBean
    private AdministrateurRepository administrateurRepository;
    @MockitoBean
    private ParticipantRepository participantRepository;

    private Paiement paiement;
    private PaiementResponse paiementResponse;

    @BeforeEach
    void setUp() {
        paiement = new Paiement();
        paiement.setId(1L);
        paiement.setMontant(15.0);

        paiementResponse = new PaiementResponse();
        paiementResponse.setId(1L);
        paiementResponse.setMontant(15.0);
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("POST /paiements/reservation/{resId}/participant/{memId} - Doit traiter un paiement")
    void pay_ShouldProcessPayment_AndReturnOk() throws Exception {
        when(paiementService.pay(anyLong(), anyLong())).thenReturn(paiement);
        when(paiementMapper.toResponse(any(Paiement.class))).thenReturn(paiementResponse);
        when(currentParticipantService.currentParticipantId()).thenReturn(1L);

        mockMvc.perform(post("/api/paiements/reservation/1/membre/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.montant").value(15.0));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("GET /paiements/{id} - Doit retourner un paiement par ID")
    void getById_ShouldReturnPayment_WhenFound() throws Exception {
        when(paiementService.getById(1L)).thenReturn(paiement);
        when(paiementMapper.toResponse(any(Paiement.class))).thenReturn(paiementResponse);
        when(currentParticipantService.currentParticipantId()).thenReturn(1L);

        mockMvc.perform(get("/api/paiements/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }
}
