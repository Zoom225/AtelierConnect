package com.padell.padell.mapper;

import com.padell.padell.dto.request.MembreRequest;
import com.padell.padell.dto.response.MembreResponse;
import com.padell.padell.entity.Participant;
import org.springframework.stereotype.Component;

@Component
public class MembreMapper {

    public Participant toEntity(MembreRequest request) {
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

    public MembreResponse toResponse(Participant membre) {
        return MembreResponse.builder()
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
