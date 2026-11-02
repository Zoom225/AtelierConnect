package com.atelierconnect.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.atelierconnect.entity.enums.TypeAtelier;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtelierRequest {

    @NotNull(message = "L'identifiant de la salle est obligatoire.")
    @com.fasterxml.jackson.annotation.JsonAlias("terrainId")
    private Long salleId;

    @NotNull(message = "L'ID de l'organisateur est obligatoire.")
    private Long organisateurId;

    @NotNull(message = "La date est obligatoire.")
    @Future(message = "La date de l'atelier doit être dans le futur.")
    private LocalDate date;

    @NotNull(message = "L'heure de début est obligatoire.")
    private LocalTime heureDebut;

    @NotNull(message = "Le type d'atelier est obligatoire.")
    @com.fasterxml.jackson.annotation.JsonAlias("typeMatch")
    private TypeAtelier typeAtelier;
}
