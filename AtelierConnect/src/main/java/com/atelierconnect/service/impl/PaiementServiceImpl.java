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
import com.atelierconnect.service.PaiementService;
import com.atelierconnect.service.AtelierRules;
import com.atelierconnect.service.ReservationService;
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
    private final ReservationService reservationService;
    private final ParticipantRepository membreRepository;
    private final AtelierRepository matchRepository;

    @Override
    @Transactional
    public Paiement pay(Long reservationId, Long membreId) {
        Reservation reservation = reservationService.getById(reservationId);
        Participant membre = reservation.getMembre();

        // Regle metier : seul le membre lié a la reservation peut payer.
        if (!membre.getId().equals(membreId)) {
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
        // La place est attribuee uniquement au moment du paiement, apres verification du match sous verrou.
        Atelier match = matchRepository.findByIdForUpdate(reservation.getMatch().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Atelier introuvable avec l'ID : " + reservation.getMatch().getId()));
        // Regle metier : le match doit toujours etre valide une fois verrouille.
        if (match.getStatut() == StatutAtelier.ANNULE) {
            throw new BusinessException("Cet atelier est annulé.");
        }
        if (match.getNbJoueursActuels() != null && match.getNbJoueursActuels() >= AtelierRules.CAPACITE) {
            throw new BusinessException("Cet atelier est déjà complet.");
        }
        double montantFinal = paiement.getMontant();
        if (membre.getSolde() > 0.0) {
            // Regle metier : un solde impaye est recupere au paiement suivant.
            montantFinal += membre.getSolde();
            log.info("Solde impayé de {} ajouté au paiement du membre {}",
                    membre.getSolde(), membreId);
            membre.setSolde(0.0);
            membreRepository.save(membre);
        }

        paiement.setMontant(montantFinal);
        paiement.setStatut(StatutPaiement.PAYE);
        paiement.setDatePaiement(LocalDateTime.now());
        paiementRepository.save(paiement);

        reservationService.confirm(reservationId);
        int nbJoueursActuels = match.getNbJoueursActuels() == null ? 0 : match.getNbJoueursActuels();
        nbJoueursActuels++;
        match.setNbJoueursActuels(nbJoueursActuels);
        if (nbJoueursActuels == AtelierRules.CAPACITE) {
            match.setStatut(StatutAtelier.COMPLET);
        }
        matchRepository.save(match);

        log.info("Paiement effectué pour la réservation {} par le participant {}", reservationId, membreId);

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
    public List<Paiement> getByMembreId(Long membreId) {
        return paiementRepository.findByReservationMembreId(membreId);
    }

    @Override
    public void checkPaiementOwner(Long paiementId, Long membreId) {
        Paiement paiement = getById(paiementId);
        // Regle metier : un membre ne peut consulter que ses propres paiements.
        if (!paiement.getReservation().getMembre().getId().equals(membreId)) {
            throw new BusinessException("Accès refusé : ce paiement appartient à un autre membre.");
        }
    }

    @Override
    public void checkReservationPaiementOwner(Long reservationId, Long membreId) {
        Paiement paiement = getByReservationId(reservationId);
        // Regle metier : un membre ne peut agir que sur le paiement de sa reservation.
        if (!paiement.getReservation().getMembre().getId().equals(membreId)) {
            throw new BusinessException("Accès refusé : ce paiement appartient à un autre membre.");
        }
    }

    @Override
    @Transactional
    public void checkUnpaidBeforeMatch() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // Regle metier : a J-1, les reservations non payees sont annulees et imputees a l'organisateur.
        paiementRepository.findByStatut(StatutPaiement.EN_ATTENTE)
                .stream()
                .filter(p -> p.getReservation().getMatch()
                        .getDateDebut().toLocalDate().equals(tomorrow))
                .forEach(p -> {
                    Reservation reservation = p.getReservation();
                    Atelier match = reservation.getMatch();

                    reservationService.cancel(reservation.getId());

                    double partManquante = match.getPrixParJoueur();
                    Participant organisateur = match.getOrganisateur();
                    organisateur.setSolde(organisateur.getSolde() + partManquante);
                    membreRepository.save(organisateur);

                    log.info("Réservation impayée {} annulée, solde {} ajouté à l'organisateur {}",
                            reservation.getId(), partManquante, organisateur.getId());
                });
    }
}
