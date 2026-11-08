package com.atelierconnect.service.impl;

import com.atelierconnect.dto.request.ReglerDetteRequest;
import com.atelierconnect.dto.response.DetteResponse;
import com.atelierconnect.entity.*;
import com.atelierconnect.entity.MouvementDette.Nature;
import com.atelierconnect.exception.*;
import com.atelierconnect.repository.*;
import com.atelierconnect.service.AtelierRules;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.*;
import java.time.*;

@Service
@RequiredArgsConstructor
public class DetteService {
    private final ParticipantRepository participants;
    private final MouvementDetteRepository mouvements;
    private final PenaliteRepository penalites;
    private final EntityManager entityManager;

    private static BigDecimal money(double value) {
        if (!Double.isFinite(value) || value < 0) throw new BusinessException("Le solde comptable est invalide.");
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private Participant lockedParticipant(Long id) {
        if (participants.lockParticipantById(id) == null)
            throw new ResourceNotFoundException("Participant introuvable.");
        Participant participant = participants.findById(id).orElseThrow();
        entityManager.refresh(participant);
        return participant;
    }

    @Transactional(readOnly = true)
    public DetteResponse get(Long id) {
        Participant participant = participants.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Participant introuvable."));
        return response(participant);
    }

    private DetteResponse response(Participant participant) {
        return new DetteResponse(money(participant.getSolde()),
                penalites.existsByParticipantIdAndDateFinGreaterThanEqual(participant.getId(), LocalDate.now()),
                mouvements.findTop50ByParticipantIdOrderByIdDesc(participant.getId()).stream().map(m ->
                        new DetteResponse.Mouvement(m.getId(), m.getNature(), m.getMontant(), m.getDescription(),
                                m.getAtelierId(), m.getDateOperation())).toList());
    }

    @Transactional
    public DetteResponse pay(Long id, ReglerDetteRequest request) {
        Participant participant = lockedParticipant(id);
        String reference = "reglement:" + id + ":" + request.cleRequete();
        var previous = mouvements.findByReferenceSource(reference);
        if (previous.isPresent()) {
            if (previous.get().getMontant().compareTo(request.montantAttendu()) != 0)
                throw new BusinessException("Cette demande de règlement a déjà été utilisée avec un autre montant.");
            return response(participant);
        }
        BigDecimal due = money(participant.getSolde());
        if (due.signum() == 0) throw new BusinessException("Vous n'avez aucune dette à régler.");
        if (due.compareTo(request.montantAttendu()) != 0)
            throw new BusinessException("Votre solde a changé. Actualisez le montant avant de confirmer le règlement.");
        record(participant, null, Nature.REGLEMENT_DETTE, due, reference, "Règlement du solde impayé");
        participant.setSolde(0.0);
        participants.save(participant);
        // Le règlement n'efface aucune suspension, même lorsque le solde atteint zéro.
        return response(participant);
    }

    @Transactional(readOnly = true)
    public boolean isPrivateAtelierCharged(Long atelierId) {
        return mouvements.existsByReferenceSource("atelier-prive:" + atelierId);
    }

    /** L'appelant détient le verrou de l'atelier, puis on verrouille le compte. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void chargePrivateAtelier(Atelier atelier, long paidSeats) {
        String reference = "atelier-prive:" + atelier.getId();
        Participant owner = lockedParticipant(atelier.getOrganisateur().getId());
        if (mouvements.existsByReferenceSource(reference)) return;
        BigDecimal paid = money(atelier.getPrixParParticipant()).multiply(BigDecimal.valueOf(paidSeats));
        BigDecimal missing = money(atelier.getPrixTotal()).subtract(paid).max(BigDecimal.ZERO);
        record(owner, atelier.getId(), Nature.IMPUTATION_PRIVE, missing, reference,
                "Atelier privé #" + atelier.getId() + " : " + (AtelierRules.CAPACITE - paidSeats) + " place(s) impayée(s)");
        owner.setSolde(money(owner.getSolde()).add(missing).doubleValue());
        participants.save(owner);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void chargeUnpaidPublicReservation(Atelier atelier, Long reservationId) {
        Participant owner = lockedParticipant(atelier.getOrganisateur().getId());
        String reference = "reservation-impayee:" + reservationId;
        if (mouvements.existsByReferenceSource(reference)) return;
        BigDecimal amount = money(atelier.getPrixParParticipant());
        record(owner, atelier.getId(), Nature.IMPUTATION_IMPAYE, amount, reference,
                "Réservation impayée #" + reservationId + " de l'atelier #" + atelier.getId());
        owner.setSolde(money(owner.getSolde()).add(amount).doubleValue());
        participants.save(owner);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public BigDecimal settleWithReservation(Long id, Long reservationId) {
        Participant participant = lockedParticipant(id);
        BigDecimal amount = money(participant.getSolde());
        if (amount.signum() == 0) return amount;
        String reference = "reglement-reservation:" + reservationId;
        if (mouvements.existsByReferenceSource(reference))
            throw new BusinessException("Le règlement de cette réservation a déjà été enregistré.");
        record(participant, null, Nature.REGLEMENT_RESERVATION, amount, reference,
                "Dette réglée avec la réservation #" + reservationId);
        participant.setSolde(0.0);
        participants.save(participant);
        return amount;
    }

    private void record(Participant participant, Long atelierId, Nature nature, BigDecimal amount,
                        String reference, String description) {
        mouvements.save(MouvementDette.builder().participantId(participant.getId()).atelierId(atelierId)
                .nature(nature).montant(amount).referenceSource(reference).description(description)
                .dateOperation(LocalDateTime.now()).build());
    }
}
