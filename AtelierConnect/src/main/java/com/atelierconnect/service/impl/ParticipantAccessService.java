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
    private final CurrentParticipantService currentMemberService;
    private final ParticipantRepository membreRepository;

    public boolean canCreate(Long siteId) {
        return adminAuthorizationService.canAccessMembreSite(siteId);
    }

    public void assertCanCreate(Long siteId) {
        adminAuthorizationService.checkMembreAccess(siteId);
    }

    public boolean canManageById(Long membreId) {
        Participant membre = membreRepository.findById(membreId).orElse(null);
        return membre == null || adminAuthorizationService.canAccessMembre(membre);
    }

    public void assertCanManage(Participant membre) {
        adminAuthorizationService.checkMembreAccess(membre);
    }

    public boolean canReadById(Long membreId) {
        Participant membre = membreRepository.findById(membreId).orElse(null);
        return membre == null || canRead(membre);
    }

    public boolean canReadByMatricule(String matricule) {
        Participant membre = membreRepository.findByMatriculeIgnoreCase(matricule).orElse(null);
        return membre == null || canRead(membre);
    }

    public List<Participant> filterMembres(List<Participant> membres) {
        return adminAuthorizationService.filterMembres(membres);
    }

    public void assertCanRead(Participant membre) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if (admin) {
            adminAuthorizationService.checkMembreAccess(membre);
            return;
        }

        currentMemberService.requireCurrentMember(membre.getId());
    }

    private boolean canRead(Participant membre) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if (admin) {
            return adminAuthorizationService.canAccessMembre(membre);
        }

        return currentMemberService.isCurrentMemberId(membre.getId());
    }
}
