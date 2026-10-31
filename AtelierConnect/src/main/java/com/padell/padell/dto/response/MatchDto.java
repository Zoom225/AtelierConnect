package com.padell.padell.dto.response;

import com.padell.padell.entity.enums.StatutAtelier;
import com.padell.padell.entity.enums.TypeAtelier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record MatchDto(
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
) {}
