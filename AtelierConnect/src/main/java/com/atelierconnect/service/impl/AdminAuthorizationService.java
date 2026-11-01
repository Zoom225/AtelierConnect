package com.atelierconnect.service.impl;

import com.atelierconnect.dto.response.AtelierDto;
import com.atelierconnect.entity.Administrateur;
import com.atelierconnect.entity.Atelier;
import com.atelierconnect.entity.Participant;
import com.atelierconnect.entity.Salle;
import com.atelierconnect.entity.enums.TypeAdministrateur;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.repository.AdministrateurRepository;
import com.atelierconnect.repository.AtelierRepository;
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
    private final AtelierRepository atelierRepository;

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

    public void checkCentreAccess(Long centreId) {
        Administrateur admin = currentAdmin();
        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return;
        }
        if (admin.getCentre() == null || !admin.getCentre().getId().equals(centreId)) {
            throw new BusinessException("Un admin SITE ne peut agir que sur son propre centre.");
        }
    }

    public void checkSalleAccess(Salle salle) {
        checkCentreAccess(salle.getCentre().getId());
    }

    public void checkAtelierAccess(Long atelierId) {
        Atelier atelier = atelierRepository.findById(atelierId)
                .orElseThrow(() -> new BusinessException("Atelier introuvable avec l'ID : " + atelierId));
        checkCentreAccess(atelier.getSalle().getCentre().getId());
    }

    public void checkParticipantAccess(Participant participant) {
        checkParticipantAccess(participant != null && participant.getCentre() != null ? participant.getCentre().getId() : null);
    }

    public void checkParticipantAccess(Long centreId) {
        Administrateur admin = currentAdmin();
        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return;
        }
        if (centreId == null || admin.getCentre() == null || !centreId.equals(admin.getCentre().getId())) {
            throw new BusinessException("Un admin SITE ne peut gérer que les participants de son centre.");
        }
    }

    public boolean canAccessParticipant(Participant participant) {
        try {
            checkParticipantAccess(participant);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    public boolean canAccessParticipantCentre(Long centreId) {
        try {
            checkParticipantAccess(centreId);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    public List<Participant> filterParticipants(List<Participant> participants) {
        Administrateur admin = currentAdmin();
        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return participants;
        }
        Long centreId = admin.getCentre() != null ? admin.getCentre().getId() : null;
        return participants.stream()
                .filter(participant -> participant.getCentre() != null && participant.getCentre().getId().equals(centreId))
                .toList();
    }

    public List<Atelier> filterAteliers(List<Atelier> ateliers) {
        Administrateur admin = currentAdmin();
        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return ateliers;
        }
        Long centreId = admin.getCentre() != null ? admin.getCentre().getId() : null;
        return ateliers.stream()
                .filter(atelier -> atelier.getSalle() != null
                        && atelier.getSalle().getCentre() != null
                        && atelier.getSalle().getCentre().getId().equals(centreId))
                .toList();
    }

    public List<AtelierDto> filterAtelierDtos(List<AtelierDto> ateliers) {
        Administrateur admin = currentAdmin();
        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return ateliers;
        }
        Long centreId = admin.getCentre() != null ? admin.getCentre().getId() : null;
        if (centreId == null) return List.of();
        var allowedIds = atelierRepository.findBySalleCentreId(centreId).stream()
                .map(Atelier::getId).collect(java.util.stream.Collectors.toSet());
        return ateliers.stream()
                .filter(atelier -> allowedIds.contains(atelier.id()))
                .toList();
    }
}
