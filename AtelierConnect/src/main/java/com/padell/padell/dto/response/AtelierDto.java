package com.padell.padell.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.padell.padell.entity.enums.StatutAtelier;
import com.padell.padell.entity.enums.TypeAtelier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AtelierDto(
        Long id,
        Long terrainId,
        String terrainNom,
        String siteNom,
        Long organisateurId,
        String organisateurNom,
        LocalDate date,
        LocalTime heureDebut,
        LocalTime heureFin,
        TypeAtelier typeMatch,
        StatutAtelier statut,
        Integer nbJoueursActuels,
        Double prixParJoueur,
        LocalDateTime dateConversionPublic
) {
    @JsonProperty("salleId") public Long salleId() { return terrainId; }
    @JsonProperty("salleNom") public String salleNom() { return terrainNom; }
    @JsonProperty("centreNom") public String centreNom() { return siteNom; }
    @JsonProperty("typeAtelier") public TypeAtelier typeAtelier() { return typeMatch; }
    @JsonProperty("nbParticipantsActuels") public Integer nbParticipantsActuels() { return nbJoueursActuels; }
    @JsonProperty("prixParParticipant") public Double prixParParticipant() { return prixParJoueur; }
}
