package com.atelierconnect.mapper;

import com.atelierconnect.dto.request.CentreRequest;
import com.atelierconnect.dto.response.CentreResponse;
import com.atelierconnect.entity.Centre;
import org.springframework.stereotype.Component;

@Component
public class CentreMapper {

    public Centre toEntity(CentreRequest request) {
        if (request == null) {
            return null;
        }
        return Centre.builder()
                .nom(request.getNom())
                .adresse(request.getAdresse())
                .heureOuverture(request.getHeureOuverture())
                .heureFermeture(request.getHeureFermeture())
                .dureeAtelierMinutes(request.getDureeAtelierMinutes())
                .dureeEntreAtelierMinutes(request.getDureeEntreAtelierMinutes())
                .anneeCivile(request.getAnneeCivile())
                .ouvertVisiteurs(Boolean.TRUE.equals(request.getOuvertVisiteurs()))
                .build();
    }

    public CentreResponse toResponse(Centre centre) {
        if (centre == null) {
            return null;
        }
        // et montre la robustesse. Le simple fait d'utiliser le builder sur une entité
        // avec des champs null ne lèvera pas de NullPointerException.
        // L'erreur venait probablement d'une entité Centre elle-même nulle dans la liste.
        // Ajouter cette vérification de nullité pour l'entité entière est la clé.
        return CentreResponse.builder()
                .id(centre.getId())
                .nom(centre.getNom())
                .adresse(centre.getAdresse())
                .heureOuverture(centre.getHeureOuverture())
                .heureFermeture(centre.getHeureFermeture())
                .dureeAtelierMinutes(centre.getDureeAtelierMinutes())
                .dureeEntreAtelierMinutes(centre.getDureeEntreAtelierMinutes())
                .anneeCivile(centre.getAnneeCivile())
                .ouvertVisiteurs(Boolean.TRUE.equals(centre.getOuvertVisiteurs()))
                .build();
    }
}
