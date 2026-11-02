package com.atelierconnect.service;

import com.atelierconnect.entity.Participant;
import com.atelierconnect.service.impl.AdminAuthorizationService;
import com.atelierconnect.service.impl.CurrentParticipantService;
import com.atelierconnect.service.impl.ParticipantAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de MembreAccessService")
class ParticipantAccessServiceTest {

    @Mock
    private AdminAuthorizationService adminAuthorizationService;

    @Mock
    private CurrentParticipantService currentParticipantService;

    @InjectMocks
    private ParticipantAccessService participantAccessService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Un administrateur délègue l'accès lecture au service admin")
    void adminReadAccessDelegatesToAdminAuthorization() {
        authenticate("admin@padel.com", "ROLE_ADMIN");
        Participant participant = new Participant();
        participant.setId(12L);

        participantAccessService.assertCanRead(participant);

        verify(adminAuthorizationService).checkParticipantAccess(participant);
        verifyNoInteractions(currentParticipantService);
    }

    @Test
    @DisplayName("Un participant délègue l'accès lecture à son propre contrôle")
    void memberReadAccessDelegatesToCurrentParticipantCheck() {
        authenticate("S10001", "ROLE_MEMBER");
        Participant participant = new Participant();
        participant.setId(7L);

        participantAccessService.assertCanRead(participant);

        verify(currentParticipantService).requireCurrentParticipant(7L);
        verifyNoInteractions(adminAuthorizationService);
    }

    @Test
    @DisplayName("La création d'un participant passe par la vérification admin")
    void createAccessDelegatesToAdminAuthorization() {
        authenticate("admin@padel.com", "ROLE_ADMIN");

        participantAccessService.assertCanCreate(3L);

        verify(adminAuthorizationService).checkParticipantAccess(3L);
        verifyNoInteractions(currentParticipantService);
    }

    @Test
    @DisplayName("La liste des participants est filtrée par le service admin")
    void filterParticipantsDelegatesToAdminAuthorization() {
        authenticate("admin@padel.com", "ROLE_ADMIN");
        Participant participant = new Participant();
        participant.setId(1L);
        List<Participant> participants = List.of(participant);

        participantAccessService.filterParticipants(participants);

        verify(adminAuthorizationService).filterParticipants(participants);
        verifyNoInteractions(currentParticipantService);
    }

    private void authenticate(String name, String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        name,
                        null,
                        List.of(new SimpleGrantedAuthority(authority))
                )
        );
    }
}
