package com.atelierconnect.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.atelierconnect.dto.request.CreateAtelierRequest;
import com.atelierconnect.dto.request.ReservationRequest;
import com.atelierconnect.dto.response.AtelierDto;
import com.atelierconnect.entity.enums.StatutAtelier;
import com.atelierconnect.entity.enums.TypeAtelier;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class DtoJsonCompatibilityTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void accepteLesNouveauxNomsSansCasserLesAnciens() throws Exception {
        CreateAtelierRequest request = mapper.readValue("""
                {"salleId": 3, "atelierDate": "2030-05-01T10:00:00", "atelierType": "PUBLIC"}
                """, CreateAtelierRequest.class);
        assertThat(request.terrainId()).isEqualTo(3L);
        assertThat(request.matchDate()).isEqualTo(LocalDateTime.of(2030, 5, 1, 10, 0));
        assertThat(request.matchType()).isEqualTo("PUBLIC");

        ReservationRequest reservation = mapper.readValue("""
                {"atelierId": 4, "participantId": 5, "requesterId": 5}
                """, ReservationRequest.class);
        assertThat(reservation.getMatchId()).isEqualTo(4L);
        assertThat(reservation.getMembreId()).isEqualTo(5L);
    }

    @Test
    void exposeLeConsentementEnJsonSansSupprimerLesAnciensChamps() throws Exception {
        LocalDateTime consentTime = LocalDateTime.of(2026, 10, 31, 11, 30);
        AtelierDto dto = new AtelierDto(7L, 2L, "Salle A", "Centre A", 3L, "Organisateur",
                LocalDate.of(2030, 5, 1), LocalTime.of(10, 0), LocalTime.of(12, 0),
                TypeAtelier.PRIVE, StatutAtelier.PLANIFIE, 3, 15.0, null,
                true, consentTime);

        String json = mapper.writeValueAsString(dto);

        assertThat(json).contains(
                "\"accordOuverturePublique\":true",
                "\"dateAccordOuverturePublique\":\"2026-10-31T11:30:00\"",
                "\"typeMatch\":\"PRIVE\"",
                "\"terrainId\":2");
    }

    @Test
    void exposeLesDeuxNomenclaturesEnReponse() throws Exception {
        AtelierDto dto = new AtelierDto(1L, 2L, "Salle A", "Centre A", 3L, "Organisateur",
                LocalDate.of(2030, 5, 1), LocalTime.of(10, 0), LocalTime.of(12, 0),
                TypeAtelier.PUBLIC, StatutAtelier.PLANIFIE, 1, 15.0, null);
        String json = mapper.writeValueAsString(dto);
        assertThat(json).contains("\"terrainId\":2", "\"salleId\":2",
                "\"typeMatch\":\"PUBLIC\"", "\"typeAtelier\":\"PUBLIC\"");
    }
}
