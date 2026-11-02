package com.atelierconnect.service;

import com.atelierconnect.entity.Participant;
import com.atelierconnect.entity.Centre;
import com.atelierconnect.entity.enums.TypeParticipant;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.exception.ResourceNotFoundException;
import com.atelierconnect.repository.ParticipantRepository;
import com.atelierconnect.repository.PenaliteRepository;
import com.atelierconnect.service.impl.ParticipantServiceImpl;
import com.atelierconnect.service.CentreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder; // Added import
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de MembreService")
class ParticipantServiceTest {

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private PenaliteRepository penaliteRepository;

    @Mock
    private CentreService centreService;

    @Mock // Added PasswordEncoder mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ParticipantServiceImpl participantService;

    private Participant participantGlobal;
    private Participant participantCentre;
    private Participant participantLibre;
    private Centre centre;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(participantService, "defaultParticipantPassword", "Membre1234!");
        centre = Centre.builder()
                .nom("Centre collaboratif Lyon")
                .build();
        centre.setId(1L); // pas de setter via Builder car id vient de BaseEntity

        participantGlobal = Participant.builder()
                .matricule("G1001")
                .nom("Martin")
                .prenom("Lucas")
                .email("lucas@email.com")
                .typeParticipant(TypeParticipant.PARTENAIRE)
                .solde(0.0)
                .passwordHash("$2a$10$somehashedpassword") // Added a password hash for tests
                .build();

        participantCentre = Participant.builder()
                .matricule("S10001")
                .nom("Bernard")
                .prenom("Tom")
                .email("tom@email.com")
                .typeParticipant(TypeParticipant.ABONNE)
                .solde(0.0)
                .centre(centre)
                .passwordHash("$2a$10$somehashedpassword") // Added a password hash for tests
                .build();

