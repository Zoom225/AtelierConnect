package com.atelierconnect.controller;

import com.atelierconnect.entity.Centre;
import com.atelierconnect.entity.Salle;
import com.atelierconnect.repository.CentreRepository;
import com.atelierconnect.repository.SalleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SalleWriteEndpointIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private SalleRepository salleRepository;
    @Autowired private CentreRepository centreRepository;

    @Test
    @WithMockUser(username = "admin@atelierconnect.example", roles = "ADMIN")
    void updateReturnsCentreAfterRepositoryTransactionsHaveClosed() throws Exception {
        // No @Transactional on the test: that would hide the production lazy-loading failure.
        Centre centre = centreRepository.findAll().getFirst();
        Salle salle = salleRepository.save(Salle.builder().nom("Salle regression update").centre(centre).build());
        try {
            mvc.perform(put("/api/salles/{id}", salle.getId()).contentType("application/json")
                            .content("{\"nom\":\"Salle modifiee\",\"centreId\":" + centre.getId() + "}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nom").value("Salle modifiee"))
                    .andExpect(jsonPath("$.centreId").value(centre.getId()))
                    .andExpect(jsonPath("$.centreNom").value(centre.getNom()));
        } finally {
            salleRepository.deleteById(salle.getId());
        }
    }
}
