package com.padell.padell.mapper;

import com.padell.padell.dto.request.TerrainRequest;
import com.padell.padell.dto.response.TerrainResponse;
import com.padell.padell.entity.Salle;
import org.springframework.stereotype.Component;

@Component
public class TerrainMapper {

    public Salle toEntity(TerrainRequest request) {
        return Salle.builder()
                .nom(request.getNom())
                .build();
    }

    public TerrainResponse toResponse(Salle terrain) {
        return TerrainResponse.builder()
                .id(terrain.getId())
                .nom(terrain.getNom())
                .siteId(terrain.getSite() != null ? terrain.getSite().getId() : null)
                .siteNom(terrain.getSite() != null ? terrain.getSite().getNom() : null)
                .prix(terrain.getPrix())
                .build();
    }
}
