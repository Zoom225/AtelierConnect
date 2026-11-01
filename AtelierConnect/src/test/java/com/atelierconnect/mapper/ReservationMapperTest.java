package com.atelierconnect.mapper;

import com.atelierconnect.dto.response.PaiementResponse;
import com.atelierconnect.dto.response.ReservationResponse;
import com.atelierconnect.entity.Atelier;
import com.atelierconnect.entity.Participant;
import com.atelierconnect.entity.Paiement;
import com.atelierconnect.entity.Reservation;
import com.atelierconnect.entity.enums.StatutPaiement;
import com.atelierconnect.entity.enums.StatutReservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de ReservationMapper")
class ReservationMapperTest {

    @Mock
    private PaiementMapper paiementMapper;

    @InjectMocks
    private ReservationMapper reservationMapper;

    private Reservation reservation;
    private Atelier atelier;
    private Participant participant;
    private Paiement paiement;

    @BeforeEach
    void setUp() {
        atelier = new Atelier();
        atelier.setId(1L);
        atelier.setDateDebut(LocalDateTime.of(2024, 8, 20, 19, 0));

        participant = new Participant();
        participant.setId(10L);
        participant.setNom("Doe");
        participant.setPrenom("John");

        paiement = new Paiement();
        paiement.setId(100L);
        paiement.setStatut(StatutPaiement.EN_ATTENTE);

        reservation = new Reservation();
        reservation.setId(1L);
        reservation.setAtelier(atelier);
        reservation.setParticipant(participant);
        reservation.setStatut(StatutReservation.EN_ATTENTE);
        reservation.setPaiement(paiement);
    }

    @Test
    @DisplayName("Doit mapper Reservation Entity vers ReservationResponse")
    void shouldMapReservationEntityToReservationResponse() {
        // Arrange
        PaiementResponse paiementResponse = PaiementResponse.builder()
                .id(100L)
                .statut(StatutPaiement.EN_ATTENTE)
                .build();
        when(paiementMapper.toResponse(paiement)).thenReturn(paiementResponse);

        // Act
        ReservationResponse response = reservationMapper.toResponse(reservation);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(1L, response.getAtelierId());
        assertEquals(LocalDateTime.of(2024, 8, 20, 19, 0), response.getAtelierDateTime());
        assertEquals(10L, response.getParticipantId());
        assertEquals("John Doe", response.getParticipantNom()); // Correction de l'ordre
        assertEquals(StatutReservation.EN_ATTENTE, response.getStatut());
        assertNotNull(response.getPaiement());
        assertEquals(100L, response.getPaiement().getId());
    }

    @Test
    @DisplayName("Doit gérer un Paiement null lors du mapping")
    void shouldHandleNullPaiementWhenMapping() {
        // Arrange
        reservation.setPaiement(null);

        // Act
        ReservationResponse response = reservationMapper.toResponse(reservation);

        // Assert
        assertNotNull(response);
        assertNull(response.getPaiement());
    }

    @Test
    @DisplayName("Doit lever une exception si Atelier ou Participant est null")
    void shouldThrowExceptionIfRequiredEntitiesAreNull() {
        // Arrange 1
        reservation.setAtelier(null);

        // Act & Assert 1
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> {
            reservationMapper.toResponse(reservation);
        });
        assertEquals("La réservation doit avoir un atelier et un participant associés.", ex1.getMessage());

        // Arrange 2
        reservation.setAtelier(atelier); // Rétablir l'atelier
        reservation.setParticipant(null);

        // Act & Assert 2
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> {
            reservationMapper.toResponse(reservation);
        });
        assertEquals("La réservation doit avoir un atelier et un participant associés.", ex2.getMessage());
    }
}
