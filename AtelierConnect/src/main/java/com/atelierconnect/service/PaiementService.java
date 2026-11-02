package com.atelierconnect.service;

import com.atelierconnect.entity.Paiement;

import java.util.List;

public interface PaiementService {
    Paiement pay(Long reservationId, Long participantId);
    Paiement getById(Long id);
    Paiement getByReservationId(Long reservationId);
    List<Paiement> getByParticipantId(Long participantId);
    void checkPaiementOwner(Long paiementId, Long participantId);
    void checkReservationPaiementOwner(Long reservationId, Long participantId);
    void checkUnpaidBeforeAtelier();
}
