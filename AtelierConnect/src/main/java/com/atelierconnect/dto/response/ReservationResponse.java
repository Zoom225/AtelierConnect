package com.atelierconnect.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.atelierconnect.entity.enums.StatutReservation;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {
    private Long id;
    private Long atelierId;
    private LocalDateTime atelierDateTime;
    private Long participantId;
    private String participantNom;
    private StatutReservation statut;
    private PaiementResponse paiement;

    // Wire compatibility for existing clients; business code uses canonical properties.
    @com.fasterxml.jackson.annotation.JsonProperty("matchId") public Long legacyMatchId() { return atelierId; }
    @com.fasterxml.jackson.annotation.JsonProperty("matchDateTime") public LocalDateTime legacyMatchDateTime() { return atelierDateTime; }
    @com.fasterxml.jackson.annotation.JsonProperty("membreId") public Long legacyMembreId() { return participantId; }
    @com.fasterxml.jackson.annotation.JsonProperty("membreNom") public String legacyMembreNom() { return participantNom; }
}
