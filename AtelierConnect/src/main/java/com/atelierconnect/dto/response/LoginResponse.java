package com.atelierconnect.dto.response;

import com.atelierconnect.entity.enums.TypeAdministrateur;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    private String email;
    private String nom;
    private String prenom;
    private TypeAdministrateur role;
    private Long centreId;

    // Wire compatibility for existing clients; business code uses canonical properties.
    @com.fasterxml.jackson.annotation.JsonProperty("siteId") public Long legacySiteId() { return centreId; }
}
