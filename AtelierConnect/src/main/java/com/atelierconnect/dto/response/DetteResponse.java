package com.atelierconnect.dto.response;

import com.atelierconnect.entity.MouvementDette.Nature;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DetteResponse(BigDecimal solde, boolean suspensionActive, List<Mouvement> mouvements) {
    public record Mouvement(Long id, Nature nature, BigDecimal montant, String description,
                            Long atelierId, LocalDateTime dateOperation) {}
}
