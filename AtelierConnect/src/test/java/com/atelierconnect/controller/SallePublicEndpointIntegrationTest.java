package com.atelierconnect.controller;

import com.atelierconnect.entity.Salle;
import com.atelierconnect.repository.SalleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SallePublicEndpointIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private SalleRepository salleRepository;

    @Test
    void listePubliqueInclutLeCentreSansLazyInitializationException() throws Exception {
        // Même condition que la production : JPA Open Session in View est désactivé.
        assertThat(salleRepository.findAll()).isNotEmpty();

        mvc.perform(get("/api/salles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].siteNom").isNotEmpty())
                .andExpect(jsonPath("$[0].siteId").isNumber());
    }

    @Test
    void detailPublicEtListeParCentreSontAccessiblesSansSessionJpa() throws Exception {
        List<Salle> salles = salleRepository.findAll();
        assertThat(salles).isNotEmpty();
        Long salleId = salles.get(0).getId();
        Long centreId = salles.get(0).getSite().getId();

        mvc.perform(get("/api/salles/{id}", salleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteNom").isNotEmpty());

        mvc.perform(get("/api/salles/site/{id}", centreId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].siteNom").isNotEmpty());
    }
}
