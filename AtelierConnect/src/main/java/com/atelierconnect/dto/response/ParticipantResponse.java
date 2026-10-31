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
    private TypeParticipantLegacy typeMembre;
    private TypeParticipant categorie;
    private Long siteId;
    private String siteNom;
    private Double solde;
    private String token;

    @JsonProperty("centreId") public Long centreId() { return siteId; }
    @JsonProperty("centreNom") public String centreNom() { return siteNom; }
}
