package com.atelierconnect.mapper;

import com.atelierconnect.dto.response.PaiementResponse;
import com.atelierconnect.entity.Paiement;
import org.springframework.stereotype.Component;

@Component
public class PaiementMapper {

    public PaiementResponse toResponse(Paiement paiement) {
        return PaiementResponse.builder()
                .id(paiement.getId())
                .reservationId(paiement.getReservation() != null ? paiement.getReservation().getId() : null)
                .montant(paiement.getMontant())
                .statut(paiement.getStatut())
                .datePaiement(paiement.getDatePaiement())
                .build();
    }
}
