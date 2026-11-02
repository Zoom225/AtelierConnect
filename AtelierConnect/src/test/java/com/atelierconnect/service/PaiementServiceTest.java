package com.atelierconnect.service;

import com.atelierconnect.entity.*;
import com.atelierconnect.entity.enums.*;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.exception.ResourceNotFoundException;
import com.atelierconnect.repository.AtelierRepository;
import com.atelierconnect.repository.ParticipantRepository;
import com.atelierconnect.repository.PaiementRepository;
import com.atelierconnect.repository.ReservationRepository;
import com.atelierconnect.service.impl.PaiementServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de PaiementService")
class PaiementServiceTest {

    @Mock
    private PaiementRepository paiementRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationService reservationService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private AtelierRepository atelierRepository;

    @InjectMocks
    private PaiementServiceImpl paiementService;

    private Centre centre;
    private Salle salle;
    private Participant organisateur;
    private Participant participant;
    private Atelier atelier;
    private Reservation reservation;
    private Paiement paiement;

    @BeforeEach
    void setUp() {
        centre = Centre.builder().nom("Centre collaboratif Lyon").build();
        salle = Salle.builder().nom("Court A").centre(centre).build();

        organisateur = Participant.builder()
                .matricule("G1001").nom("Martin").prenom("Lucas")
                .typeParticipant(TypeParticipant.PARTENAIRE).solde(0.0).build();
        organisateur.setId(1L);

        participant = Participant.builder()
                .matricule("G1002").nom("Dupont").prenom("Julie")
                .typeParticipant(TypeParticipant.PARTENAIRE).solde(0.0).build();
        participant.setId(2L);

        // Correction : Utiliser uniquement dateDebut et dateFin
        LocalDateTime atelierStart = LocalDate.now().plusDays(25).atTime(15, 0);
        atelier = Atelier.builder()
                .salle(salle)
                .organisateur(organisateur)
                .dateDebut(atelierStart)
                .dateFin(atelierStart.plusMinutes(90))
                .typeAtelier(TypeAtelier.PUBLIC)
                .statut(StatutAtelier.PLANIFIE)
                .nbParticipantsActuels(1)
                .prixTotal(60.0)
                .prixParParticipant(15.0)
                .build();
        atelier.setId(10L);

        reservation = Reservation.builder()
                .atelier(atelier).participant(participant).statut(StatutReservation.EN_ATTENTE).build();
        reservation.setId(1L);

        paiement = Paiement.builder()
                .reservation(reservation).montant(15.0).statut(StatutPaiement.EN_ATTENTE).build();
        paiement.setId(1L);

        reservation.setPaiement(paiement);
    }

    @Nested
    @DisplayName("pay()")
    class PayTests {
        @Test
        @DisplayName("verrouille la réservation avant toute lecture du paiement")
        void shouldLockReservationBeforeReadingItsPayment() {
            when(reservationService.getById(1L)).thenReturn(reservation);
            when(atelierRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(atelier));

            paiementService.pay(1L, 2L);

            var order = inOrder(reservationRepository, reservationService, atelierRepository);
            order.verify(reservationRepository).lockReservationById(1L);
            order.verify(reservationService).getById(1L);
            order.verify(atelierRepository).findByIdForUpdate(10L);
        }

