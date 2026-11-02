package com.atelierconnect.dto.response;

import java.time.LocalDate;

public record JourFermetureResponse(
        Long id,
        LocalDate date,
        String raison,
        Boolean global,
        Long centreId,
        String centreNom
) {

    // Wire compatibility for existing clients; business code uses canonical properties.
    @com.fasterxml.jackson.annotation.JsonProperty("siteId") public Long legacySiteId() { return centreId; }
}
