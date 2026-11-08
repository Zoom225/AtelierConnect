package com.atelierconnect.service.impl;

import com.atelierconnect.entity.*;
import com.atelierconnect.entity.enums.*;
import com.atelierconnect.repository.*;
import com.atelierconnect.service.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UnpaidReservationProcessor {
    private final PaiementRepository paiementRepository;
    private final ReservationRepository reservationRepository;
    private final AtelierRepository atelierRepository;
    private final ReservationService reservationService;
    private final ParticipantService participantService;
    private final DetteService detteService;
    private final EntityManager entityManager;

    @Transactional
    public void process(Long paiementId) {
        var candidate = paiementRepository.findById(paiementId);
        if (candidate.isEmpty()) return;
        Paiement paiement = candidate.get();
        Reservation reservation = paiement.getReservation();
        reservationRepository.lockReservationById(reservation.getId());
        entityManager.refresh(paiement);
        entityManager.refresh(reservation);
        if (paiement.getStatut() != StatutPaiement.EN_ATTENTE || reservation.getStatut() != StatutReservation.EN_ATTENTE) return;
        Atelier atelier = atelierRepository.findByIdForUpdate(reservation.getAtelier().getId()).orElseThrow();
        entityManager.refresh(atelier);
        if (atelier.getStatut() == StatutAtelier.ANNULE
                || !atelier.getDateDebut().toLocalDate().equals(LocalDate.now().plusDays(1))) return;
        if (atelier.getTypeAtelier() == TypeAtelier.PRIVE) {
            // Le privé est imputé globalement : aucune seconde facturation par invitation.
            participantService.penalizeIncompletePrivateAtelier(atelier.getId());
        } else {
            detteService.chargeUnpaidPublicReservation(atelier, reservation.getId());
        }
        reservationService.cancel(reservation.getId());
    }
}