        @Test
        @DisplayName("✅ doit traiter le paiement et confirmer la réservation")
        void shouldProcessPayment() {
            when(reservationService.getById(1L)).thenReturn(reservation);
            when(atelierRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(atelier));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(atelierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doAnswer(inv -> {
                reservation.setStatut(StatutReservation.CONFIRMEE);
                return null;
            }).when(reservationService).confirm(1L);

            Paiement result = paiementService.pay(1L, 2L);

            assertThat(result.getStatut()).isEqualTo(StatutPaiement.PAYE);
            assertThat(reservation.getStatut()).isEqualTo(StatutReservation.CONFIRMEE);
            assertThat(atelier.getNbParticipantsActuels()).isEqualTo(2);
            assertThat(atelier.getStatut()).isEqualTo(StatutAtelier.PLANIFIE);
            verify(atelierRepository).findByIdForUpdate(10L);
            verify(reservationService).confirm(1L);
            verify(atelierRepository).save(atelier);
        }

        @Test
        @DisplayName("✅ doit ajouter le solde impayé et l'effacer")
        void shouldAddOutstandingBalanceToPayment() {
            participant.setSolde(15.0);
            when(reservationService.getById(1L)).thenReturn(reservation);
            when(atelierRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(atelier));
            when(participantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Paiement result = paiementService.pay(1L, 2L);

            assertThat(result.getMontant()).isEqualTo(30.0);
            assertThat(participant.getSolde()).isEqualTo(0.0);
            verify(participantRepository).save(participant);
        }

        @Test
        @DisplayName("❌ doit lever une exception quand déjà payé")
        void shouldThrowWhenAlreadyPaid() {
            paiement.setStatut(StatutPaiement.PAYE);
            when(reservationService.getById(1L)).thenReturn(reservation);

            assertThatThrownBy(() -> paiementService.pay(1L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("déjà été effectué");
        }

        @Test
        @DisplayName("✅ doit faire passer l'atelier à COMPLET au quatrième paiement")
        void shouldCompleteAtelierOnFourthPayment() {
            atelier.setNbParticipantsActuels(5);
            when(reservationService.getById(1L)).thenReturn(reservation);
            when(atelierRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(atelier));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(atelierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doAnswer(inv -> {
                reservation.setStatut(StatutReservation.CONFIRMEE);
                return null;
            }).when(reservationService).confirm(1L);

            Paiement result = paiementService.pay(1L, 2L);

            assertThat(result.getStatut()).isEqualTo(StatutPaiement.PAYE);
            assertThat(reservation.getStatut()).isEqualTo(StatutReservation.CONFIRMEE);
            assertThat(atelier.getNbParticipantsActuels()).isEqualTo(6);
            assertThat(atelier.getStatut()).isEqualTo(StatutAtelier.COMPLET);
            verify(reservationService).confirm(1L);
            verify(atelierRepository).findByIdForUpdate(10L);
            verify(atelierRepository).save(atelier);
        }

        @Test
        @DisplayName("❌ doit refuser le paiement quand l'atelier est déjà complet")
        void shouldThrowWhenAtelierIsAlreadyFull() {
            atelier.setNbParticipantsActuels(6);
            atelier.setStatut(StatutAtelier.COMPLET);
            when(reservationService.getById(1L)).thenReturn(reservation);
            when(atelierRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(atelier));

            assertThatThrownBy(() -> paiementService.pay(1L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("déjà complet");

            assertThat(atelier.getNbParticipantsActuels()).isEqualTo(6);
            assertThat(reservation.getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
            assertThat(paiement.getStatut()).isEqualTo(StatutPaiement.EN_ATTENTE);
            verify(atelierRepository).findByIdForUpdate(10L);
            verify(paiementRepository, never()).save(any());
            verify(reservationService, never()).confirm(any());
            verify(atelierRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ doit refuser le paiement quand la réservation est annulée")
        void shouldThrowWhenReservationIsCancelled() {
            reservation.setStatut(StatutReservation.ANNULEE);
            when(reservationService.getById(1L)).thenReturn(reservation);

            assertThatThrownBy(() -> paiementService.pay(1L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("réservation annulée");

            verify(atelierRepository, never()).findByIdForUpdate(any());
            verify(paiementRepository, never()).save(any());
            verify(reservationService, never()).confirm(any());
        }
    }

    @Nested
    @DisplayName("getById() and getByReservationId()")
    class GetTests {
        @Test
        @DisplayName("✅ doit retourner le paiement quand l'id existe")
        void shouldReturnPaymentById() {
            when(paiementRepository.findById(1L)).thenReturn(Optional.of(paiement));
            Paiement result = paiementService.getById(1L);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("❌ doit lever une exception quand l'id du paiement n'est pas trouvé")
        void shouldThrowWhenPaymentNotFound() {
            when(paiementRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> paiementService.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("checkUnpaidBeforeMatch()")
    class SchedulerTests {
        @Test
        @DisplayName("✅ doit annuler la réservation impayée et ajouter le solde")
        void shouldCancelUnpaidReservationAndAddBalanceToOrganizer() {
            // Correction : Utiliser setDateDebut pour modifier la date de l'atelier
            LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();
            atelier.setDateDebut(tomorrowStart);

            when(paiementRepository.findByStatut(StatutPaiement.EN_ATTENTE)).thenReturn(List.of(paiement));
            when(participantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            paiementService.checkUnpaidBeforeAtelier();

            verify(reservationService).cancel(reservation.getId());
            assertThat(organisateur.getSolde()).isEqualTo(15.0);
            verify(participantRepository).save(organisateur);
        }

        @Test
        @DisplayName("ne pénalise pas un paiement confirmé juste avant le verrou")
        void shouldSkipPenaltyWhenPaymentWasJustConfirmed() {
            atelier.setDateDebut(LocalDate.now().plusDays(1).atStartOfDay());
            when(paiementRepository.findByStatut(StatutPaiement.EN_ATTENTE))
                    .thenReturn(List.of(paiement));
            doAnswer(invocation -> {
                paiement.setStatut(StatutPaiement.PAYE);
                reservation.setStatut(StatutReservation.CONFIRMEE);
                return null;
            }).when(entityManager).refresh(paiement);

            paiementService.checkUnpaidBeforeAtelier();

            verify(reservationRepository).lockReservationById(reservation.getId());
            verify(reservationService, never()).cancel(any());
            verify(participantRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ NE doit PAS annuler quand l'atelier n'est pas demain")
        void shouldNotCancelWhenAtelierIsNotTomorrow() {
            // Correction : S'assurer que la date de l'atelier n'est pas demain
            atelier.setDateDebut(LocalDate.now().plusDays(5).atStartOfDay());

            when(paiementRepository.findByStatut(StatutPaiement.EN_ATTENTE)).thenReturn(List.of(paiement));

            paiementService.checkUnpaidBeforeAtelier();

            verify(reservationService, never()).cancel(any());
            verify(participantRepository, never()).save(any());
        }
    }
}
