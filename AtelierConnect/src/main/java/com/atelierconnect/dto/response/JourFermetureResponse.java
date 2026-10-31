package com.atelierconnect.dto.response;

import java.time.LocalDate;

public record JourFermetureResponse(
        Long id,
        LocalDate date,
        String raison,
        Boolean global,
        Long siteId,
        String siteNom
) {
}
