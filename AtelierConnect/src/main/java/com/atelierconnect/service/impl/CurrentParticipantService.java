package com.atelierconnect.service.impl;

import com.atelierconnect.entity.Participant;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentParticipantService {

    private final ParticipantRepository membreRepository;

    public Participant currentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException("Authentification membre requise.");
        }

        return membreRepository.findByMatriculeIgnoreCase(authentication.getName())
                .orElseThrow(() -> new BusinessException("Membre connecté introuvable."));
    }

    public Long currentMemberId() {
        return currentMember().getId();
    }

    public void requireCurrentMember(Long membreId) {
        if (!currentMemberId().equals(membreId)) {
            throw new BusinessException("Accès refusé : cette ressource appartient à un autre membre.");
        }
    }

    public boolean isCurrentMemberId(Long membreId) {
        try {
            requireCurrentMember(membreId);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }
}
