package com.atelierconnect.mapper;

import com.atelierconnect.entity.Atelier;
import com.atelierconnect.entity.Participant;
import com.atelierconnect.entity.Centre;
import com.atelierconnect.entity.Salle;
import com.atelierconnect.dto.response.AtelierDto;
import org.springframework.stereotype.Service;

@Service
public class AtelierMapper {

    public AtelierDto toAtelierDto(Atelier atelier) {
        if (atelier == null) {
            return null;
        }

        Salle salle = atelier.getSalle();
        Participant organisateur = atelier.getOrganisateur();
        Centre centre = (salle != null) ? salle.getCentre() : null;

        Long salleId = (salle != null) ? salle.getId() : null;
        String salleNom = (salle != null) ? salle.getNom() : null;
        String centreNom = (centre != null) ? centre.getNom() : null;
        Long organisateurId = (organisateur != null) ? organisateur.getId() : null;
        String organisateurNom = (organisateur != null && organisateur.getPrenom() != null && organisateur.getNom() != null)
                ? organisateur.getPrenom() + " " + organisateur.getNom()
                : null;

        return new AtelierDto(
                atelier.getId(),
                salleId,
                salleNom,
                centreNom,
                organisateurId,
                organisateurNom,
                atelier.getDateDebut() != null ? atelier.getDateDebut().toLocalDate() : null,
                atelier.getDateDebut() != null ? atelier.getDateDebut().toLocalTime() : null,
                atelier.getDateFin() != null ? atelier.getDateFin().toLocalTime() : null,
                atelier.getTypeAtelier(),
                atelier.getStatut(),
                atelier.getNbParticipantsActuels(),
                atelier.getPrixParParticipant(),
                atelier.getDateConversionPublic(),
                atelier.getAccordOuverturePublique(),
                atelier.getDateAccordOuverturePublique()
        );
    }
}
