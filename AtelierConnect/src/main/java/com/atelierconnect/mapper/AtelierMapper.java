package com.atelierconnect.mapper;

import com.atelierconnect.entity.Atelier;
import com.atelierconnect.entity.Participant;
import com.atelierconnect.entity.Centre;
import com.atelierconnect.entity.Salle;
import com.atelierconnect.dto.response.AtelierDto;
import org.springframework.stereotype.Service;

@Service
public class AtelierMapper {

    public AtelierDto toAtelierDto(Atelier match) {
        if (match == null) {
            return null;
        }

        Salle terrain = match.getTerrain();
        Participant organisateur = match.getOrganisateur();
        Centre site = (terrain != null) ? terrain.getSite() : null;

        Long terrainId = (terrain != null) ? terrain.getId() : null;
        String terrainNom = (terrain != null) ? terrain.getNom() : null;
        String siteNom = (site != null) ? site.getNom() : null;
        Long organisateurId = (organisateur != null) ? organisateur.getId() : null;
        String organisateurNom = (organisateur != null && organisateur.getPrenom() != null && organisateur.getNom() != null)
                ? organisateur.getPrenom() + " " + organisateur.getNom()
                : null;

        return new AtelierDto(
                match.getId(),
                terrainId,
                terrainNom,
                siteNom,
                organisateurId,
                organisateurNom,
                match.getDateDebut() != null ? match.getDateDebut().toLocalDate() : null,
                match.getDateDebut() != null ? match.getDateDebut().toLocalTime() : null,
                match.getDateFin() != null ? match.getDateFin().toLocalTime() : null,
                match.getTypeMatch(),
                match.getStatut(),
                match.getNbJoueursActuels(),
                match.getPrixParJoueur(),
                match.getDateConversionPublic()
        );
    }
}
