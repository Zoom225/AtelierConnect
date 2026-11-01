package com.atelierconnect.service;

import com.atelierconnect.entity.Administrateur;
import com.atelierconnect.entity.Participant;
import com.atelierconnect.entity.Centre;
import com.atelierconnect.entity.Salle;
import com.atelierconnect.entity.enums.TypeAdministrateur;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.repository.AdministrateurRepository;
import com.atelierconnect.repository.AtelierRepository;
import com.atelierconnect.service.impl.AdminAuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de AdminAuthorizationService")
class AdminAuthorizationServiceTest {

    @Mock
    private AdministrateurRepository administrateurRepository;
    @Mock
    private AtelierRepository atelierRepository;

    private AdminAuthorizationService adminAuthorizationService;
    private Centre centreLyon;
    private Centre centreParis;

    @BeforeEach
    void setUp() {
        adminAuthorizationService = new AdminAuthorizationService(administrateurRepository, atelierRepository);
        centreLyon = Centre.builder().nom("Centre collaboratif Lyon").build();
        centreLyon.setId(1L);
        centreParis = Centre.builder().nom("Centre collaboratif Paris").build();
        centreParis.setId(2L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Un admin GLOBAL peut accéder à tous les centres")
    void globalAdminCanAccessEveryCentre() {
        String adminEmail = "admin@padel.com";
        authenticate(adminEmail);
        when(administrateurRepository.findByEmail(adminEmail))
                .thenReturn(Optional.of(admin(adminEmail, TypeAdministrateur.GLOBAL, null)));

        adminAuthorizationService.checkCentreAccess(centreParis.getId());
    }

    @Test
    @DisplayName("Un admin SITE peut accéder uniquement à son centre")
    void centreAdminCanAccessOwnCentreOnly() {
        String adminEmail = "admin.lyon@padel.com";
        authenticate(adminEmail);
        when(administrateurRepository.findByEmail(adminEmail))
                .thenReturn(Optional.of(admin(adminEmail, TypeAdministrateur.SITE, centreLyon)));

        adminAuthorizationService.checkCentreAccess(centreLyon.getId());
        assertThatThrownBy(() -> adminAuthorizationService.checkCentreAccess(centreParis.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("son propre centre");
    }

    @Test
    @DisplayName("Un admin SITE sans centre rattaché est refusé")
    void centreAdminWithoutCentreIsRejected() {
        String adminEmail = "admin.site@padel.com";
        authenticate(adminEmail);
        when(administrateurRepository.findByEmail(adminEmail))
                .thenReturn(Optional.of(admin(adminEmail, TypeAdministrateur.SITE, null)));

        assertThatThrownBy(() -> adminAuthorizationService.checkCentreAccess(centreLyon.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("son propre centre");
    }

    @Test
    @DisplayName("Un admin SITE ne peut gérer que les participants de son centre")
    void centreAdminCanManageOnlyOwnParticipants() {
        String adminEmail = "admin.lyon@padel.com";
        authenticate(adminEmail);
        // La ligne suivante est la ligne 94 dans le fichier de test si le reste est identique.
        // Elle doit stuber avec 'adminEmail' qui est "admin.lyon@padel.com"
        when(administrateurRepository.findByEmail(adminEmail))
                .thenReturn(Optional.of(admin(adminEmail, TypeAdministrateur.SITE, centreLyon)));

        Participant participantParis = Participant.builder().centre(centreParis).build();

        assertThatThrownBy(() -> adminAuthorizationService.checkParticipantAccess(participantParis))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("participants de son centre");
    }

    @Test
    @DisplayName("Un admin SITE ne peut gérer que les salles de son centre")
    void centreAdminCanManageOnlyOwnSalles() {
        String adminEmail = "admin.lyon@padel.com";
        authenticate(adminEmail);
        when(administrateurRepository.findByEmail(adminEmail))
                .thenReturn(Optional.of(admin(adminEmail, TypeAdministrateur.SITE, centreLyon)));

        Salle salleParis = Salle.builder().centre(centreParis).build();

        assertThatThrownBy(() -> adminAuthorizationService.checkSalleAccess(salleParis))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("son propre centre");
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null)
        );
    }

    // Méthode admin modifiée pour accepter l'email
    private Administrateur admin(String email, TypeAdministrateur type, Centre centre) {
        return Administrateur.builder()
                .email(email) // Utilise l'email passé en paramètre
                .typeAdministrateur(type)
                .centre(centre)
                .build();
    }
}