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
                .typeParticipant(request.getCategorie() != null
                        ? request.getCategorie() : request.getTypeParticipant().toCategorie())
                .build();
        // le centre est résolu dans le service via siteId
    }

    public ParticipantResponse toResponse(Participant participant) {
        return ParticipantResponse.builder()
                .id(participant.getId())
                .matricule(participant.getMatricule())
                .nom(participant.getNom())
                .prenom(participant.getPrenom())
                .email(participant.getEmail())
                .typeParticipant(participant.getTypeParticipant().toLegacyType())
                .categorie(participant.getTypeParticipant())
                .centreId(participant.getCentre() != null ? participant.getCentre().getId() : null)
                .centreNom(participant.getCentre() != null ? participant.getCentre().getNom() : null)
                .solde(participant.getSolde())
                .build();
    }
}
