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
    private Integer dureeAtelierMinutes;
    private Integer dureeEntreAtelierMinutes;
    private Integer anneeCivile;
    private Boolean ouvertVisiteurs;

    @JsonProperty("pauseAtelierMinutes") public Integer pauseAtelierMinutes() { return dureeEntreAtelierMinutes; }

    // Wire compatibility for existing clients; business code uses canonical properties.
    @com.fasterxml.jackson.annotation.JsonProperty("dureeMatchMinutes") public Integer legacyDureeMatchMinutes() { return dureeAtelierMinutes; }
    @com.fasterxml.jackson.annotation.JsonProperty("dureeEntreMatchMinutes") public Integer legacyDureeEntreMatchMinutes() { return dureeEntreAtelierMinutes; }
}
