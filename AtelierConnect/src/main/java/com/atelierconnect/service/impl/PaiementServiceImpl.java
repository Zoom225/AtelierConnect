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
import com.atelierconnect.repository.ParticipantRepository;
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
    private final ParticipantRepository participantRepository;
    private final AtelierRepository atelierRepository;

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

        // Regle metier : une reservation annulee ne peut pas etre payee.
        if (reservation.getStatut() == com.atelierconnect.entity.enums.StatutReservation.ANNULEE) {
            throw new BusinessException("Impossible de payer une réservation annulée.");
        }

        Paiement paiement = reservation.getPaiement();
        if (paiement == null) {
            throw new ResourceNotFoundException("Aucun paiement trouvé pour la réservation : " + reservationId);
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
        // La place est attribuee uniquement au moment du paiement, apres verification de l'atelier sous verrou.
        Atelier atelier = atelierRepository.findByIdForUpdate(reservation.getAtelier().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Atelier introuvable avec l'ID : " + reservation.getAtelier().getId()));
        // Rafraîchir sous verrou : l'atelier peut avoir été chargé en mémoire
        // AVANT le SELECT FOR UPDATE d'une transaction concurrente.
        entityManager.refresh(atelier);
        // Regle metier : l'atelier doit toujours etre valide une fois verrouille.
        if (atelier.getStatut() == StatutAtelier.ANNULE) {
            throw new BusinessException("Cet atelier est annulé.");
        }
        if (atelier.getNbParticipantsActuels() != null && atelier.getNbParticipantsActuels() >= AtelierRules.CAPACITE) {
            throw new BusinessException("Cet atelier est déjà complet.");
        }
        double montantFinal = paiement.getMontant();
        if (participant.getSolde() > 0.0) {
            // Regle metier : un solde impaye est recupere au paiement suivant.
            montantFinal += participant.getSolde();
            log.info("Solde impayé de {} ajouté au paiement du participant {}",
                    participant.getSolde(), participantId);
            participant.setSolde(0.0);
            participantRepository.save(participant);
        }

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
    public void checkPaiementOwner(Long paiementId, Long participantId) {
        Paiement paiement = getById(paiementId);
        // Regle metier : un participant ne peut consulter que ses propres paiements.
        if (!paiement.getReservation().getParticipant().getId().equals(participantId)) {
            throw new BusinessException("Accès refusé : ce paiement appartient à un autre participant.");
        }
    }

    @Override
    public void checkReservationPaiementOwner(Long reservationId, Long participantId) {
        Paiement paiement = getByReservationId(reservationId);
        // Regle metier : un participant ne peut agir que sur le paiement de sa reservation.
        if (!paiement.getReservation().getParticipant().getId().equals(participantId)) {
            throw new BusinessException("Accès refusé : ce paiement appartient à un autre participant.");
        }
    }

    @Override
    @Transactional
    public void checkUnpaidBeforeAtelier() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // Regle metier : a J-1, les reservations non payees sont annulees et imputees a l'organisateur.
        paiementRepository.findByStatut(StatutPaiement.EN_ATTENTE)
                .stream()
                .filter(p -> p.getReservation().getAtelier()
                        .getDateDebut().toLocalDate().equals(tomorrow))
                .forEach(p -> {
                    Long reservationId = p.getReservation().getId();
                    // Le paiement peut avoir eu lieu pendant l'attente du verrou :
                    // recharger l'état persistant avant de décider une sanction.
                    reservationRepository.lockReservationById(reservationId);
                    entityManager.refresh(p);
                    Reservation reservation = p.getReservation();
                    entityManager.refresh(reservation);
                    if (p.getStatut() != StatutPaiement.EN_ATTENTE
                            || reservation.getStatut() != StatutReservation.EN_ATTENTE) {
                        return;
                    }

                    Atelier atelier = reservation.getAtelier();
                    reservationService.cancel(reservationId);

                    double partManquante = atelier.getPrixParParticipant();
                    Participant organisateur = atelier.getOrganisateur();
                    organisateur.setSolde(organisateur.getSolde() + partManquante);
                    participantRepository.save(organisateur);

                    log.info("Réservation impayée {} annulée, solde {} ajouté à l'organisateur {}",
                            reservation.getId(), partManquante, organisateur.getId());
                });
    }
}
