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

    public SalleResponse toResponse(Salle salle) {
        return SalleResponse.builder()
                .id(salle.getId())
                .nom(salle.getNom())
                .centreId(salle.getCentre() != null ? salle.getCentre().getId() : null)
                .centreNom(salle.getCentre() != null ? salle.getCentre().getNom() : null)
                .prix(salle.getPrix())
                .build();
    }
}
