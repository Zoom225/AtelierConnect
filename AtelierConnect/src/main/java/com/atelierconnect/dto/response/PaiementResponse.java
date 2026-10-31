package com.atelierconnect.dto.response;

import com.atelierconnect.entity.enums.StatutPaiement;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementResponse {
    private Long id;
    private Long reservationId;
    private Double montant;
    private StatutPaiement statut;
    private LocalDateTime datePaiement;
}
