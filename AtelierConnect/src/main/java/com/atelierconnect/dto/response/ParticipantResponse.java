package com.atelierconnect.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.atelierconnect.entity.enums.TypeParticipant;
import com.atelierconnect.entity.enums.TypeParticipantLegacy;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantResponse {
    private Long id;
    private String matricule;
    private String nom;
    private String prenom;
    private String email;
    private TypeParticipantLegacy typeParticipant;
    private TypeParticipant categorie;
    private Long centreId;
    private String centreNom;
    private Double solde;
    private String token;

    // Wire compatibility for existing clients; business code uses canonical properties.
    @com.fasterxml.jackson.annotation.JsonProperty("typeMembre") public TypeParticipantLegacy legacyTypeMembre() { return typeParticipant; }
    @com.fasterxml.jackson.annotation.JsonProperty("siteId") public Long legacySiteId() { return centreId; }
    @com.fasterxml.jackson.annotation.JsonProperty("siteNom") public String legacySiteNom() { return centreNom; }
}
