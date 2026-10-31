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
    private Long siteId;
    private String siteNom;
    private Double prix;

    @JsonProperty("centreId") public Long centreId() { return siteId; }
    @JsonProperty("centreNom") public String centreNom() { return siteNom; }
}
