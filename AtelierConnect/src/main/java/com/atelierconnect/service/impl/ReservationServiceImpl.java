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
    private final AtelierService atelierService;
    private final ParticipantService participantService;

    @Override
    @Transactional
    public Reservation create(Long atelierId, Long participantId, Long requesterId) {
        Atelier atelier = atelierService.getAtelierEntityById(atelierId);
        Participant participant = participantService.getById(participantId);

        // Regle metier : impossible de reserver un atelier deja complet.
        if (atelier.getStatut() == StatutAtelier.COMPLET) {
            throw new BusinessException("Cet atelier est déjà complet.");
        }
        // Regle metier : impossible de reserver un atelier annule.
        if (atelier.getStatut() == StatutAtelier.ANNULE) {
            throw new BusinessException("Cet atelier est annulé.");
        }
        if (!atelier.getDateDebut().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Un atelier passé ne peut plus être réservé.");
        }
        long joursAvant = ChronoUnit.DAYS.between(LocalDate.now(), atelier.getDateDebut().toLocalDate());
        if (joursAvant > AtelierRules.delaiReservation(participant.getTypeParticipant())) {
            throw new BusinessException("Ce participant ne peut pas réserver aussi tôt pour sa catégorie.");
        }

        // Regle metier : un participant ne peut pas etre inscrit deux fois au meme atelier.
        if (reservationRepository.existsByAtelierIdAndParticipantId(atelierId, participantId)) {
            throw new BusinessException("Ce participant est déjà inscrit à cet atelier.");
        }

        // Regle metier : un participant penalise ne peut pas reserver.
        if (participantService.hasActivePenalty(participantId)) {
            throw new BusinessException("Vous avez une suspension active et ne pouvez pas réserver cet atelier.");
        }

        // Regle metier : un participant avec un solde impaye ne peut pas reserver.
        if (participantService.hasOutstandingBalance(participantId)) {
            throw new BusinessException("Vous avez un solde impayé et ne pouvez pas réserver cet atelier.");
        }

        // Le requesterId est l'ID de la personne qui initie la demande de reservation.
        // Regle metier : seul l'organisateur peut inviter des participants sur un atelier prive.
        if (atelier.getTypeAtelier() == TypeAtelier.PRIVE
                && !atelier.getOrganisateur().getId().equals(requesterId)) {
            throw new BusinessException("Pour un atelier privé, seul l'organisateur peut inscrire des invités.");
        }

        // Regle metier : un participant reserve un atelier public uniquement pour lui-meme.
        if (atelier.getTypeAtelier() == TypeAtelier.PUBLIC && !participantId.equals(requesterId)) {
            throw new BusinessException("Un participant ne peut réserver un atelier public que pour lui-même.");
        }

        if (participant.getTypeParticipant() == TypeParticipant.ABONNE) {
            Long participantCentreId = participant.getCentre().getId();
            Long atelierCentreId = atelier.getSalle().getCentre().getId();
            // Regle metier : un participant SITE ne reserve que sur son centre.
            if (!participantCentreId.equals(atelierCentreId)) {
                throw new BusinessException("Un abonné ne peut réserver que dans son centre de rattachement.");
            }
        }
        if (participant.getTypeParticipant() == TypeParticipant.VISITEUR
                && !Boolean.TRUE.equals(atelier.getSalle().getCentre().getOuvertVisiteurs())) {
            throw new BusinessException("Ce centre n'est pas ouvert aux visiteurs.");
        }

        Reservation reservation = Reservation.builder()
                .atelier(atelier)
                .participant(participant)
                .statut(StatutReservation.EN_ATTENTE)
                .build();

        reservation = reservationRepository.save(reservation);

        // Regle metier : chaque reservation cree automatiquement un paiement en attente.
        Paiement paiement = Paiement.builder()
                .reservation(reservation)
                .montant(atelier.getPrixParParticipant())
                .statut(StatutPaiement.EN_ATTENTE)
                .build();

        paiement = paiementRepository.save(paiement);
        reservation.setPaiement(paiement);

        log.info("Réservation créée pour le participant {} à l'atelier {}", participantId, atelierId);

        return reservation;
    }

    @Override
    @Transactional
    public List<Reservation> createForInvites(Long atelierId, Long requesterId, List<String> inviteeMatricules) {
        if (inviteeMatricules == null || inviteeMatricules.isEmpty()) {
            return List.of();
        }

        List<Reservation> createdReservations = new ArrayList<>();
        for (String matricule : inviteeMatricules) {
            String normalizedMatricule = matricule == null ? "" : matricule.trim();
            if (normalizedMatricule.isEmpty()) {
                continue;
            }

            Participant participant = participantService.getByMatricule(normalizedMatricule);
            createdReservations.add(create(atelierId, participant.getId(), requesterId));
        }

        return createdReservations;
    }

    @Override
    public Reservation getById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable avec l'ID : " + id));
    }

    @Override
    public List<Reservation> getByAtelierId(Long atelierId) {
        return reservationRepository.findByAtelierId(atelierId);
    }

    @Override
    public List<Reservation> getByParticipantId(Long participantId) {
        return reservationRepository.findByParticipantId(participantId);
    }

    @Override
    public void checkReservationOwner(Long reservationId, Long participantId) {
        Reservation reservation = getById(reservationId);
        // Regle metier : un participant ne peut consulter que ses propres reservations.
        if (!reservation.getParticipant().getId().equals(participantId)) {
            throw new BusinessException("Accès refusé : cette réservation appartient à un autre participant.");
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
            atelierService.decrementParticipants(reservation.getAtelier().getId());
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
