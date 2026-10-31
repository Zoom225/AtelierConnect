package com.padell.padell.mapper;

import com.padell.padell.dto.request.SiteRequest;
import com.padell.padell.dto.response.SiteResponse;
import com.padell.padell.entity.Centre;
import org.springframework.stereotype.Component;

@Component
public class SiteMapper {

    public Centre toEntity(SiteRequest request) {
        if (request == null) {
            return null;
        }
        return Centre.builder()
                .nom(request.getNom())
                .adresse(request.getAdresse())
                .heureOuverture(request.getHeureOuverture())
                .heureFermeture(request.getHeureFermeture())
                .dureeMatchMinutes(request.getDureeMatchMinutes())
                .dureeEntreMatchMinutes(request.getDureeEntreMatchMinutes())
                .anneeCivile(request.getAnneeCivile())
                .ouvertVisiteurs(Boolean.TRUE.equals(request.getOuvertVisiteurs()))
                .build();
    }

    public SiteResponse toResponse(Centre site) {
        if (site == null) {
            return null;
        }
        // et montre la robustesse. Le simple fait d'utiliser le builder sur une entité
        // avec des champs null ne lèvera pas de NullPointerException.
        // L'erreur venait probablement d'une entité Site elle-même nulle dans la liste.
        // Ajouter cette vérification de nullité pour l'entité entière est la clé.
        return SiteResponse.builder()
                .id(site.getId())
                .nom(site.getNom())
                .adresse(site.getAdresse())
                .heureOuverture(site.getHeureOuverture())
                .heureFermeture(site.getHeureFermeture())
                .dureeMatchMinutes(site.getDureeMatchMinutes())
                .dureeEntreMatchMinutes(site.getDureeEntreMatchMinutes())
                .anneeCivile(site.getAnneeCivile())
                .ouvertVisiteurs(Boolean.TRUE.equals(site.getOuvertVisiteurs()))
                .build();
    }
}
