package com.atelierconnect.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.atelierconnect.entity.enums.StatutAtelier;
import com.atelierconnect.entity.enums.TypeAtelier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AtelierDto(
        Long id,
        Long salleId,
        String salleNom,
        String centreNom,
        Long organisateurId,
        String organisateurNom,
        LocalDate date,
        LocalTime heureDebut,
        LocalTime heureFin,
        TypeAtelier typeAtelier,
        StatutAtelier statut,
        Integer nbParticipantsActuels,
        Double prixParParticipant,
        LocalDateTime dateConversionPublic,
        Boolean accordOuverturePublique,
        LocalDateTime dateAccordOuverturePublique
) {
    // Constructeur transitoire : les consommateurs Java historiques continuent de compiler.
    public AtelierDto(
            Long id, Long salleId, String salleNom, String centreNom,
            Long organisateurId, String organisateurNom,
            LocalDate date, LocalTime heureDebut, LocalTime heureFin,
            TypeAtelier typeAtelier, StatutAtelier statut,
            Integer nbParticipantsActuels, Double prixParParticipant,
            LocalDateTime dateConversionPublic) {
        this(id, salleId, salleNom, centreNom, organisateurId, organisateurNom,
                date, heureDebut, heureFin, typeAtelier, statut, nbParticipantsActuels,
                prixParParticipant, dateConversionPublic, null, null);
    }

    // Wire compatibility for existing clients; business code uses canonical properties.
    @com.fasterxml.jackson.annotation.JsonProperty("terrainId") public Long legacyTerrainId() { return salleId; }
    @com.fasterxml.jackson.annotation.JsonProperty("terrainNom") public String legacyTerrainNom() { return salleNom; }
    @com.fasterxml.jackson.annotation.JsonProperty("siteNom") public String legacySiteNom() { return centreNom; }
    @com.fasterxml.jackson.annotation.JsonProperty("typeMatch") public TypeAtelier legacyTypeMatch() { return typeAtelier; }
    @com.fasterxml.jackson.annotation.JsonProperty("nbJoueursActuels") public Integer legacyNbJoueursActuels() { return nbParticipantsActuels; }
    @com.fasterxml.jackson.annotation.JsonProperty("prixParJoueur") public Double legacyPrixParJoueur() { return prixParParticipant; }
}
