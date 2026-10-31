package com.atelierconnect.service.impl;

import com.atelierconnect.entity.Atelier;
import com.atelierconnect.entity.Participant;
import com.atelierconnect.entity.Paiement;
import com.atelierconnect.entity.Reservation;
import com.atelierconnect.entity.enums.*;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.exception.ResourceNotFoundException;
import com.atelierconnect.repository.PaiementRepository;
import com.atelierconnect.repository.ReservationRepository;
import com.atelierconnect.service.AtelierService;
import com.atelierconnect.service.ParticipantService;
import com.atelierconnect.service.ReservationService;
import com.atelierconnect.service.AtelierRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final PaiementRepository paiementRepository;
    private final AtelierService matchService;
    private final ParticipantService membreService;

    @Override
    @Transactional
    public Reservation create(Long matchId, Long membreId, Long requesterId) {
        Atelier match = matchService.getMatchEntityById(matchId);
        Participant membre = membreService.getById(membreId);

        // Regle metier : impossible de reserver un match deja complet.
        if (match.getStatut() == StatutAtelier.COMPLET) {
            throw new BusinessException("Le match est déjà complet.");
        }
        // Regle metier : impossible de reserver un match annule.
        if (match.getStatut() == StatutAtelier.ANNULE) {
            throw new BusinessException("Le match est annulé.");
        }
        if (!match.getDateDebut().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Un atelier passé ne peut plus être réservé.");
        }
        long joursAvant = ChronoUnit.DAYS.between(LocalDate.now(), match.getDateDebut().toLocalDate());
        if (joursAvant > AtelierRules.delaiReservation(membre.getTypeMembre())) {
            throw new BusinessException("Ce participant ne peut pas réserver aussi tôt pour sa catégorie.");
        }

        // Regle metier : un membre ne peut pas etre inscrit deux fois au meme match.
        if (reservationRepository.existsByMatchIdAndMembreId(matchId, membreId)) {
            throw new BusinessException("Ce membre est déjà inscrit à ce match.");
        }

        // Regle metier : un membre penalise ne peut pas reserver.
        if (membreService.hasActivePenalty(membreId)) {
            throw new BusinessException("Vous avez une pénalité active et ne pouvez pas réserver ce match.");
        }

        // Regle metier : un membre avec un solde impaye ne peut pas reserver.
        if (membreService.hasOutstandingBalance(membreId)) {
            throw new BusinessException("Vous avez un solde impayé et ne pouvez pas réserver ce match.");
        }

        // Le requesterId est l'ID de la personne qui initie la demande de reservation.
        // Regle metier : seul l'organisateur peut inviter des joueurs sur un match prive.
        if (match.getTypeMatch() == TypeAtelier.PRIVE
                && !match.getOrganisateur().getId().equals(requesterId)) {
            throw new BusinessException("Pour un match privé, seul l'organisateur peut ajouter des joueurs.");
        }

        // Regle metier : un membre reserve un match public uniquement pour lui-meme.
        if (match.getTypeMatch() == TypeAtelier.PUBLIC && !membreId.equals(requesterId)) {
            throw new BusinessException("Un membre ne peut réserver un match public que pour lui-même.");
        }

        if (membre.getTypeMembre() == TypeParticipant.SITE) {
            Long membreSiteId = membre.getSite().getId();
            Long matchSiteId = match.getTerrain().getSite().getId();
            // Regle metier : un membre SITE ne reserve que sur son site.
            if (!membreSiteId.equals(matchSiteId)) {
                throw new BusinessException("Un membre SITE ne peut réserver que sur son propre site.");
            }
        }
        if (membre.getTypeMembre() == TypeParticipant.LIBRE
                && !Boolean.TRUE.equals(match.getTerrain().getSite().getOuvertVisiteurs())) {
            throw new BusinessException("Ce centre n'est pas ouvert aux visiteurs.");
        }

        Reservation reservation = Reservation.builder()
                .match(match)
                .membre(membre)
                .statut(StatutReservation.EN_ATTENTE)
                .build();

        reservation = reservationRepository.save(reservation);

        // Regle metier : chaque reservation cree automatiquement un paiement en attente.
        Paiement paiement = Paiement.builder()
                .reservation(reservation)
                .montant(match.getPrixParJoueur())
                .statut(StatutPaiement.EN_ATTENTE)
                .build();

        paiement = paiementRepository.save(paiement);
        reservation.setPaiement(paiement);

        log.info("Réservation créée pour le membre {} sur le match {}", membreId, matchId);

        return reservation;
    }

    @Override
    @Transactional
    public List<Reservation> createForInvites(Long matchId, Long requesterId, List<String> inviteeMatricules) {
        if (inviteeMatricules == null || inviteeMatricules.isEmpty()) {
            return List.of();
        }

        List<Reservation> createdReservations = new ArrayList<>();
        for (String matricule : inviteeMatricules) {
            String normalizedMatricule = matricule == null ? "" : matricule.trim();
            if (normalizedMatricule.isEmpty()) {
                continue;
            }

            Participant membre = membreService.getByMatricule(normalizedMatricule);
            createdReservations.add(create(matchId, membre.getId(), requesterId));
        }

        return createdReservations;
    }

    @Override
    public Reservation getById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable avec l'ID : " + id));
    }

    @Override
    public List<Reservation> getByMatchId(Long matchId) {
        return reservationRepository.findByMatchId(matchId);
    }

    @Override
    public List<Reservation> getByMembreId(Long membreId) {
        return reservationRepository.findByMembreId(membreId);
    }

    @Override
    public void checkReservationOwner(Long reservationId, Long membreId) {
        Reservation reservation = getById(reservationId);
        // Regle metier : un membre ne peut consulter que ses propres reservations.
        if (!reservation.getMembre().getId().equals(membreId)) {
            throw new BusinessException("Accès refusé : cette réservation appartient à un autre membre.");
        }
    }

    @Override
    @Transactional
    public void cancel(Long reservationId) {
        // Même ordre de verrouillage que le paiement : réservation, puis atelier.
        // Empêche l'annulation et la confirmation simultanées d'une même place.
        reservationRepository.lockReservationById(reservationId);
        Reservation reservation = getById(reservationId);
        boolean wasConfirmed = reservation.getStatut() == StatutReservation.CONFIRMEE;

        // Regle metier : impossible d'annuler deux fois la meme reservation.
        if (reservation.getStatut() == StatutReservation.ANNULEE) {
            throw new BusinessException("La réservation est déjà annulée.");
        }

        reservation.setStatut(StatutReservation.ANNULEE);
        reservationRepository.save(reservation);

        // Regle metier : annuler une reservation met aussi a jour son paiement.
        Paiement paiement = reservation.getPaiement();
        if (paiement != null) {
            if (paiement.getStatut() == StatutPaiement.PAYE) {
                paiement.setStatut(StatutPaiement.REMBOURSE);
            } else if (paiement.getStatut() == StatutPaiement.EN_ATTENTE) {
                paiement.setStatut(StatutPaiement.ANNULE);
            }
            paiementRepository.save(paiement);
        }

        // Regle metier : seule une reservation confirmee libere une place.
        if (wasConfirmed) {
            matchService.decrementPlayers(reservation.getMatch().getId());
        }

        log.info("Réservation {} annulée", reservationId);
    }

    @Override
    @Transactional
    public void confirm(Long reservationId) {
        Reservation reservation = getById(reservationId);
        // Regle metier : impossible de confirmer deux fois la meme reservation.
        if (reservation.getStatut() == StatutReservation.CONFIRMEE) {
            throw new BusinessException("La réservation est déjà confirmée.");
        }
        if (reservation.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new BusinessException("Seule une réservation en attente peut être confirmée.");
        }

        reservation.setStatut(StatutReservation.CONFIRMEE);
        reservationRepository.save(reservation);

        log.info("Réservation {} confirmée", reservationId);
    }
}
