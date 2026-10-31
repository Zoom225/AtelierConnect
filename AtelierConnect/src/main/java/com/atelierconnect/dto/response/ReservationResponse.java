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
    private Long matchId;
    private LocalDateTime matchDateTime;
    private Long membreId;
    private String membreNom;
    private StatutReservation statut;
    private PaiementResponse paiement;

    @JsonProperty("atelierId") public Long atelierId() { return matchId; }
    @JsonProperty("atelierDateTime") public LocalDateTime atelierDateTime() { return matchDateTime; }
    @JsonProperty("participantId") public Long participantId() { return membreId; }
    @JsonProperty("participantNom") public String participantNom() { return membreNom; }
}
