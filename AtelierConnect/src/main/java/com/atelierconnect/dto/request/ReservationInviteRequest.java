package com.atelierconnect.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationInviteRequest {

    @NotNull(message = "L'ID de l'atelier est obligatoire")
    @com.fasterxml.jackson.annotation.JsonAlias("matchId")
    private Long atelierId;

    @NotEmpty(message = "Au moins un matricule invité est obligatoire")
    private List<String> inviteeMatricules;
}
