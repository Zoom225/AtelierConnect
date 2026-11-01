package com.atelierconnect.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRequest {

    @NotNull(message = "L'ID de l'atelier est obligatoire")
    @com.fasterxml.jackson.annotation.JsonAlias("matchId")
    private Long atelierId;

    @NotNull(message = "L'ID du participant est obligatoire")
    @com.fasterxml.jackson.annotation.JsonAlias("membreId")
    private Long participantId;

    @NotNull(message = "L'ID du demandeur est obligatoire")
    private Long requesterId;
}
