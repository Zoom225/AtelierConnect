package com.padell.padell.service.impl;

import com.padell.padell.dto.response.AtelierDto;
import com.padell.padell.entity.Administrateur;
import com.padell.padell.entity.Atelier;
import com.padell.padell.entity.Participant;
import com.padell.padell.entity.Salle;
import com.padell.padell.entity.enums.TypeAdministrateur;
import com.padell.padell.exception.BusinessException;
import com.padell.padell.repository.AdministrateurRepository;
import com.padell.padell.repository.AtelierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuthorizationService {

    private final AdministrateurRepository administrateurRepository;
    private final AtelierRepository matchRepository;

    public Administrateur currentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException("Authentification administrateur requise.");
        }

        return administrateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new BusinessException("Administrateur introuvable."));
    }

    public void requireGlobalAdmin() {
        if (currentAdmin().getTypeAdministrateur() != TypeAdministrateur.GLOBAL) {
            throw new BusinessException("Action réservée à un admin GLOBAL.");
        }
    }

    public void checkSiteAccess(Long siteId) {
        Administrateur admin = currentAdmin();
        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return;
        }
        if (admin.getSite() == null || !admin.getSite().getId().equals(siteId)) {
            throw new BusinessException("Un admin SITE ne peut agir que sur son propre site.");
        }
    }

    public void checkTerrainAccess(Salle terrain) {
        checkSiteAccess(terrain.getSite().getId());
    }

    public void checkMatchAccess(Long matchId) {
        Atelier match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException("Match introuvable avec l'ID : " + matchId));
        checkSiteAccess(match.getTerrain().getSite().getId());
    }

    public void checkMembreAccess(Participant membre) {
        checkMembreAccess(membre != null && membre.getSite() != null ? membre.getSite().getId() : null);
    }

    public void checkMembreAccess(Long siteId) {
        Administrateur admin = currentAdmin();
        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return;
        }
        if (siteId == null || admin.getSite() == null || !siteId.equals(admin.getSite().getId())) {
            throw new BusinessException("Un admin SITE ne peut gérer que les membres de son site.");
        }
    }

    public boolean canAccessMembre(Participant membre) {
        try {
            checkMembreAccess(membre);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    public boolean canAccessMembreSite(Long siteId) {
        try {
            checkMembreAccess(siteId);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    public List<Participant> filterMembres(List<Participant> membres) {
        Administrateur admin = currentAdmin();
        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return membres;
        }
        Long siteId = admin.getSite() != null ? admin.getSite().getId() : null;
        return membres.stream()
                .filter(membre -> membre.getSite() != null && membre.getSite().getId().equals(siteId))
                .toList();
    }

    public List<Atelier> filterMatches(List<Atelier> matches) {
        Administrateur admin = currentAdmin();
        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return matches;
        }
        Long siteId = admin.getSite() != null ? admin.getSite().getId() : null;
        return matches.stream()
                .filter(match -> match.getTerrain() != null
                        && match.getTerrain().getSite() != null
                        && match.getTerrain().getSite().getId().equals(siteId))
                .toList();
    }

    public List<AtelierDto> filterMatchDtos(List<AtelierDto> matches) {
        Administrateur admin = currentAdmin();
        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return matches;
        }
        String siteNom = admin.getSite() != null ? admin.getSite().getNom() : null;
        return matches.stream()
                .filter(match -> match.siteNom() != null && match.siteNom().equals(siteNom))
                .toList();
    }
}
