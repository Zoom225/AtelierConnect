package com.padell.padell.mapper;

import com.padell.padell.dto.request.SalleRequest;
import com.padell.padell.dto.response.SalleResponse;
import com.padell.padell.entity.Salle;
import org.springframework.stereotype.Component;

@Component
public class SalleMapper {

    public Salle toEntity(SalleRequest request) {
        return Salle.builder()
                .nom(request.getNom())
                .build();
    }

    public SalleResponse toResponse(Salle terrain) {
        return SalleResponse.builder()
                .id(terrain.getId())
                .nom(terrain.getNom())
                .siteId(terrain.getSite() != null ? terrain.getSite().getId() : null)
                .siteNom(terrain.getSite() != null ? terrain.getSite().getNom() : null)
                .prix(terrain.getPrix())
                .build();
    }
}
