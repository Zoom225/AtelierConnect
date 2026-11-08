package com.atelierconnect.controller;

import com.atelierconnect.config.JwtConfig;
import com.atelierconnect.entity.*;
import com.atelierconnect.entity.enums.TypeParticipant;
import com.atelierconnect.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class PenaltyCreationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtConfig jwt;
    @Autowired ObjectMapper json;
    @Autowired CentreRepository centres;
    @Autowired SalleRepository salles;
    @Autowired ParticipantRepository participants;
    @Autowired PenaliteRepository penalites;
    @Autowired AtelierRepository ateliers;
    @Autowired ReservationRepository reservations;
    @Autowired PaiementRepository paiements;

    @ParameterizedTest(name = "{0} : fin de suspension J{1}, création autorisée={2}")
    @CsvSource({"PUBLIC,-1,true", "PRIVE,-1,true", "PUBLIC,0,false", "PRIVE,0,false",
            "PUBLIC,1,false", "PRIVE,1,false"})
    void penaltyIncludesItsFinalDayForBothCreationModes(String type, int endOffset, boolean allowed) throws Exception {
        LocalDate today = LocalDate.now();
        Centre centre = centres.save(Centre.builder().nom("Centre test suspension " + UUID.randomUUID())
                .adresse("Test isolé").heureOuverture(LocalTime.of(8, 0)).heureFermeture(LocalTime.of(22, 0))
                .dureeAtelierMinutes(120).dureeEntreAtelierMinutes(20).anneeCivile(today.getYear())
                .ouvertVisiteurs(true).build());
        Salle salle = salles.save(Salle.builder().nom("Salle test suspension").centre(centre).build());
        Participant participant = participants.save(Participant.builder().matricule("PEN-" + UUID.randomUUID())
                .nom("Participant").prenom("Suspension").typeParticipant(TypeParticipant.VISITEUR).solde(0.0).build());
        penalites.saveAndFlush(Penalite.builder().participant(participant).dateFin(today.plusDays(endOffset))
                .motif("Test de la date de fin inclusive").build());
        String token = jwt.generateToken(participant.getMatricule(), "VISITEUR", "MEMBER");

        mvc.perform(get("/api/participants/{id}/penalty", participant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().string(Boolean.toString(!allowed)));

        long atelierCount = ateliers.count(), reservationCount = reservations.count(), paiementCount = paiements.count();
        var result = mvc.perform(post("/api/ateliers").header("Authorization", "Bearer " + token)
                .contentType("application/json").content(json.writeValueAsString(Map.of(
                        "salleId", salle.getId(), "atelierDate", today.plusDays(1).atTime(9, 0).toString(),
                        "atelierType", type))));
        if (allowed) {
            result.andExpect(status().isCreated()).andExpect(jsonPath("$.typeAtelier").value(type));
            assertThat(ateliers.count()).isEqualTo(atelierCount + 1);
            assertThat(reservations.count()).isEqualTo(reservationCount + 1);
            assertThat(paiements.count()).isEqualTo(paiementCount + 1);
        } else {
            result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
                    .value("Vous avez une suspension active et ne pouvez pas créer d'atelier."));
            assertThat(ateliers.count()).isEqualTo(atelierCount);
            assertThat(reservations.count()).isEqualTo(reservationCount);
            assertThat(paiements.count()).isEqualTo(paiementCount);
        }
    }
}
