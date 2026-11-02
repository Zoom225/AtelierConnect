package com.atelierconnect.service;

import com.atelierconnect.entity.*;
import com.atelierconnect.entity.enums.*;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.exception.ResourceNotFoundException;
import com.atelierconnect.repository.PaiementRepository;
import com.atelierconnect.repository.ReservationRepository;
import com.atelierconnect.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de ReservationService")
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PaiementRepository paiementRepository;

    @Mock
    private AtelierService atelierService;

    @Mock
    private ParticipantService participantService;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Centre centre;
    private Centre centreB;
    private Salle salle;
    private Participant organisateur;
    private Participant participant;
    private Participant participantCentreB;
    private Atelier atelierPrive;
    private Atelier atelierPublic;
    private Atelier atelierComplet;

    @BeforeEach
    void setUp() {
        centreB = Centre.builder()
                .nom("Centre collaboratif Paris")
                .build();
        centreB.setId(2L);

        centre = Centre.builder()
                .nom("Centre collaboratif Lyon")
                .build();
        centre.setId(1L);

        salle = Salle.builder()
                .nom("Court A")
                .centre(centre)
                .build();
        salle.setId(1L);

        organisateur = Participant.builder()
                .matricule("G1001")
                .nom("Martin")
                .prenom("Lucas")
                .typeParticipant(TypeParticipant.PARTENAIRE)
                .solde(0.0)
                .centre(null)
                .build();
        organisateur.setId(1L);

        participant = Participant.builder()
                .matricule("G1002")
                .nom("Dupont")
                .prenom("Julie")
                .typeParticipant(TypeParticipant.PARTENAIRE)
                .solde(0.0)
                .centre(null)
                .build();
        participant.setId(2L);

        // participant rattaché à un autre centre → ne peut pas réserver sur siteLyon
        participantCentreB = Participant.builder()
                .matricule("S20001")
                .nom("Leclerc")
                .prenom("Paul")
                .typeParticipant(TypeParticipant.ABONNE)
                .solde(0.0)
                .centre(centreB)
                .build();
        participantCentreB.setId(3L);

        atelierPrive = Atelier.builder()
                .salle(salle)
                .organisateur(organisateur)
                .dateDebut(java.time.LocalDateTime.of(LocalDate.now().plusDays(25), LocalTime.of(15, 0)))
                .dateFin(java.time.LocalDateTime.of(LocalDate.now().plusDays(25), LocalTime.of(16, 30)))
                .typeAtelier(TypeAtelier.PRIVE)
                .statut(StatutAtelier.PLANIFIE)
                .nbParticipantsActuels(1)
                .prixTotal(60.0)
                .prixParParticipant(15.0)
                .build();
        atelierPrive.setId(10L);

        atelierPublic = Atelier.builder()
                .salle(salle)
                .organisateur(organisateur)
                .dateDebut(java.time.LocalDateTime.of(LocalDate.now().plusDays(25), LocalTime.of(17, 0)))
                .dateFin(java.time.LocalDateTime.of(LocalDate.now().plusDays(25), LocalTime.of(18, 30)))
                .typeAtelier(TypeAtelier.PUBLIC)
                .statut(StatutAtelier.PLANIFIE)
                .nbParticipantsActuels(1)
                .prixTotal(60.0)
                .prixParParticipant(15.0)
                .build();
        atelierPublic.setId(11L);

        atelierComplet = Atelier.builder()
                .salle(salle)
                .organisateur(organisateur)
                .dateDebut(java.time.LocalDateTime.of(LocalDate.now().plusDays(25), LocalTime.of(19, 0)))
                .dateFin(java.time.LocalDateTime.of(LocalDate.now().plusDays(25), LocalTime.of(20, 30)))
                .typeAtelier(TypeAtelier.PUBLIC)
                .statut(StatutAtelier.COMPLET)
                .nbParticipantsActuels(4)
                .prixTotal(60.0)
                .prixParParticipant(15.0)
                .build();
        atelierComplet.setId(12L);
    }

    // ================================================================
    // CREATE
    // ================================================================
    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("✅ doit créer une réservation pour un atelier PUBLIC avec un participant valide")
        void shouldCreateReservationForPublicAtelier() {
            when(atelierService.getAtelierEntityByIdForUpdate(11L)).thenReturn(atelierPublic); // MODIFIÉ
            when(participantService.getById(2L)).thenReturn(participant);
            when(participantService.hasActivePenalty(2L)).thenReturn(false);
            when(participantService.hasOutstandingBalance(2L)).thenReturn(false);
            when(reservationRepository.existsByAtelierIdAndParticipantId(11L, 2L)).thenReturn(false);
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Reservation result = reservationService.create(11L, 2L, 2L);

            assertThat(result).isNotNull();
            assertThat(result.getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
            assertThat(result.getParticipant()).isEqualTo(participant);
            assertThat(result.getAtelier()).isEqualTo(atelierPublic);

            // vérifier que le paiement EN_ATTENTE est créé automatiquement
            verify(paiementRepository, times(1)).save(argThat(paiement ->
                    paiement.getMontant().equals(15.0) &&
                            paiement.getStatut() == StatutPaiement.EN_ATTENTE
            ));
            verify(atelierService, never()).incrementParticipants(any());
        }

        @Test
        @DisplayName("✅ doit créer une réservation pour un atelier PRIVE quand l'organisateur ajoute un participant")
        void shouldCreateReservationForPriveAtelierByOrganizer() {
            when(atelierService.getAtelierEntityByIdForUpdate(10L)).thenReturn(atelierPrive); // MODIFIÉ
            when(participantService.getById(2L)).thenReturn(participant);
            when(participantService.hasActivePenalty(2L)).thenReturn(false);
            when(participantService.hasOutstandingBalance(2L)).thenReturn(false);
            when(reservationRepository.existsByAtelierIdAndParticipantId(10L, 2L)).thenReturn(false);
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // l'organisateur (id=1) ajoute participant (id=2) → autorisé
            Reservation result = reservationService.create(10L, 2L, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
            verify(atelierService, never()).incrementParticipants(any());
        }

        @Test
        void refusesSixthPrivateGuestEvenBeforePayment() {
            when(atelierService.getAtelierEntityByIdForUpdate(10L)).thenReturn(atelierPrive);
            when(participantService.getById(2L)).thenReturn(participant);
            when(reservationRepository.countByAtelierIdAndStatutNot(10L, StatutReservation.ANNULEE))
                    .thenReturn(6L);

            assertThatThrownBy(() -> reservationService.create(10L, 2L, 1L))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("cinq invités");
            verify(reservationRepository, never()).save(any());
            verify(paiementRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ doit créer des réservations à partir de matricules invités")
        void shouldCreateReservationsForInvites() {
            when(atelierService.getAtelierEntityByIdForUpdate(10L)).thenReturn(atelierPrive);
            when(participantService.getByMatricule("g1002")).thenReturn(participant);
            when(participantService.getById(2L)).thenReturn(participant);
            when(participantService.hasActivePenalty(2L)).thenReturn(false);
            when(participantService.hasOutstandingBalance(2L)).thenReturn(false);
            when(reservationRepository.existsByAtelierIdAndParticipantId(10L, 2L)).thenReturn(false);
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = reservationService.createForInvites(10L, 1L, java.util.List.of("g1002"));

            assertThat(result.size()).isEqualTo(1);
            verify(participantService).getByMatricule("g1002");
            verify(atelierService, never()).incrementParticipants(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand l'atelier est COMPLET")
        void shouldThrowWhenAtelierIsFull() {
            when(atelierService.getAtelierEntityByIdForUpdate(12L)).thenReturn(atelierComplet); // MODIFIÉ
            when(participantService.getById(2L)).thenReturn(participant);

            assertThatThrownBy(() -> reservationService.create(12L, 2L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("déjà complet");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand l'atelier est ANNULE")
        void shouldThrowWhenAtelierIsCancelled() {
            atelierPublic.setStatut(StatutAtelier.ANNULE);
            when(atelierService.getAtelierEntityByIdForUpdate(11L)).thenReturn(atelierPublic); // MODIFIÉ
            when(participantService.getById(2L)).thenReturn(participant);

            assertThatThrownBy(() -> reservationService.create(11L, 2L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("annulé");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand le participant est déjà inscrit au atelier")
        void shouldThrowWhenParticipantAlreadyRegistered() {
            when(atelierService.getAtelierEntityByIdForUpdate(11L)).thenReturn(atelierPublic); // MODIFIÉ
            when(participantService.getById(2L)).thenReturn(participant);
            when(reservationRepository.existsByAtelierIdAndParticipantId(11L, 2L)).thenReturn(true);

            assertThatThrownBy(() -> reservationService.create(11L, 2L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("déjà inscrit");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand le participant a une pénalité active")
        void shouldThrowWhenParticipantHasActivePenalty() {
            when(atelierService.getAtelierEntityByIdForUpdate(11L)).thenReturn(atelierPublic); // MODIFIÉ
            when(participantService.getById(2L)).thenReturn(participant);
            when(reservationRepository.existsByAtelierIdAndParticipantId(11L, 2L)).thenReturn(false);
            when(participantService.hasActivePenalty(2L)).thenReturn(true);

            assertThatThrownBy(() -> reservationService.create(11L, 2L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("suspension active");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand le participant a un solde impayé")
        void shouldThrowWhenParticipantHasOutstandingBalance() {
            when(atelierService.getAtelierEntityByIdForUpdate(11L)).thenReturn(atelierPublic); // MODIFIÉ
            when(participantService.getById(2L)).thenReturn(participant);
            when(reservationRepository.existsByAtelierIdAndParticipantId(11L, 2L)).thenReturn(false);
            when(participantService.hasActivePenalty(2L)).thenReturn(false);
            when(participantService.hasOutstandingBalance(2L)).thenReturn(true);

            assertThatThrownBy(() -> reservationService.create(11L, 2L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("solde impayé");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand un non-organisateur tente de rejoindre un atelier PRIVE")
        void shouldThrowWhenNonOrganizerJoinsPriveAtelier() {
            // participant (id=2) essaie de rejoindre directement un atelier privé
            // sans passer par l'organisateur (id=1)
            when(atelierService.getAtelierEntityByIdForUpdate(10L)).thenReturn(atelierPrive); // MODIFIÉ
            when(participantService.getById(2L)).thenReturn(participant);
            when(reservationRepository.existsByAtelierIdAndParticipantId(10L, 2L)).thenReturn(false);
            when(participantService.hasActivePenalty(2L)).thenReturn(false);
            when(participantService.hasOutstandingBalance(2L)).thenReturn(false);

            assertThatThrownBy(() -> reservationService.create(10L, 2L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("seul l'organisateur");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand un participant SITE réserve sur le mauvais centre")
        void shouldThrowWhenCentreParticipantBooksOnWrongCentre() {
            atelierPublic.setDateDebut(LocalDate.now().plusDays(10).atTime(15, 0));
            // joueurSiteB est rattaché à Paris → ne peut pas réserver sur Lyon
            when(atelierService.getAtelierEntityByIdForUpdate(11L)).thenReturn(atelierPublic); // MODIFIÉ
            when(participantService.getById(3L)).thenReturn(participantCentreB);
            when(reservationRepository.existsByAtelierIdAndParticipantId(11L, 3L)).thenReturn(false);
            when(participantService.hasActivePenalty(3L)).thenReturn(false);
            when(participantService.hasOutstandingBalance(3L)).thenReturn(false);

            assertThatThrownBy(() -> reservationService.create(11L, 3L, 3L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("centre de rattachement");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        void visiteurRefuseSiCentreFermeAuxVisiteurs() {
            participant.setTypeParticipant(TypeParticipant.VISITEUR);
            atelierPublic.setDateDebut(LocalDate.now().plusDays(3).atTime(15, 0));
            when(atelierService.getAtelierEntityByIdForUpdate(11L)).thenReturn(atelierPublic);
            when(participantService.getById(2L)).thenReturn(participant);

            assertThatThrownBy(() -> reservationService.create(11L, 2L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ouvert aux visiteurs");
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ paiement créé avec le montant correct = prixParJoueur de l'atelier")
        void shouldCreatePaiementWithCorrectAmount() {
            when(atelierService.getAtelierEntityByIdForUpdate(11L)).thenReturn(atelierPublic); // MODIFIÉ
            when(participantService.getById(2L)).thenReturn(participant);
            when(reservationRepository.existsByAtelierIdAndParticipantId(11L, 2L)).thenReturn(false);
            when(participantService.hasActivePenalty(2L)).thenReturn(false);
            when(participantService.hasOutstandingBalance(2L)).thenReturn(false);
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reservationService.create(11L, 2L, 2L);

            // prixParJoueur = 60€ / 4 = 15€
            verify(paiementRepository).save(argThat(p ->
                    p.getMontant().equals(15.0) &&
                            p.getStatut() == StatutPaiement.EN_ATTENTE
            ));
            verify(atelierService, never()).incrementParticipants(any());
        }
    }

    // ================================================================
    // CANCEL
    // ================================================================
    @Nested
    @DisplayName("cancel()")
    class CancelTests {

        @Test
        @DisplayName("✅ doit annuler la réservation en attente sans décrémenter le nombre de participants")
        void shouldCancelReservation() {
            Paiement paiement = Paiement.builder()
                    .montant(15.0)
                    .statut(StatutPaiement.EN_ATTENTE)
                    .build();

            Reservation reservation = Reservation.builder()
                    .atelier(atelierPublic)
                    .participant(participant)
                    .statut(StatutReservation.EN_ATTENTE)
                    .paiement(paiement)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reservationService.cancel(1L);

            assertThat(reservation.getStatut()).isEqualTo(StatutReservation.ANNULEE);
            verify(atelierService, never()).decrementParticipants(any());
        }

        @Test
        @DisplayName("✅ doit rembourser le paiement quand la réservation est annulée après paiement")
        void shouldRefundPaymentWhenCancelledAfterPayment() {
            Paiement paiement = Paiement.builder()
                    .montant(15.0)
                    .statut(StatutPaiement.PAYE) // déjà payé
                    .build();

            Reservation reservation = Reservation.builder()
                    .atelier(atelierPublic)
                    .participant(participant)
                    .statut(StatutReservation.CONFIRMEE)
                    .paiement(paiement)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reservationService.cancel(1L);

            // paiement doit passer à REMBOURSE
            assertThat(paiement.getStatut()).isEqualTo(StatutPaiement.REMBOURSE);
            verify(paiementRepository, times(1)).save(paiement);
            verify(atelierService, times(1)).decrementParticipants(atelierPublic.getId());
        }

        @Test
        @DisplayName("✅ doit annuler le paiement quand la réservation est annulée avant paiement")
        void shouldCancelPaymentWhenNotYetPaid() {
            Paiement paiement = Paiement.builder()
                    .montant(15.0)
                    .statut(StatutPaiement.EN_ATTENTE) // pas encore payé
                    .build();

            Reservation reservation = Reservation.builder()
                    .atelier(atelierPublic)
                    .participant(participant)
                    .statut(StatutReservation.EN_ATTENTE)
                    .paiement(paiement)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reservationService.cancel(1L);

            // paiement passe à ANNULE — réservation annulée avant paiement
            assertThat(paiement.getStatut()).isEqualTo(StatutPaiement.ANNULE);
            verify(paiementRepository, times(1)).save(paiement);
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand la réservation est déjà annulée")
        void shouldThrowWhenAlreadyCancelled() {
            Reservation reservation = Reservation.builder()
                    .atelier(atelierPublic)
                    .participant(participant)
                    .statut(StatutReservation.ANNULEE)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.cancel(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("déjà annulée");

            verify(atelierService, never()).decrementParticipants(any());
        }

        @Test
        @DisplayName("❌ doit lever une ResourceNotFoundException quand la réservation n'est pas trouvée")
        void shouldThrowWhenReservationNotFound() {
            when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.cancel(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Réservation introuvable");
        }
    }

    // ================================================================
    // CONFIRM
    // ================================================================
    @Nested
    @DisplayName("confirm()")
    class ConfirmTests {

        @Test
        void refusesConfirmationWithoutValidatedPayment() {
            Reservation reservation = Reservation.builder().statut(StatutReservation.EN_ATTENTE).build();
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            assertThatThrownBy(() -> reservationService.confirm(1L))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("paiement validé");
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ doit confirmer la réservation sans incrémenter à nouveau les participants de l'atelier")
        void shouldConfirmReservation() {
            Reservation reservation = Reservation.builder()
                    .atelier(atelierPublic)
                    .participant(participant)
                    .statut(StatutReservation.EN_ATTENTE)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reservation.setPaiement(Paiement.builder().statut(StatutPaiement.PAYE).build());
            reservationService.confirm(1L);

            assertThat(reservation.getStatut()).isEqualTo(StatutReservation.CONFIRMEE);
            verify(atelierService, never()).incrementParticipants(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand la réservation est déjà confirmée")
        void shouldThrowWhenAlreadyConfirmed() {
            Reservation reservation = Reservation.builder()
                    .atelier(atelierPublic)
                    .participant(participant)
                    .statut(StatutReservation.CONFIRMEE)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.confirm(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("déjà confirmée");

            verify(atelierService, never()).incrementParticipants(any());
        }
    }

    // ================================================================
    // GET
    // ================================================================
    @Nested
    @DisplayName("getById()")
    class GetTests {

        @Test
        @DisplayName("✅ doit retourner la réservation quand l'id existe")
        void shouldReturnReservationById() {
            Reservation reservation = Reservation.builder()
                    .atelier(atelierPublic)
                    .participant(participant)
                    .statut(StatutReservation.EN_ATTENTE)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

            Reservation result = reservationService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
        }

        @Test
        @DisplayName("❌ doit lever une ResourceNotFoundException quand l'id n'est pas trouvé")
        void shouldThrowWhenReservationNotFound() {
            when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Réservation introuvable");
        }
    }
}
