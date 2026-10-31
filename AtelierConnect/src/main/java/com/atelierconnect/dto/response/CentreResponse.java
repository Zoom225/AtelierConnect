package com.atelierconnect.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CentreResponse {
    private Long id;
    private String nom;
    private String adresse;
    private LocalTime heureOuverture;
    private LocalTime heureFermeture;
    private Integer dureeMatchMinutes;
    private Integer dureeEntreMatchMinutes;
    private Integer anneeCivile;
    private Boolean ouvertVisiteurs;

    @JsonProperty("dureeAtelierMinutes") public Integer dureeAtelierMinutes() { return dureeMatchMinutes; }
    @JsonProperty("pauseAtelierMinutes") public Integer pauseAtelierMinutes() { return dureeEntreMatchMinutes; }
}
