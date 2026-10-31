package com.padell.padell.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRequest {

    @NotNull(message = "L'ID du match est obligatoire")
    @JsonAlias("atelierId")
    private Long matchId;

    @NotNull(message = "L'ID du membre est obligatoire")
    @JsonAlias("participantId")
    private Long membreId;

    @NotNull(message = "L'ID du demandeur est obligatoire")
    private Long requesterId;
}
