package com.atelierconnect.mapper;

import com.atelierconnect.dto.response.ReservationResponse;
import com.atelierconnect.entity.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationMapper {

    private final PaiementMapper paiementMapper;

    public ReservationResponse toResponse(Reservation reservation) {
        if (reservation == null) {
            return null;
        }

        // Gestion des relations potentiellement nulles pour éviter les NullPointerExceptions
        if (reservation.getAtelier() == null || reservation.getParticipant() == null) {
            // Il est préférable de lever une exception ou de retourner un DTO invalide
            // plutôt que de laisser une NullPointerException se produire.
            // Pour cet exemple, nous retournons null, mais une exception serait plus claire.
            throw new IllegalArgumentException("La réservation doit avoir un atelier et un participant associés.");
        }

        return ReservationResponse.builder()
                .id(reservation.getId())
                .atelierId(reservation.getAtelier().getId())
                .atelierDateTime(reservation.getAtelier().getDateDebut())
                .participantId(reservation.getParticipant().getId())
                .participantNom(reservation.getParticipant().getPrenom() + " " + reservation.getParticipant().getNom())
                .statut(reservation.getStatut())
                .paiement(reservation.getPaiement() != null
                        ? paiementMapper.toResponse(reservation.getPaiement())
                        : null)
                .build();
    }
}
