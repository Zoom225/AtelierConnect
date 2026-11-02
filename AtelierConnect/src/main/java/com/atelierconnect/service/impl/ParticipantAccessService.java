package com.atelierconnect.service.impl;

import com.atelierconnect.entity.Participant;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParticipantAccessService {

    private final AdminAuthorizationService adminAuthorizationService;
    private final CurrentParticipantService currentParticipantService;
    private final ParticipantRepository participantRepository;

    public boolean canCreate(Long centreId) {
        return adminAuthorizationService.canAccessParticipantCentre(centreId);
    }

    public void assertCanCreate(Long centreId) {
        adminAuthorizationService.checkParticipantAccess(centreId);
    }

    public boolean canManageById(Long participantId) {
        Participant participant = participantRepository.findById(participantId).orElse(null);
        return participant == null || adminAuthorizationService.canAccessParticipant(participant);
    }

    public void assertCanManage(Participant participant) {
        adminAuthorizationService.checkParticipantAccess(participant);
    }

    public boolean canReadById(Long participantId) {
        Participant participant = participantRepository.findById(participantId).orElse(null);
        return participant == null || canRead(participant);
    }

    public boolean canReadByMatricule(String matricule) {
        Participant participant = participantRepository.findByMatriculeIgnoreCase(matricule).orElse(null);
        return participant == null || canRead(participant);
    }

    public List<Participant> filterParticipants(List<Participant> participants) {
        return adminAuthorizationService.filterParticipants(participants);
    }

    public void assertCanRead(Participant participant) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if (admin) {
            adminAuthorizationService.checkParticipantAccess(participant);
            return;
        }

        currentParticipantService.requireCurrentParticipant(participant.getId());
    }

    private boolean canRead(Participant participant) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if (admin) {
            return adminAuthorizationService.canAccessParticipant(participant);
        }

        return currentParticipantService.isCurrentParticipantId(participant.getId());
    }
}
