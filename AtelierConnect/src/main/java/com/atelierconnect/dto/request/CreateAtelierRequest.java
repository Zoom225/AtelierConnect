package com.atelierconnect.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

public record CreateAtelierRequest(
    @NotNull(message = "L'ID de la salle est obligatoire")
    @com.fasterxml.jackson.annotation.JsonAlias("terrainId") Long salleId,

    @NotNull(message = "La date et l'heure de l'atelier sont obligatoires")
    @FutureOrPresent(message = "La date de l'atelier doit etre dans le futur ou le present")
    @com.fasterxml.jackson.annotation.JsonAlias("matchDate") LocalDateTime atelierDate,

    @NotBlank(message = "Le type de atelier est obligatoire")
    @Pattern(regexp = "PUBLIC|PRIVE", message = "Le type de atelier doit etre PUBLIC ou PRIVE")
    @com.fasterxml.jackson.annotation.JsonAlias("matchType") String atelierType // PUBLIC ou PRIVE
) {}