        participantLibre = Participant.builder()
                .matricule("L10001")
                .nom("Petit")
                .prenom("Alex")
                .email("alex@email.com")
                .typeParticipant(TypeParticipant.VISITEUR)
                .solde(0.0)
                .passwordHash("$2a$10$somehashedpassword") // Added a password hash for tests
                .build();
    }

    // ================================================================
    // CREATE
    // ================================================================
    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("✅ doit créer un participant GLOBAL avec un matricule valide G1001")
        void shouldCreateGlobalParticipant() {
            when(participantRepository.existsByMatricule("G1001")).thenReturn(false);
            when(participantRepository.existsByEmail(anyString())).thenReturn(false);
            when(participantRepository.save(any())).thenReturn(participantGlobal);

            Participant result = participantService.create(participantGlobal);

            assertThat(result).isNotNull();
            assertThat(result.getMatricule()).isEqualTo("G1001");
            assertThat(result.getTypeParticipant()).isEqualTo(TypeParticipant.PARTENAIRE);
            assertThat(result.getSolde()).isEqualTo(0.0);
            verify(participantRepository, times(1)).save(participantGlobal);
        }

        @Test
        @DisplayName("âœ… doit normaliser le matricule en majuscules lors de la création")
        void shouldNormalizeMatriculeOnCreate() {
            Participant participant = Participant.builder()
                    .matricule("g1001")
                    .nom("Martin")
                    .prenom("Lucas")
                    .email("lucas@email.com")
                    .typeParticipant(TypeParticipant.PARTENAIRE)
                    .solde(0.0)
                    .passwordHash("$2a$10$somehashedpassword")
                    .build();

            when(participantRepository.existsByMatricule("G1001")).thenReturn(false);
            when(participantRepository.existsByEmail(anyString())).thenReturn(false);
            when(participantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Participant result = participantService.create(participant);

            assertThat(result.getMatricule()).isEqualTo("G1001");
            verify(participantRepository).existsByMatricule("G1001");
            verify(participantRepository).save(argThat(saved -> "G1001".equals(saved.getMatricule())));
        }

        @Test
        @DisplayName("✅ doit créer un participant SITE lié à un centre")
        void shouldCreateCentreParticipant() {
            when(participantRepository.existsByMatricule("S10001")).thenReturn(false);
            when(participantRepository.existsByEmail(anyString())).thenReturn(false);
            when(participantRepository.save(any())).thenReturn(participantCentre);

            Participant result = participantService.create(participantCentre);

            assertThat(result.getTypeParticipant()).isEqualTo(TypeParticipant.ABONNE);
            assertThat(result.getCentre()).isNotNull();
            assertThat(result.getCentre().getNom()).isEqualTo("Centre collaboratif Lyon");
        }

        @Test
        @DisplayName("✅ doit créer un participant LIBRE avec un matricule valide L10001")
        void shouldCreateLibreParticipant() {
            when(participantRepository.existsByMatricule("L10001")).thenReturn(false);
            when(participantRepository.existsByEmail(anyString())).thenReturn(false);
            when(participantRepository.save(any())).thenReturn(participantLibre);

            Participant result = participantService.create(participantLibre);

            assertThat(result.getTypeParticipant()).isEqualTo(TypeParticipant.VISITEUR);
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand le matricule existe déjà")
        void shouldThrowWhenMatriculeExists() {
            when(participantRepository.existsByMatricule("G1001")).thenReturn(true);

            assertThatThrownBy(() -> participantService.create(participantGlobal))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("matricule existe déjà");

            verify(participantRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand l'email existe déjà")
        void shouldThrowWhenEmailExists() {
            when(participantRepository.existsByMatricule("G1001")).thenReturn(false);
            when(participantRepository.existsByEmail("lucas@email.com")).thenReturn(true);

            assertThatThrownBy(() -> participantService.create(participantGlobal))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("email existe déjà");

            verify(participantRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand un participant SITE n'a pas de centre")
        void shouldThrowWhenCentreParticipantHasNoCentre() {
            Participant participantSansCentre = Participant.builder()
                    .matricule("S10002")
                    .nom("Test")
                    .prenom("Test")
                    .typeParticipant(TypeParticipant.ABONNE)
                    .solde(0.0)
                    .centre(null) // ← pas de centre
                    .build();

            when(participantRepository.existsByMatricule("S10002")).thenReturn(false);

            assertThatThrownBy(() -> participantService.create(participantSansCentre))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("participant SITE doit être lié");
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand le format du matricule GLOBAL est incorrect")
        void shouldThrowWhenGlobalMatriculeFormatIsWrong() {
            Participant badParticipant = Participant.builder()
                    .matricule("G12") // ← trop court, doit être G + 4 chiffres
                    .typeParticipant(TypeParticipant.PARTENAIRE)
                    .solde(0.0)
                    .build();

            assertThatThrownBy(() -> participantService.create(badParticipant))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Format de matricule invalide");
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand le format du matricule SITE est incorrect")
        void shouldThrowWhenCentreMatriculeFormatIsWrong() {
            Participant badParticipant = Participant.builder()
                    .matricule("S123") // ← doit être S + 5 chiffres
                    .typeParticipant(TypeParticipant.ABONNE)
                    .solde(0.0)
                    .centre(centre)
                    .build();

            assertThatThrownBy(() -> participantService.create(badParticipant))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Format de matricule invalide");
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand le matricule LIBRE commence par la mauvaise lettre")
        void shouldThrowWhenLibreMatriculeStartsWithWrongLetter() {
            Participant badParticipant = Participant.builder()
                    .matricule("X10001") // ← doit commencer par L
                    .typeParticipant(TypeParticipant.VISITEUR)
                    .solde(0.0)
                    .build();

            assertThatThrownBy(() -> participantService.create(badParticipant))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Format de matricule invalide");
        }

        @Test
        @DisplayName("✅ doit initialiser le solde à 0.0 lors de la création")
        void shouldInitializeSoldeToZero() {
            participantGlobal.setSolde(99.0); // on force un solde non nul
            when(participantRepository.existsByMatricule(any())).thenReturn(false);
            when(participantRepository.existsByEmail(any())).thenReturn(false);
            when(participantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Participant result = participantService.create(participantGlobal);

            assertThat(result.getSolde()).isEqualTo(0.0);
        }
    }

    // ================================================================
    // GET
    // ================================================================
    @Nested
    @DisplayName("getById() and getByMatricule()")
    class GetTests {

        @Test
        @DisplayName("✅ doit retourner le participant quand l'id existe")
        void shouldReturnParticipantById() {
            when(participantRepository.findById(1L)).thenReturn(Optional.of(participantGlobal));

            Participant result = participantService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getMatricule()).isEqualTo("G1001");
        }

        @Test
        @DisplayName("❌ doit lever une ResourceNotFoundException quand l'id n'est pas trouvé")
        void shouldThrowWhenIdNotFound() {
            when(participantRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> participantService.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Participant introuvable");
        }

        @Test
        @DisplayName("✅ doit retourner le participant quand le matricule existe")
        void shouldReturnParticipantByMatricule() {
            when(participantRepository.findByMatriculeIgnoreCase("G1001")).thenReturn(Optional.of(participantGlobal));

            Participant result = participantService.getByMatricule("G1001");

            assertThat(result.getMatricule()).isEqualTo("G1001");
        }

        @Test
        @DisplayName("âœ… doit normaliser le matricule avant la recherche")
        void shouldNormalizeMatriculeBeforeLookup() {
            when(participantRepository.findByMatriculeIgnoreCase("G1001")).thenReturn(Optional.of(participantGlobal));

            Participant result = participantService.getByMatricule("g1001");

            assertThat(result.getMatricule()).isEqualTo("G1001");
            verify(participantRepository).findByMatriculeIgnoreCase("G1001");
        }

        @Test
        @DisplayName("❌ doit lever une ResourceNotFoundException quand le matricule n'est pas trouvé")
        void shouldThrowWhenMatriculeNotFound() {
            when(participantRepository.findByMatriculeIgnoreCase("G9999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> participantService.getByMatricule("G9999"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("G9999");
        }
    }

    // ================================================================
    // AUTHENTICATE
    // ================================================================
    @Nested
    @DisplayName("authenticate()")
    class AuthenticateTests {

        @Test
        @DisplayName("✅ doit retourner le participant quand les identifiants sont valides")
        void shouldAuthenticateWithValidCredentials() {
            when(participantRepository.findByMatriculeIgnoreCase("G1001")).thenReturn(Optional.of(participantGlobal));
            when(passwordEncoder.matches("Membre1234!", participantGlobal.getPasswordHash())).thenReturn(true);

            Participant result = participantService.authenticate("G1001", "Membre1234!");

            assertThat(result).isEqualTo(participantGlobal);
        }

        @Test
        @DisplayName("❌ doit refuser quand le mot de passe est invalide")
        void shouldRejectInvalidPassword() {
            when(participantRepository.findByMatriculeIgnoreCase("G1001")).thenReturn(Optional.of(participantGlobal));
            when(passwordEncoder.matches("wrong-password", participantGlobal.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> participantService.authenticate("G1001", "wrong-password"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Identifiants invalides.");
        }

        @Test
        @DisplayName("❌ doit refuser quand le matricule est introuvable")
        void shouldRejectInvalidMatricule() {
            when(participantRepository.findByMatriculeIgnoreCase("G9999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> participantService.authenticate("G9999", "Membre1234!"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Identifiants invalides.");
        }

        @Test
        @DisplayName("❌ doit refuser quand le participant n'a pas de hash de mot de passe")
        void shouldRejectParticipantWithoutPasswordHash() {
            participantGlobal.setPasswordHash(null); // Simulate old account without hash
            when(participantRepository.findByMatriculeIgnoreCase("G1001")).thenReturn(Optional.of(participantGlobal));

            assertThatThrownBy(() -> participantService.authenticate("G1001", "Membre1234!"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Identifiants invalides.");
        }

        @Test
        @DisplayName("✅ doit normaliser le matricule en majuscules avant la recherche")
        void shouldNormalizeMatriculeToUpperCase() {
            when(participantRepository.findByMatriculeIgnoreCase("G1001")).thenReturn(Optional.of(participantGlobal));
            when(passwordEncoder.matches("Membre1234!", participantGlobal.getPasswordHash())).thenReturn(true);

            participantService.authenticate("g1001", "Membre1234!");

            verify(participantRepository).findByMatriculeIgnoreCase("G1001");
        }
    }

    // ================================================================
    // PENALTY
    // ================================================================
    @Nested
    @DisplayName("hasActivePenalty()")
    class PenaltyTests {

        @Test
        @DisplayName("✅ doit retourner vrai quand le participant a une pénalité active")
        void shouldReturnTrueWhenActivePenalty() {
            when(penaliteRepository.existsByParticipantIdAndDateFinAfter(eq(1L), any(LocalDate.class)))
                    .thenReturn(true);

            boolean result = participantService.hasActivePenalty(1L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("✅ doit retourner faux quand le participant n'a pas de pénalité active")
        void shouldReturnFalseWhenNoPenalty() {
            when(penaliteRepository.existsByParticipantIdAndDateFinAfter(eq(1L), any(LocalDate.class)))
                    .thenReturn(false);

            boolean result = participantService.hasActivePenalty(1L);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("✅ doit ajouter une pénalité avec dateFin = aujourd'hui + 7 jours")
        void shouldAddPenaltyWithCorrectDateFin() {
            when(participantRepository.findById(1L)).thenReturn(Optional.of(participantGlobal));
            when(penaliteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            participantService.addPenalty(1L);

            verify(penaliteRepository, times(1)).save(argThat(penalite ->
                    penalite.getDateFin().equals(LocalDate.now().plusWeeks(1)) &&
                            penalite.getMotif().equals("Atelier privé incomplet avant l'échéance")
            ));
        }
    }

    // ================================================================
    // OUTSTANDING BALANCE
    // ================================================================
    @Nested
    @DisplayName("hasOutstandingBalance()")
    class BalanceTests {

        @Test
        @DisplayName("✅ doit retourner vrai quand le participant a un solde impayé")
        void shouldReturnTrueWhenBalancePositive() {
            participantGlobal.setSolde(15.0);
            when(participantRepository.findById(1L)).thenReturn(Optional.of(participantGlobal));

            boolean result = participantService.hasOutstandingBalance(1L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("✅ doit retourner faux quand le participant n'a pas de solde impayé")
        void shouldReturnFalseWhenBalanceZero() {
            participantGlobal.setSolde(0.0);
            when(participantRepository.findById(1L)).thenReturn(Optional.of(participantGlobal));

            boolean result = participantService.hasOutstandingBalance(1L);

            assertThat(result).isFalse();
        }
    }

    // ================================================================
    // UPDATE
    // ================================================================
    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("✅ doit mettre à jour le nom et l'email du participant")
        void shouldUpdateParticipant() {
            when(participantRepository.findById(1L)).thenReturn(Optional.of(participantGlobal));
            when(participantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Participant updated = Participant.builder()
                    .nom("Nouveau")
                    .prenom("Nom")
                    .email("nouveau@email.com")
                    .build();

            Participant result = participantService.update(1L, updated);

            assertThat(result.getNom()).isEqualTo("Nouveau");
            assertThat(result.getPrenom()).isEqualTo("Nom");
            assertThat(result.getEmail()).isEqualTo("nouveau@email.com");
        }

        @Test
        @DisplayName("❌ doit lever une ResourceNotFoundException lors de la mise à jour d'un participant inexistant")
        void shouldThrowWhenUpdatingNonExistentParticipant() {
            when(participantRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> participantService.update(99L, participantGlobal))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ================================================================
    // DELETE
    // ================================================================
    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("✅ doit supprimer le participant quand il existe")
        void shouldDeleteParticipant() {
            when(participantRepository.findById(1L)).thenReturn(Optional.of(participantGlobal));

            participantService.delete(1L);

            verify(participantRepository, times(1)).delete(participantGlobal);
        }

        @Test
        @DisplayName("❌ doit lever une ResourceNotFoundException lors de la suppression d'un participant inexistant")
        void shouldThrowWhenDeletingNonExistentParticipant() {
            when(participantRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> participantService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(participantRepository, never()).delete(any());
        }
    }
}
