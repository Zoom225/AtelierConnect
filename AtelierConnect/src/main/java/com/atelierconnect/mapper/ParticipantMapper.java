package com.atelierconnect.mapper;

import com.atelierconnect.dto.request.ParticipantRequest;
import com.atelierconnect.dto.response.ParticipantResponse;
import com.atelierconnect.entity.Participant;
import org.springframework.stereotype.Component;

@Component
public class ParticipantMapper {

    public Participant toEntity(ParticipantRequest request) {
        return Participant.builder()
                .matricule(request.getMatricule())
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .typeMembre(request.getCategorie() != null
                        ? request.getCategorie() : request.getTypeMembre().toCategorie())
                .build();
        // le site est résolu dans le service via siteId
    }

    public ParticipantResponse toResponse(Participant membre) {
        return ParticipantResponse.builder()
                .id(membre.getId())
                .matricule(membre.getMatricule())
                .nom(membre.getNom())
                .prenom(membre.getPrenom())
                .email(membre.getEmail())
                .typeMembre(membre.getTypeMembre().toLegacyType())
                .categorie(membre.getTypeMembre())
                .siteId(membre.getSite() != null ? membre.getSite().getId() : null)
                .siteNom(membre.getSite() != null ? membre.getSite().getNom() : null)
                .solde(membre.getSolde())
                .build();
    }
}
