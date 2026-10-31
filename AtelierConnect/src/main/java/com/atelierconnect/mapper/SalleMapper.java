package com.atelierconnect.mapper;

import com.atelierconnect.dto.request.SalleRequest;
import com.atelierconnect.dto.response.SalleResponse;
import com.atelierconnect.entity.Salle;
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
