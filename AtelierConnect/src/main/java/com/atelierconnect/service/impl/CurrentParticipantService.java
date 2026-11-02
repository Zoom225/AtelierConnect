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

    private final ParticipantRepository participantRepository;

    public Participant currentParticipant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException("Authentification participant requise.");
        }

        return participantRepository.findByMatriculeIgnoreCase(authentication.getName())
                .orElseThrow(() -> new BusinessException("Participant connecté introuvable."));
    }

    public Long currentParticipantId() {
        return currentParticipant().getId();
    }

    public void requireCurrentParticipant(Long participantId) {
        if (!currentParticipantId().equals(participantId)) {
            throw new BusinessException("Accès refusé : cette ressource appartient à un autre participant.");
        }
    }

    public boolean isCurrentParticipantId(Long participantId) {
        try {
            requireCurrentParticipant(participantId);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }
}
