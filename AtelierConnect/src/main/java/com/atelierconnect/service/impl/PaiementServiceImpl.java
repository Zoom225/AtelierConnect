package com.atelierconnect.service.impl;

import com.atelierconnect.entity.Participant;
import com.atelierconnect.entity.Atelier;
import com.atelierconnect.entity.Paiement;
import com.atelierconnect.entity.Reservation;
import com.atelierconnect.entity.enums.StatutAtelier;
import com.atelierconnect.entity.enums.StatutPaiement;
import com.atelierconnect.entity.enums.StatutReservation;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.exception.ResourceNotFoundException;
import com.atelierconnect.repository.AtelierRepository;
import com.atelierconnect.repository.PaiementRepository;
import com.atelierconnect.repository.ReservationRepository;
import com.atelierconnect.service.PaiementService;
import com.atelierconnect.service.AtelierRules;
import com.atelierconnect.service.ReservationService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaiementServiceImpl implements PaiementService {

    private final PaiementRepository paiementRepository;
    private final ReservationRepository reservationRepository;
    private final EntityManager entityManager;
    private final ReservationService reservationService;
    private final AtelierRepository atelierRepository;
    private final DetteService detteService;
    private final UnpaidReservationProcessor unpaidProcessor;

    @Override
    @Transactional
    public Paiement pay(Long reservationId, Long participantId) {
        // Toujours verrouiller avant de lire les statuts : deux requêtes simultanées
        // sur la même réservation ne doivent jamais confirmer deux paiements.
        reservationRepository.lockReservationById(reservationId);
        Reservation reservation = reservationService.getById(reservationId);
        Participant participant = reservation.getParticipant();

        // Regle metier : seul le participant lié a la reservation peut payer.
        if (!participant.getId().equals(participantId)) {
            throw new BusinessException("Seul le participant associé à cette réservation peut payer.");
        }

        Paiement paiement = getPendingPayment(reservation);
        Atelier atelier = getAvailableAtelierForUpdate(reservation.getAtelier().getId());
        double montantFinal = paiement.getMontant()
                + detteService.settleWithReservation(participantId, reservationId).doubleValue();

        paiement.setMontant(montantFinal);
        paiement.setStatut(StatutPaiement.PAYE);
        paiement.setDatePaiement(LocalDateTime.now());
        paiementRepository.save(paiement);

        reservationService.confirm(reservationId);
        int nbParticipantsActuels = atelier.getNbParticipantsActuels() == null ? 0 : atelier.getNbParticipantsActuels();
        nbParticipantsActuels++;
        atelier.setNbParticipantsActuels(nbParticipantsActuels);
        if (nbParticipantsActuels == AtelierRules.CAPACITE) {
            atelier.setStatut(StatutAtelier.COMPLET);
        }
        atelierRepository.save(atelier);

        log.info("Paiement effectué pour la réservation {} par le participant {}", reservationId, participantId);
        return paiement;
    }

    private Paiement getPendingPayment(Reservation reservation) {
        if (reservation.getStatut() == StatutReservation.ANNULEE) {
            throw new BusinessException("Impossible de payer une réservation annulée.");
        }

        Paiement paiement = reservation.getPaiement();
        if (paiement == null) {
            throw new ResourceNotFoundException("Aucun paiement trouvé pour la réservation : " + reservation.getId());
        }

        // Regle metier : un paiement deja effectue ne peut pas etre refait.
        if (paiement.getStatut() == StatutPaiement.PAYE) {
            throw new BusinessException("Le paiement de cette réservation a déjà été effectué.");
        }

        // Regle metier : un paiement annule ne peut pas etre relance.
        if (paiement.getStatut() == StatutPaiement.ANNULE) {
            throw new BusinessException("Le paiement de cette réservation a été annulé.");
        }
        // Regle metier : seules les reservations en attente peuvent etre payees.
        if (reservation.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new BusinessException("Seules les réservations en attente peuvent être payées.");
        }
        return paiement;
    }

    private Atelier getAvailableAtelierForUpdate(Long atelierId) {
        Atelier atelier = atelierRepository.findByIdForUpdate(atelierId)
                .orElseThrow(() -> new ResourceNotFoundException("Atelier introuvable avec l'ID : " + atelierId));
        // Rafraîchir sous verrou : l'atelier peut avoir été chargé en mémoire
        // AVANT le SELECT FOR UPDATE d'une transaction concurrente.
        entityManager.refresh(atelier);
        if (detteService.isPrivateAtelierCharged(atelier.getId())) {
            throw new BusinessException("L'échéance de cet atelier privé est dépassée : ses places impayées sont déjà imputées à l'organisateur.");
        }
        // Regle metier : l'atelier doit toujours etre valide une fois verrouille.
        if (atelier.getStatut() == StatutAtelier.ANNULE) {
            throw new BusinessException("Cet atelier est annulé.");
        }
        if (atelier.getNbParticipantsActuels() != null && atelier.getNbParticipantsActuels() >= AtelierRules.CAPACITE) {
            throw new BusinessException("Cet atelier est déjà complet.");
        }
        return atelier;
    }

    @Override
    public Paiement getById(Long id) {
        return paiementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable avec l'ID : " + id));
    }

    @Override
    public Paiement getByReservationId(Long reservationId) {
        return paiementRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable pour la réservation : " + reservationId));
    }

    @Override
    public List<Paiement> getByParticipantId(Long participantId) {
        return paiementRepository.findByReservationParticipantId(participantId);
    }

    @Override
    @Transactional(readOnly = true)
    public void checkPaiementOwner(Long paiementId, Long participantId) {
        checkOwner(getById(paiementId), participantId);
    }

    @Override
    @Transactional(readOnly = true)
    public void checkReservationPaiementOwner(Long reservationId, Long participantId) {
        checkOwner(getByReservationId(reservationId), participantId);
    }

    private void checkOwner(Paiement paiement, Long participantId) {
        if (!paiement.getReservation().getParticipant().getId().equals(participantId)) {
            throw new BusinessException("Accès refusé : ce paiement appartient à un autre participant.");
        }
    }

    @Override
    public void checkUnpaidBeforeAtelier() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // Regle metier : a J-1, les reservations non payees sont annulees et imputees a l'organisateur.
        paiementRepository.findUnpaidIdsForDay(tomorrow.atStartOfDay(), tomorrow.plusDays(1).atStartOfDay())
                .forEach(unpaidProcessor::process);
    }
}
