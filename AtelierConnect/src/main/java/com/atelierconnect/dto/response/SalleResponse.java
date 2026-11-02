package com.atelierconnect.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalleResponse {
    private Long id;
    private String nom;
    private Long centreId;
    private String centreNom;
    private Double prix;

    // Wire compatibility for existing clients; business code uses canonical properties.
    @com.fasterxml.jackson.annotation.JsonProperty("siteId") public Long legacySiteId() { return centreId; }
    @com.fasterxml.jackson.annotation.JsonProperty("siteNom") public String legacySiteNom() { return centreNom; }
}
